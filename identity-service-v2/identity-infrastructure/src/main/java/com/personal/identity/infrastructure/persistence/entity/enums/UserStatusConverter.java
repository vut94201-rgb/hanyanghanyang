package com.personal.identity.infrastructure.persistence.entity.enums;

import com.personal.identity.core.domain.user.UserStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter extends  AbstractCodeEnumConverter<UserStatus >{

    protected UserStatusConverter() {
        super(UserStatus.class);
    }
}
