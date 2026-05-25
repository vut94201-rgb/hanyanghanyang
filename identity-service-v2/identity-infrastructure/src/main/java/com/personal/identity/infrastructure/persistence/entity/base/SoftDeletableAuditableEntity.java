package com.personal.identity.infrastructure.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
public abstract class SoftDeletableAuditableEntity<ID extends Serializable>
    extends AuditableEntity<ID> {
  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "deleted", nullable = false)
  private boolean deleted = Boolean.FALSE;

  @Column(name = "deleted_at")
  private Instant deletedAt;
}
