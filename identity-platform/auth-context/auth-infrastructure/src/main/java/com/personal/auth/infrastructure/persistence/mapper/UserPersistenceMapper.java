package com.personal.auth.infrastructure.persistence.mapper;

import com.personal.auth.domain.model.User;
import com.personal.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
/**
 * MapStruct mapper between the {@link User} domain aggregate and its
 * persistence twin {@link UserJpaEntity}.
 *
 * <p>The mapping is largely field-by-field; the only subtlety is the
 * direction:
 *
 * <ul>
 *   <li><b>Domain → Entity (toEntity / updateEntity):</b> we never copy
 *       audit columns or the version. Those are owned by Hibernate /
 *       Spring Data JPA auditing; touching them from domain would
 *       overwrite metadata managed by the persistence layer.</li>
 *   <li><b>Entity → Domain (toDomain):</b> uses {@link User#restore} —
 *       the dedicated factory that <i>skips</i> {@code register()}
 *       validation, because data coming back from the DB has already
 *       satisfied DB constraints.</li>
 * </ul>
 *
 * <p>{@code unmappedTargetPolicy = IGNORE} silently drops any target
 * fields that {@link User} doesn't carry — namely the inherited audit
 * columns on {@link UserJpaEntity} ({@code version}, {@code createdBy},
 * {@code updatedBy}, {@code createdAt}, {@code updatedAt}, {@code active},
 * {@code deleted}). They are filled by Spring Data JPA auditing and
 * default initialisers on the entity; the mapper has no business
 * touching them.
 *
 * <p>Generated as a Spring bean via {@code componentModel = "spring"}.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserPersistenceMapper {

    /**
     * Map a domain {@link User} to a fresh {@link UserJpaEntity} for INSERT.
     * Audit fields are left at their defaults — Spring Data JPA auditing
     * and the parent {@code BaseJpaAuditEntity}'s field initialisers fill
     * them on save.
     */
    UserJpaEntity toEntity(User user);

    /**
     * Copy mutable fields from a (potentially mutated) domain {@link User}
     * onto an existing managed {@link UserJpaEntity} for UPDATE.
     *
     * <p>id is ignored — the managed entity already owns its primary key
     * and overwriting it would be a bug.
     */
    @Mapping(target = "id", ignore = true)
    void updateEntity(User user, @MappingTarget UserJpaEntity entity);

    /**
     * Reconstruct a domain {@link User} from a persistence row.
     *
     * <p>MapStruct can't call a static factory method by itself, so we
     * implement this with a default method that delegates to
     * {@link User#restore}.
     */
    default User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.restore(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getFullName(),
                entity.getStatus()
        );
    }
}