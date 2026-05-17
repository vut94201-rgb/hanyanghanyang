package com.personal.identity.core.user;

import com.personal.identity.core.shared.exception.DomainException;

/**
 * Throw khi tìm user theo id / username / email mà không thấy.
 *
 * <p>Phân biệt với {@code InvalidCredentialsException}: khi LOGIN sai password,
 * chúng ta dùng {@code InvalidCredentialsException} (KHÔNG dùng exception này),
 * để tránh leak thông tin "username có tồn tại nhưng password sai".
 */
public class UserNotFoundException extends DomainException {

    private static final String CODE = "USER.NOT_FOUND";

    public static UserNotFoundException byId(Long id) {
        return new UserNotFoundException("User not found with id=" + id);
    }

    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException("User not found with username=" + username);
    }

    private UserNotFoundException(String message) {
        super(CODE, message);
    }
}
