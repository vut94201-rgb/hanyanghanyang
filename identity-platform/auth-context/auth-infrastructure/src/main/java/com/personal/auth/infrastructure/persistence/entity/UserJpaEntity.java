package com.personal.auth.infrastructure.persistence.entity;

import com.personal.auth.domain.model.UserStatus;
import com.personal.persistence.BaseJpaAuditEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity mirroring the {@code users} table (see
 * {@code V1__create_users_table.sql}).
 *
 * <p><b>NOT a domain object.</b> This is the persistence representation —
 * it carries audit columns, optimistic-locking version, JPA annotations,
 * and a no-arg constructor required by Hibernate. The domain's
 * {@link com.personal.auth.domain.model.User} is converted to/from this
 * entity by {@code UserPersistenceMapper}.
 *
 * <p>Why a separate entity at all? See discussion in chat: domain stays
 * pure (no JPA, no Spring), entity stays free to evolve with persistence
 * concerns. Trade-off is the field duplication; benefit is testability
 * and clean dependency direction.
 *
 * <p>Mapping notes:
 * <ul>
 *   <li>Id strategy: Oracle sequence {@code seq_users} created by Flyway.
 *       JPA generation strategy is {@link GenerationType#SEQUENCE} pointing
 *       at it with allocationSize=1 (no Hibernate-side caching since the
 *       sequence is {@code NOCACHE} and increments by 1).</li>
 *   <li>{@link UserStatus} is stored as {@code STRING} (the
 *       {@code ck_users_status} CHECK constraint guards the values),
 *       never ordinal — context v2 §1.5 / §4.3.</li>
 *   <li>Audit/active/deleted columns inherited from
 *       {@link BaseJpaAuditEntity}.</li>
 * </ul>
 */
@Entity
@Table(name = "users")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserJpaEntity extends BaseJpaAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq_gen")
    @SequenceGenerator(name = "users_seq_gen", sequenceName = "seq_users", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    /**
     * BCrypt hash — never plain text.
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;
}
