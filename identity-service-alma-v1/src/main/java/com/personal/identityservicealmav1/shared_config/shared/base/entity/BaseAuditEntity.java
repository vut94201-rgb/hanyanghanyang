package com.personal.identityservicealmav1.shared_config.shared.base.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@MappedSuperclass
public class BaseAuditEntity {
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        setAuditUser();
        this.updatedBy = this.createdBy;
    }

    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        CustomUserDetail user = SecurityUtils.getCurrentUser();
        if (user != null) this.updatedBy = user.getUser().getId();
    }

    private void setAuditUser() {
        CustomUserDetail user = SecurityUtils.getCurrentUser();
        if (user != null) this.createdBy = user.getUser().getId();
    }
}
