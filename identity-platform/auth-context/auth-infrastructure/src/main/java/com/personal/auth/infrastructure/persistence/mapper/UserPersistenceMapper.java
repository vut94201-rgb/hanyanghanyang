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
 * <p>Generated as a Spring bean via {@code componentModel = "spring"}.
 *
 * <p>Why a separate mapper at all (vs. a plain {@code static} method)?
 * Same reason MapStruct exists everywhere else: compile-time generation
 * catches field renames, no reflection, easy to add edge-case mapping
 * rules without sprinkling code through the use case.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserPersistenceMapper {
    /**
     * Map a domain {@link User} to a fresh {@link UserJpaEntity} for INSERT.
     *
     * <p>Audit fields ({@code version}, {@code createdBy}, {@code updatedBy},
     * {@code createdAt}, {@code updatedAt}, {@code active}, {@code deleted})
     * are left at their defaults — Spring Data JPA auditing + the entity's
     * default initialisers fill them.
     */
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    UserJpaEntity toEntity(User user);

    /**
     * Copy mutable fields from a (potentially mutated) domain {@link User}
     * onto an existing managed {@link UserJpaEntity} for UPDATE.
     *
     * <p>id and audit metadata are intentionally not copied.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deleted", ignore = true)
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
