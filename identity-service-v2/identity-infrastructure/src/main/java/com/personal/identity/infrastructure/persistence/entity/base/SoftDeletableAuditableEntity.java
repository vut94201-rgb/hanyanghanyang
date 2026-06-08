package com.personal.identity.infrastructure.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;

/**
 * Extends {@link AuditableEntity} to add <b>soft delete</b> capabilities:
 * <ul>
 * <li>{@code is_deleted} - boolean flag (Oracle NUMBER(1), 0/1).</li>
 * <li>{@code deleted_at} - deletion timestamp (UTC), null if not deleted.</li>
 * </ul>
 *
 * <p>Subclasses must inherit with a specific type parameter:
 * {@code UserEntity extends SoftDeletableAuditableEntity<Long>}.
 *
 * <p><b>Soft delete mechanism:</b> this class ONLY holds the fields. The behaviors
 * "UPDATE instead of actual deletion upon DELETE" and "filter out deleted records upon SELECT"
 * are declared by each subclass entity using {@code @SQLDelete} + {@code @SQLRestriction}.
 *
 * <h3>Note on Oracle boolean types</h3>
 * Oracle <b>lacks a standard BOOLEAN before 23ai</b>, so the convention is to store booleans as
 * {@code NUMBER(1)}. Hibernate 6 with Oracle 23ai defaults to mapping Java {@code boolean}
 * -> JDBC {@code BOOLEAN} (native Oracle 23ai). To match the legacy schema {@code NUMBER(1)},
 * we use {@code @JdbcTypeCode(SqlTypes.NUMERIC)} to force the mapping to NUMERIC.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class SoftDeletableAuditableEntity<ID extends Serializable>
        extends AuditableEntity<ID> {
    /**
     * Soft delete flag. Forced mapping to JDBC NUMERIC to match Oracle {@code NUMBER(1)}.
     * Hibernate automatically converts {@code true <-> 1}, {@code false <-> 0}.
     */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    /**
     * Deletion timestamp (UTC), null if not deleted.
     * Manually set by the UPDATE statement in the subclass entity's {@code @SQLDelete}
     * (using Oracle's {@code SYSTIMESTAMP}).
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
