package com.personal.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Root cho mọi JPA entity.
 *
 * <p>Cung cấp:
 * <ul>
 *   <li>{@code id} - primary key (kiểu Long, ID strategy do class con quyết định
 *       qua {@code @GeneratedValue} của riêng nó).</li>
 *   <li>{@code version} - optimistic locking. Hibernate sẽ tự tăng giá trị và
 *       so sánh ở mỗi UPDATE/DELETE; nếu khác sẽ throw {@code OptimisticLockException}.</li>
 *   <li>{@link #equals(Object)} / {@link #hashCode()} dựa trên id - tránh
 *       bug "equals trên proxy" mà Lombok {@code @EqualsAndHashCode} hay gây ra.</li>
 * </ul>
 *
 * <p>Class con KHÔNG dùng {@code @EqualsAndHashCode} của Lombok.
 *
 * <p><b>Lưu ý vì sao không khai báo {@code @GeneratedValue} ở đây:</b>
 * Mỗi entity có ID strategy khác nhau:
 * <ul>
 *   <li>{@code UserEntity}, {@code RoleEntity}, {@code PermissionEntity} dùng Oracle SEQUENCE.</li>
 *   <li>{@code SessionEntity}, {@code RefreshTokenEntity} dùng UUID (String) - sẽ KHÔNG
 *       kế thừa class này (vì id ở đây là Long).</li>
 * </ul>
 * Nên ta để class con tự khai báo {@code @Id} và {@code @GeneratedValue} của riêng nó.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * Optimistic locking. Hibernate quản lý hoàn toàn - KHÔNG set bằng tay.
     * Mỗi UPDATE sẽ tự thêm WHERE version = ? và UPDATE version = version + 1.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Subclass cài đặt: trả về {@code id} của entity.
     * Khai báo abstract để equals/hashCode hoạt động đồng nhất bất kể kiểu id (Long, String, UUID).
     */
    public abstract Object getId();

    /**
     * Equality dựa trên ID, KHÔNG dựa trên các field còn lại.
     *
     * <p>Khi {@code id == null} (entity chưa persist), 2 instance đều "transient" -
     * KHÔNG được coi là bằng nhau (trừ khi cùng tham chiếu).
     *
     * <p>{@code getClass()} thay vì {@code instanceof} để xử lý đúng Hibernate proxy:
     * 1 entity và proxy của nó có thể khác class nhưng cùng id. Để chính xác hơn nữa
     * có thể dùng {@code Hibernate.getClass(other)}, nhưng cách ở đây đã đủ cho 99% case.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        BaseEntity other = (BaseEntity) o;
        return getId() != null && Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        // Constant để khi entity còn transient (id=null) vẫn nhất quán nếu bị put vào HashSet.
        // Khi id được gán sau persist, hashCode KHÔNG đổi - quan trọng cho contract.
        return getClass().hashCode();
    }
}
