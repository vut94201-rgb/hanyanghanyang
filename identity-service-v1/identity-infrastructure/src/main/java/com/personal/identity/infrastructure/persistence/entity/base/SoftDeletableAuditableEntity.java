package com.personal.identity.infrastructure.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Mở rộng {@link AuditableEntity} thêm khả năng <b>soft delete</b>:
 * <ul>
 *   <li>{@code is_deleted} - cờ boolean (Oracle NUMBER(1), 0/1).</li>
 *   <li>{@code deleted_at} - thời điểm xóa (UTC), null nếu chưa xóa.</li>
 * </ul>
 *
 * <p><b>Cơ chế soft delete:</b> class này CHỈ giữ field. Hành vi "khi DELETE thì
 * UPDATE thay vì xóa thật" và "khi SELECT thì loại record đã xóa" sẽ do từng
 * entity con khai báo bằng:
 *
 * <pre>{@code
 * @Entity
 * @Table(name = "users")
 * @SQLDelete(sql = "UPDATE users SET is_deleted = 1, deleted_at = SYSTIMESTAMP WHERE id = ? AND version = ?")
 * @SQLRestriction("is_deleted = 0")
 * public class UserEntity extends SoftDeletableAuditableEntity { ... }
 * }</pre>
 *
 * <p>Lý do KHÔNG đặt {@code @SQLDelete}/{@code @SQLRestriction} ở class này:
 * Hibernate yêu cầu SQL của {@code @SQLDelete} phải khớp tên bảng cụ thể. Đặt ở
 * superclass sẽ không biết bảng nào để UPDATE - mỗi entity con phải tự khai báo.
 *
 * <p>Dùng cho entity cần audit trail kể cả sau khi "xóa" (User là điển hình:
 * cần giữ lại để truy vết, GDPR, hoàn nguyên...).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class SoftDeletableAuditableEntity extends AuditableEntity {

    /**
     * Cờ soft delete. Map sang Oracle NUMBER(1) - JPA tự convert boolean <-> 0/1.
     * Default 0 (chưa xóa) đã được set ở migration SQL.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    /**
     * Thời điểm xóa (UTC), null nếu chưa xóa.
     * KHÔNG dùng auditing listener cho cột này - được set thủ công bởi
     * câu UPDATE trong {@code @SQLDelete} của entity con (SYSTIMESTAMP của Oracle).
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
