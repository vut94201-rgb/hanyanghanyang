package com.personal.identity.infrastructure.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Root cho mọi JPA entity.
 *
 * <h3>Generic type parameter {@code <ID>}</h3>
 * Mỗi entity có kiểu PK khác nhau:
 * <ul>
 *   <li>{@code UserEntity}, {@code RoleEntity}, {@code PermissionEntity}: {@code Long} (Oracle SEQUENCE)</li>
 *   <li>{@code SessionEntity}, {@code RefreshTokenEntity}: {@code String} UUID - NHƯNG KHÔNG kế thừa class này, do lifecycle riêng biệt.</li>
 * </ul>
 * Generic {@code <ID extends Serializable>} cho phép subclass khai báo {@code getId()}
 * trả về đúng kiểu (vd: {@code Long getId()} thay vì {@code Object getId()}).
 *
 * <p><b>Vì sao type-safe quan trọng:</b> MapStruct và Spring Data JPA cần biết
 * kiểu return cụ thể của getter để generate code correct. Trả về {@code Object}
 * sẽ làm MapStruct không biết cách map sang field {@code Long id} của domain.
 *
 * <p><b>Pattern tương đương:</b> {@code org.springframework.data.domain.Persistable<ID>}
 * của Spring Data JPA cũng làm y hệt - đây là idiom chuẩn.
 *
 * <h3>Cung cấp gì</h3>
 * <ul>
 *   <li>{@code version} - optimistic locking. Hibernate quản lý hoàn toàn.</li>
 *   <li>{@link #equals(Object)} / {@link #hashCode()} dựa trên id - tránh
 *       bug "equals trên proxy".</li>
 * </ul>
 *
 * <p>Class con KHÔNG dùng {@code @EqualsAndHashCode} của Lombok.
 *
 * <p>Class con TỰ khai báo {@code @Id} và {@code @GeneratedValue} cho field {@code id}
 * của riêng nó (vì ID strategy khác nhau giữa SEQUENCE và String UUID).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity<ID extends Serializable> {

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Subclass cài đặt: trả về {@code id} của entity với kiểu cụ thể.
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
