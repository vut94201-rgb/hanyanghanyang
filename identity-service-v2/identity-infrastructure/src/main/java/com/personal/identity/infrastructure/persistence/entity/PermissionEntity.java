package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.infrastructure.persistence.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for the {@code permissions} table.
 *
 * <p>Inherits from {@code AuditableEntity<Long>} - inherits {@code created_at}, {@code updated_at},
 * {@code version}, along with a type-safe {@code Long getId()} getter (instead of {@code Object}).
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class PermissionEntity extends AuditableEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "permissions_seq_gen")
    @SequenceGenerator(
            name = "permissions_seq_gen",
            sequenceName = "permissions_seq",
            allocationSize = 50
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "permission_code", nullable = false, length = 100, unique = true)
    private String permissionCode;

    @Column(name = "description", length = 500)
    private String description;

    // Note: NO NEED to override getId() - Lombok's @Getter auto-generates public Long getId()
    // from the 'id' field, fulfilling the abstract method in BaseEntity<Long>.
}