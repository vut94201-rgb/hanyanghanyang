package com.personal.identity.infrastructure.useragent;


import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Config tách riêng cho yauaa {@link UserAgentAnalyzer}.
 *
 * <p><b>Vì sao tách config khỏi adapter:</b> {@code UserAgentAnalyzer.newBuilder().build()}
 * tốn ~2-5 giây ở lần đầu vì phải load và compile hàng ngàn rule YAML nội bộ.
 * Khai báo bean singleton ở đây để Spring cache instance - các request login về
 * sau chỉ phí ~0.1ms để parse 1 User-Agent (nhờ in-memory cache 10000 entry bên dưới).
 *
 * <p><b>{@code withField(...)}:</b> chỉ yêu cầu các field thực sự dùng. Yauaa cho phép
 * lấy ~80 field, nhưng càng nhiều field thì RAM càng cao (mỗi field giữ rule riêng).
 * Ta chỉ cần 5 field để dựng {@link com.personal.identity.core.session.DeviceInfo}.
 *
 * <p><b>{@code withCache(10000):}</b> mỗi entry là 1 string User-Agent → object kết quả.
 * 10000 là default của yauaa - đủ cho dev và workload trung bình. Có thể tune sau
 * nếu metric cho thấy cache hit rate thấp.
 */
@Configuration
public class YauaaConfig {

    private static final int CACHE_SIZE = 10000;

    @Bean
    public UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer.newBuilder()
                .withCache(CACHE_SIZE)
                // 5 field cần cho DeviceInfo - thứ tự không quan trọng:
                .withField(UserAgent.DEVICE_CLASS)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .withField(UserAgent.OPERATING_SYSTEM_VERSION)
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.AGENT_VERSION)
                // Không log warning khi UA không match rule nào - quá ồn trong production.
                .hideMatcherLoadStats()
                .build();
    }
}