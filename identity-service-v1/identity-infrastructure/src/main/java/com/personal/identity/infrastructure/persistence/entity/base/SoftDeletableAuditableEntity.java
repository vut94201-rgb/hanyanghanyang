package com.personal.identity.infrastructure.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;

/**
 * Mở rộng {@link AuditableEntity} thêm khả năng <b>soft delete</b>:
 * <ul>
 *   <li>{@code is_deleted} - cờ boolean (Oracle NUMBER(1), 0/1).</li>
 *   <li>{@code deleted_at} - thời điểm xóa (UTC), null nếu chưa xóa.</li>
 * </ul>
 *
 * <p>Class con phải kế thừa với type parameter cụ thể:
 * {@code UserEntity extends SoftDeletableAuditableEntity<Long>}.
 *
 * <p><b>Cơ chế soft delete:</b> class này CHỈ giữ field. Hành vi "khi DELETE thì
 * UPDATE thay vì xóa thật" và "khi SELECT thì loại record đã xóa" do từng entity
 * con khai báo bằng {@code @SQLDelete} + {@code @SQLRestriction}.
 *
 * <h3>Lưu ý kiểu boolean trong Oracle</h3>
 * Oracle <b>không có BOOLEAN chuẩn trước 23ai</b>, convention lưu boolean là
 * {@code NUMBER(1)}. Hibernate 6 với Oracle 23ai mặc định map Java {@code boolean}
 * → JDBC {@code BOOLEAN} (native Oracle 23ai). Để match schema cũ {@code NUMBER(1)},
 * dùng {@code @JdbcTypeCode(SqlTypes.NUMERIC)} ép map sang NUMERIC.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class SoftDeletableAuditableEntity<ID extends Serializable> extends AuditableEntity<ID> {

    /**
     * Cờ soft delete. Ép map sang JDBC NUMERIC để khớp Oracle {@code NUMBER(1)}.
     * Hibernate tự convert {@code true ↔ 1}, {@code false ↔ 0}.
     */
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    /**
     * Thời điểm xóa (UTC), null nếu chưa xóa.
     * Được set thủ công bởi câu UPDATE trong {@code @SQLDelete} của entity con
     * (dùng {@code SYSTIMESTAMP} của Oracle).
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
