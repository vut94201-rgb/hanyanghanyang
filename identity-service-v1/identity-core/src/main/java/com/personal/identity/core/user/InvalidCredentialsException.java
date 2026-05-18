package com.personal.identity.core.user;


import com.personal.identity.core.shared.exception.DomainException;

/**
 * Throw khi login fail vì username KHÔNG tồn tại HOẶC password SAI.
 *
 * <p><b>Vì sao gộp 2 case vào 1 exception, không phân biệt:</b>
 * security best practice - nếu phân biệt {@link UserNotFoundException} (user không có)
 * và "wrong password" (user có nhưng pass sai), attacker có thể enum username
 * bằng cách thử nhiều username và xem response khác nhau. Trả về CÙNG 1 message
 * và CÙNG 1 thời gian xử lý (BCrypt verify ngay cả khi user không tồn tại - xem
 * {@code LoginUseCase}) là cách chống user enumeration cơ bản.
 *
 * <p><b>Quy ước:</b> message luôn là "Invalid username or password" -
 * KHÔNG bao giờ "user not found" hoặc "wrong password". Frontend cũng phải
 * hiển thị message này, không phân biệt 2 case.
 */
public class InvalidCredentialsException extends DomainException {

    private static final String CODE = "AUTH.INVALID_CREDENTIALS";
    private static final String MESSAGE = "Invalid username or password";

    public InvalidCredentialsException() {
        super(CODE, MESSAGE);
    }
}