package com.personal.identity.infrastructure.persistence.entity.enums;

import com.personal.identity.core.domain.session.SessionStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SessionStatusConverter extends AbstractCodeEnumConverter<SessionStatus>{

    protected SessionStatusConverter() {
        super(SessionStatus.class);
    }
}
