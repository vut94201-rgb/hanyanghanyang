package com.personal.identityservicealmav1.shared_config.shared.base.entity;

import jakarta.persistence.Column;
import org.springframework.data.annotation.*;

import java.time.LocalDateTime;

public class BaseSoftDeleteEntity {
    @Version
    private Long version;

    @CreatedBy
    @Column(name = "created_by")
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}
