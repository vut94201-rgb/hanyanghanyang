package com.personal.identity.infrastructure.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.personal.identity.core.session.GeoLocation;
import com.personal.identity.core.session.GeoLocationResolver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Adapter implements {@link GeoLocationResolver} (port của core) dùng MaxMind
 * {@code GeoLite2-City.mmdb} offline.
 *
 * <p><b>Vì sao offline (file .mmdb) thay vì API ngoài như ipinfo.io:</b>
 * <ul>
 *   <li>Không phụ thuộc network → luồng login không bị kéo dài/fail vì 1 API ngoài.</li>
 *   <li>Không tốn rate-limit, không tốn tiền/lần gọi.</li>
 *   <li>Privacy tốt hơn - không gửi IP của user ra ngoài.</li>
 *   <li>Trade-off: phải tự cập nhật file mỗi tháng (cron hoặc thủ công).</li>
 * </ul>
 *
 * <p><b>Graceful degradation khi thiếu file:</b> Dev nhiều khi chưa kịp tải
 * {@code .mmdb} về. Nếu {@code fail-on-missing-database = false}, adapter vẫn
 * register làm bean nhưng {@code reader = null}; mọi request {@code resolve()}
 * trả {@link GeoLocation#empty()}. Production set {@code true} để fail-fast.
 *
 * <p><b>IP private/loopback:</b> {@code 192.168.x.x}, {@code 10.x.x.x},
 * {@code 127.0.0.1} không có trong DB - skip luôn, không cần gọi
 * {@code DatabaseReader.city()} (tránh exception không cần thiết).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(GeoIpProperties.class)
public class MaxMindGeoLocationResolver implements GeoLocationResolver {

    private final GeoIpProperties properties;

    /**
     * Null nếu file thiếu và {@code failOnMissingDatabase=false}.
     * DatabaseReader của MaxMind là thread-safe, chia sẻ được cho mọi request.
     */
    private DatabaseReader reader;

    @PostConstruct
    void init() {
        String path = properties.databasePath();
        if (path == null || path.isBlank()) {
            handleMissingDatabase("app.geoip.database-path chưa cấu hình");
            return;
        }

        File db = new File(path);
        if (!db.exists()) {
            handleMissingDatabase("Không tìm thấy file GeoIP database tại: " + db.getAbsolutePath());
            return;
        }

        try {
            // withCache: MaxMind tự cache kết quả trong RAM. CHM với 256 entry là default.
            this.reader = new DatabaseReader.Builder(db).build();
            log.info("GeoIP database loaded từ: {}", db.getAbsolutePath());
        } catch (IOException e) {
            handleMissingDatabase("Lỗi đọc GeoIP database: " + e.getMessage());
        }
    }

    @PreDestroy
    void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                log.warn("Lỗi đóng GeoIP DatabaseReader", e);
            }
        }
    }

    @Override
    public GeoLocation resolve(String ipAddress) {
        // Reader null = file thiếu (đã warn ở init). Mọi request resolve trả empty.
        if (reader == null || ipAddress == null || ipAddress.isBlank()) {
            return GeoLocation.empty();
        }

        InetAddress addr;
        try {
            addr = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            // IP format sai (vd: "abc.def") - không log, request giả/spam có thể gửi gì cũng được.
            return GeoLocation.empty();
        }

        // Skip IP không thể có trong GeoLite2-City:
        // - loopback: 127.0.0.0/8
        // - any-local: 0.0.0.0
        // - site-local: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16 (RFC 1918)
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isSiteLocalAddress()) {
            return GeoLocation.empty();
        }

        try {
            CityResponse response = reader.city(addr);
            return new GeoLocation(
                    response.country().name(),
                    response.country().isoCode(),
                    response.city().name(),
                    response.location().latitude(),
                    response.location().longitude()
            );
        } catch (AddressNotFoundException e) {
            // IP hợp lệ nhưng không có trong DB - chuyện bình thường, không log.
            return GeoLocation.empty();
        } catch (IOException | GeoIp2Exception e) {
            // Lỗi I/O hoặc lib - log WARN, không throw để không block luồng login.
            log.warn("Resolve GeoIP fail cho IP {}: {}", ipAddress, e.getMessage());
            return GeoLocation.empty();
        }
    }

    /**
     * Tuỳ {@code failOnMissingDatabase}, fail-fast hoặc log WARN và để {@code reader = null}.
     */
    private void handleMissingDatabase(String reason) {
        if (properties.failOnMissingDatabase()) {
            throw new IllegalStateException(
                    "GeoIP database không khả dụng và app.geoip.fail-on-missing-database=true. "
                            + "Lý do: " + reason
            );
        }
        log.warn("GeoIP database không khả dụng - resolve() sẽ trả GeoLocation.empty() cho mọi IP. "
                + "Lý do: {}", reason);
    }
}