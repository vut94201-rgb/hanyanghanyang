package com.personal.identity.core.service;

import com.personal.identity.core.role.Role;
import com.personal.identity.core.role.RoleNotFoundException;
import com.personal.identity.core.role.RoleRepository;
import com.personal.identity.core.security.PasswordEncoder;
import com.personal.identity.core.user.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Use case: đăng ký user mới.
 *
 * <p><b>Flow:</b>
 * <ol>
 *   <li>Validate input (sanity, không phải business validation - cái đó ở DTO layer)</li>
 *   <li>Check username chưa tồn tại → nếu có throw {@link DuplicateUsernameException}</li>
 *   <li>Check email chưa tồn tại → nếu có throw {@link DuplicateEmailException}</li>
 *   <li>Hash password bằng BCrypt</li>
 *   <li>Lookup default role {@code USER} - nếu không có throw {@link RoleNotFoundException}
 *       (đây là lỗi cấu hình hệ thống, không phải lỗi user)</li>
 *   <li>Build {@link User} mới với status ACTIVE, gán default role, save</li>
 * </ol>
 *
 * <p><b>Không tự động login sau register.</b> Trả về user đã save, client tự gọi
 * login endpoint. Lý do: một số UX flow muốn cho user verify email trước khi
 * cho login lần đầu. Tách 2 use case giữ flexibility.
 *
 * <p><b>Không xử lý race condition unique constraint:</b> 2 request register
 * cùng username đến cùng lúc, cả 2 đều thấy {@code existsByUsername == false},
 * 1 thành công, 1 fail với {@code DataIntegrityViolationException} ở JPA. Đây
 * là chuyện cực hiếm trong thực tế (UX register form), và adapter sẽ wrap
 * thành domain exception ở bước F nếu cần. Tránh over-engineer (vd: dùng
 * SELECT FOR UPDATE chỉ vì 1 race rất hiếm).
 *
 * <p><b>Vì sao pure Java, không {@code @Service}:</b> giữ core 100% framework-free.
 * Adapter ở identity-api sẽ tạo bean qua {@code @Bean} method trong
 * {@code UseCaseConfig} (bước F).
 */
public class RegisterUseCase {

    /**
     * Role mặc định gán cho user mới register. Phải khớp với seed trong
     * {@code V3__seed_default_data.sql}.
     */
    private static final String DEFAULT_ROLE_CODE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUseCase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Đăng ký user mới với default role.
     *
     * @param command thông tin user mới (đã được validate ở DTO layer trước khi đến đây)
     * @return User đã save (có id, createdAt, gán role USER)
     * @throws DuplicateUsernameException nếu username đã tồn tại
     * @throws DuplicateEmailException    nếu email đã tồn tại
     * @throws RoleNotFoundException      nếu seed role USER bị xóa (lỗi hệ thống)
     */
    public User execute(RegisterCommand command) {
        // 1. Sanity check - phòng trường hợp service được gọi từ test/code khác
        //    bỏ qua DTO validation. KHÔNG thay thế cho @Valid ở controller.
        validateInput(command);

        // 2. Check duplicate - throw exception sớm với message thân thiện
        //    thay vì để JPA throw DataIntegrityViolationException với SQL error.
        if (userRepository.existsByUsername(command.username())) {
            throw new DuplicateUsernameException(command.username());
        }
        if (userRepository.existsByEmailAddress(command.emailAddress())) {
            throw new DuplicateEmailException(command.emailAddress());
        }

        // 3. Hash password
        String passwordHash = passwordEncoder.encode(command.rawPassword());

        // 4. Lookup default role USER
        Role defaultRole = roleRepository.findByRoleCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> RoleNotFoundException.byCode(DEFAULT_ROLE_CODE));

        // 5. Build user mới qua factory - status ACTIVE đã set mặc định trong createNew
        User user = User.createNew(
                command.username(),
                command.emailAddress(),
                passwordHash,
                command.fullName()
        );
        user.addRole(defaultRole);

        return userRepository.save(user);
    }

    private void validateInput(RegisterCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("RegisterCommand must not be null");
        }
        if (command.username() == null || command.username().isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (command.emailAddress() == null || command.emailAddress().isBlank()) {
            throw new IllegalArgumentException("emailAddress must not be blank");
        }
        if (command.rawPassword() == null || command.rawPassword().isBlank()) {
            throw new IllegalArgumentException("rawPassword must not be blank");
        }
        // fullName được phép null/blank - không phải mọi user đều cung cấp.
    }

    /**
     * Input cho {@link #execute(RegisterCommand)}. Record để immutable + gọn.
     *
     * <p>Đặt làm nested record vì chỉ use case này dùng - không cần file riêng.
     * Pattern này nhẹ hơn tạo file {@code RegisterCommand.java} ngoài.
     *
     * @param username      Username unique, đã trim/lowercase ở DTO layer
     * @param emailAddress  Email unique, đã normalize ở DTO layer
     * @param rawPassword   Plain password - sẽ được hash trong use case
     * @param fullName      Tên hiển thị, nullable
     */
    public record RegisterCommand(
            String username,
            String emailAddress,
            String rawPassword,
            String fullName
    ) {
    }
}