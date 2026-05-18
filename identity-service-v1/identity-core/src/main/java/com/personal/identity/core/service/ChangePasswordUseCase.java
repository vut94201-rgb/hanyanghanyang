package com.personal.identity.core.service;

import com.personal.identity.core.security.PasswordEncoder;
import com.personal.identity.core.session.RevokedReason;
import com.personal.identity.core.session.SessionRepository;
import com.personal.identity.core.token.RefreshTokenRepository;
import com.personal.identity.core.user.InvalidCredentialsException;
import com.personal.identity.core.user.User;
import com.personal.identity.core.user.UserNotFoundException;
import com.personal.identity.core.user.UserRepository;

/**
 * Use case: user đổi password.
 *
 * <p><b>Flow:</b>
 * <ol>
 *   <li>Load user theo id (đã authenticate từ filter, có userId trong context)</li>
 *   <li>Verify current password - nếu sai throw {@link InvalidCredentialsException}</li>
 *   <li>Check new password khác current (đỡ thao tác vô nghĩa)</li>
 *   <li>Hash new password, gọi {@code user.changePassword(newHash)}, save</li>
 *   <li>Revoke TẤT CẢ session khác của user (trừ session hiện tại) - force re-login
 *       trên các device khác. Đây là security best practice.</li>
 * </ol>
 *
 * <h2>Tại sao revoke session khác</h2>
 *
 * <p>Đổi password thường là phản ứng với 1 trong 2 tình huống:
 * <ol>
 *   <li><b>Định kỳ:</b> user thấy "password đã dùng 6 tháng, đổi cho an toàn". Không
 *       có dấu hiệu compromise.</li>
 *   <li><b>Phát hiện bất thường:</b> user thấy session lạ, đăng nhập từ device không
 *       phải của mình → đổi password để cắt access của attacker.</li>
 * </ol>
 *
 * <p>Trường hợp (2) là chí mạng. Nếu chỉ đổi password mà KHÔNG revoke session, thì:
 * attacker vẫn còn session ACTIVE → vẫn dùng access token + refresh token thoải mái
 * → đổi password chỉ ngăn được login lần SAU, không ngăn được session đang chạy.
 * <b>Revoke session khác là cách duy nhất cắt access đang chạy.</b>
 *
 * <p>Pattern này là chuẩn: Google, GitHub, Facebook đều force re-login trên các
 * device khác sau khi đổi password.
 *
 * <p><b>Vì sao GIỮ session hiện tại:</b> user đang ở web đổi password - sau khi
 * đổi xong, không lý do gì bắt user re-login trên TAB họ đang dùng. UX tệ.
 *
 * <p><b>Không blacklist access token của các session bị revoke ở đây.</b> Access
 * token TTL ngắn (15p), session đã REVOKED → filter ở bước F sẽ check session
 * status và reject mọi request từ session REVOKED ngay cả khi access token còn
 * hiệu lực. Trong vòng 15 phút (worst case) các access token cũ sẽ tự expire.
 */
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public ChangePasswordUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void execute(ChangePasswordCommand command) {
        validateInput(command);

        // 1. Load user
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> UserNotFoundException.byId(command.userId()));

        // 2. Verify current password
        // KHÔNG dùng dummy hash timing-attack trick ở đây vì user đã authenticated
        // (có valid JWT) - không có chuyện enumerate user. Sai password = sai password.
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // 3. Check new password khác current. Không strictly bắt buộc (BCrypt sẽ
        //    sinh hash khác do salt khác), nhưng tránh user nhầm tay nhấn lại
        //    password cũ.
        if (passwordEncoder.matches(command.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must differ from current password");
        }

        // 4. Hash + save
        String newHash = passwordEncoder.encode(command.newPassword());
        user.changePassword(newHash);
        userRepository.save(user);

        // 5. Revoke TẤT CẢ session khác (trừ session hiện tại) - cắt access của
        //    mọi device khác. Session hiện tại được giữ để user khỏi phải re-login
        //    ngay trên tab họ đang đổi password.
        //
        //    Bulk operation: cả refresh token thuộc các session đó sẽ được dọn
        //    ngầm bởi cleanup job, hoặc service riêng có thể gọi
        //    revokeAllBySessionId cho từng session - nhưng ở đây ưu tiên đơn giản.
        sessionRepository.revokeAllOtherSessions(
                user.getId(),
                command.currentSessionId(),
                RevokedReason.USER_ACTION
        );
    }

    private void validateInput(ChangePasswordCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("ChangePasswordCommand must not be null");
        }
        if (command.userId() == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (command.currentSessionId() == null || command.currentSessionId().isBlank()) {
            throw new IllegalArgumentException("currentSessionId must not be blank");
        }
        if (command.currentPassword() == null || command.currentPassword().isBlank()) {
            throw new IllegalArgumentException("currentPassword must not be blank");
        }
        if (command.newPassword() == null || command.newPassword().isBlank()) {
            throw new IllegalArgumentException("newPassword must not be blank");
        }
    }

    /**
     * Input cho {@link #execute(ChangePasswordCommand)}.
     *
     * @param userId            ID user (lấy từ JWT claims trong filter)
     * @param currentSessionId  Session ID hiện tại (lấy từ JWT claims) - session này
     *                          sẽ được GIỮ, các session khác bị revoke
     * @param currentPassword   Password hiện tại để verify ownership
     * @param newPassword       Password mới (đã validate strength ở DTO layer)
     */
    public record ChangePasswordCommand(
            Long userId,
            String currentSessionId,
            String currentPassword,
            String newPassword
    ) {
    }
}