package com.personal.identity.infrastructure.geoip;


import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bind config namespace {@code app.geoip.*} từ application.yml.
 *
 * <p><b>Vì sao là record, không phải class với getter:</b> record gọn, immutable
 * tự nhiên. Spring Boot 3 hỗ trợ {@code @ConfigurationProperties} trên record
 * native - không cần setter.
 *
 * <p><b>Sample yml:</b>
 * <pre>
 * app:
 *   geoip:
 *     database-path: ./docker/geoip/GeoLite2-City.mmdb
 *     fail-on-missing-database: false
 * </pre>
 *
 * @param databasePath           đường dẫn tới file {@code .mmdb}. Có thể là path
 *                               tương đối (so với working dir của process) hoặc absolute.
 * @param failOnMissingDatabase  nếu {@code true}, app fail-fast lúc start khi file
 *                               thiếu. Dùng cho production - phải đảm bảo file có
 *                               trước khi serve traffic. Dev set {@code false} để
 *                               chạy được khi chưa tải file về.
 */
@ConfigurationProperties("app.geoip")
public record GeoIpProperties(
        String databasePath,
        boolean failOnMissingDatabase
) {
}