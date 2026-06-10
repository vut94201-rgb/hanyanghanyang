package com.personal.identity.infrastructure.useragent;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dedicated configuration for yauaa's {@link UserAgentAnalyzer}.
 *
 * <p><b>Why separate configuration from the adapter:</b> {@code UserAgentAnalyzer.newBuilder().build()}
 * takes ~2-5 seconds upon initial execution as it must load and compile thousands of internal YAML rules.
 * Declaring it as a singleton bean here allows Spring to cache the instance — subsequent login requests
 * incur a parse cost of only ~0.1ms per User-Agent (thanks to the underlying 10,000-entry in-memory cache).
 *
 * <p><b>{@code withField(...)}:</b> Strictly requests only the fields genuinely in use. Yauaa can extract
 * ~80 fields, but increased field counts correlate with higher RAM consumption (as each field maintains
 * its own evaluation rules). We only require 5 fields to construct a {@link com.personal.identity.core.domain.session.DeviceInfo} object.
 *
 * <p><b>{@code withCache(10000)}:</b> Each entry maps a User-Agent string to its parsed object result.
 * 10,000 is yauaa's default capacity — sufficient for development and moderate workloads. Can be tuned
 * later if observability metrics indicate a low cache hit rate.
 */
@Configuration
public class YauaaConfig {

    private static final int CACHE_SIZE = 10000;

    @Bean
    public UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer.newBuilder()
                .withCache(CACHE_SIZE)
                // 5 fields required for DeviceInfo - order is irrelevant:
                .withField(UserAgent.DEVICE_CLASS)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .withField(UserAgent.OPERATING_SYSTEM_VERSION)
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.AGENT_VERSION)
                // Suppresses warnings when a UA fails to match any rule - prevents excessive log noise in production.
                .hideMatcherLoadStats()
                .build();
    }
}