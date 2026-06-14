package com.personal.identity.infrastructure.persistence.entity.enums;

import com.personal.identity.core.domain.audit.AdminAuditEvent;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditOutcomeConverter extends AbstractCodeEnumConverter<AdminAuditEvent.Outcome> {

    protected AuditOutcomeConverter() {
        super(AdminAuditEvent.Outcome.class);
    }
}
