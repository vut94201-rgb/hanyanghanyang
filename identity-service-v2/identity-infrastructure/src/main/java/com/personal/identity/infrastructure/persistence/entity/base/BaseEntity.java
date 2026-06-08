package com.personal.identity.infrastructure.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Version;

import java.io.Serializable;
import java.util.Objects;

/**
 * Root for all JPA entities.
 *
 * <h3>Generic type parameter {@code <ID>}</h3>
 * Each entity has a different PK type:
 * <ul>
 * <li>{@code UserEntity}, {@code RoleEntity}, {@code PermissionEntity}: {@code Long} (Oracle SEQUENCE)</li>
 * <li>{@code SessionEntity}, {@code RefreshTokenEntity}: {@code String} UUID - BUT DO NOT inherit this class, due to a distinct lifecycle.</li>
 * </ul>
 * The generic {@code <ID extends Serializable>} allows subclasses to declare {@code getId()}
 * returning the exact type (e.g., {@code Long getId()} instead of {@code Object getId()}).
 *
 * <p><b>Why type-safety is important:</b> MapStruct and Spring Data JPA need to know
 * the specific return type of the getter to generate correct code. Returning {@code Object}
 * would leave MapStruct not knowing how to map it to the domain's {@code Long id} field.
 *
 * <p><b>Equivalent Pattern:</b> {@code org.springframework.data.domain.Persistable<ID>}
 * from Spring Data JPA does the exact same thing - this is a standard idiom.
 *
 * <h3>What it provides</h3>
 * <ul>
 * <li>{@code version} - optimistic locking. Fully managed by Hibernate.</li>
 * <li>{@link #equals(Object)} / {@link #hashCode()} based on id - prevents
 * the "equals on proxy" bug.</li>
 * </ul>
 *
 * <p>Subclasses MUST NOT use Lombok's {@code @EqualsAndHashCode}.
 *
 * <p>Subclasses MUST declare their own {@code @Id} and {@code @GeneratedValue} for their
 * specific {@code id} field (because ID strategies differ between SEQUENCE and String UUID).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity<ID extends Serializable> implements Serializable {

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Subclass implementation: returns the {@code id} of the entity with a specific type.
     */

    public abstract ID getId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        BaseEntity<?> other = (BaseEntity<?>) o;
        return getId() != null && Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
