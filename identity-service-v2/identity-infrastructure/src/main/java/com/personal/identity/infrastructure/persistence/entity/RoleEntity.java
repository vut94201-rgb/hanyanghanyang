package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.infrastructure.persistence.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity for the {@code roles} table.
 *
 * <p>Owning side of the many-to-many relationship with {@link PermissionEntity} via the
 * {@code role_permissions} join table. Defaults to {@code FetchType.LAZY}; the repository uses
 * an {@code @EntityGraph} to fetch on-demand.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class RoleEntity extends AuditableEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roles_seq_gen")
    @SequenceGenerator(
            name = "roles_seq_gen",
            sequenceName = "roles_seq",
            allocationSize = 50
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "role_code", nullable = false, length = 50, unique = true)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> permissions = new HashSet<>();

    // Lombok's @Getter auto-generates Long getId() - fulfilling the abstract method in the base class.
}