package com.personal.identity.core.user;

import java.util.Optional;
import java.util.List;

/**
 * <b>PORT</b> - interface mà infrastructure phải implement (qua
 * {@code UserRepositoryAdapter} dùng JPA bên dưới).
 *
 * <p>Core chỉ biết "có cách lưu / truy vấn User", KHÔNG biết database là Oracle,
 * KHÔNG biết Hibernate, KHÔNG biết Spring Data. Đây là Dependency Inversion:
 * abstraction (interface này) thuộc về core, implementation thuộc về infrastructure.
 *
 * <p>Quy ước:
 * <ul>
 *   <li>Method tìm kiếm trả {@link Optional} - tránh null check rải rác.</li>
 *   <li>{@link #save(User)} dùng cho cả CREATE và UPDATE - JPA tự phân biệt
 *       qua {@code @Id == null} hay không.</li>
 *   <li>{@link #softDelete(User)} explicit để service biết đây là soft delete,
 *       không phải DELETE thật.</li>
 * </ul>
 */
public interface UserRepository {

    /**
     * Lưu user. Trả về user đã được persist (có id, createdAt, updatedAt set).
     */
    User save(User user);

    /**
     * Tìm theo PK. Adapter sẽ tự bỏ qua user đã soft-delete
     * (nhờ {@code @SQLRestriction("is_deleted = 0")} trên entity).
     */
    Optional<User> findById(Long id);

    /** Tìm theo username (unique). */
    Optional<User> findByUsername(String username);

    /** Tìm theo email (unique). */
    Optional<User> findByEmailAddress(String emailAddress);

    /** Check tồn tại - nhẹ hơn {@code findByXxx().isPresent()}. */
    boolean existsByUsername(String username);

    boolean existsByEmailAddress(String emailAddress);

    /**
     * Soft delete: UPDATE is_deleted=1, deleted_at=now. KHÔNG xóa record vật lý.
     * Sau khi gọi, các query mặc định sẽ không trả về user này nữa.
     */
    void softDelete(User user);

    List<User> findAll(int offset, int limit, UserStatus statusFilter);

    long count(UserStatus statusFilter);
}
