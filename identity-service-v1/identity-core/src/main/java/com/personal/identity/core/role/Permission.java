package com.personal.identity.core.role;

/**
 * Quyền chi tiết. Là value object thuần - dùng {@code record} cho gọn và immutable.
 *
 * <p>Format {@code permissionCode}: {@code resource:action} theo convention REST.
 * Ví dụ: {@code user:read}, {@code user:write}, {@code session:manage}.
 *
 * <p>Hai Permission được coi là bằng nhau khi id bằng nhau (record tự sinh
 * equals/hashCode dựa trên TẤT CẢ field - vẫn đúng ở đây vì các field còn lại
 * cũng deterministic theo id).
 *
 * @param id              PK trong DB, null khi entity chưa persist.
 * @param permissionCode  Mã định danh, unique.
 * @param description     Mô tả tiếng Việt cho admin UI.
 */
public record Permission(
        Long id,
        String permissionCode,
        String description
) {
}
