package com.personal.identity.core.domain.session;

/**
 * <b>PORT</b>: parses the User-Agent header into {@link DeviceInfo}.
 *
 * <p>Default implementation: {@code YauaaUserAgentParser} utilizing the
 * <a href="https://github.com/nielsbasjes/yauaa">Yauaa</a> library at the infrastructure layer.
 *
 * <p>Why is this a port? To enable swapping into a different implementation (e.g., an external API
 * or a lighter parser) in the future without modifying the core.
 */
public interface UserAgentParse {
    /**
     * Parses the raw User-Agent string. DO NOT throw exceptions - if parsing fails or
     * the input is null/blank, return {@link DeviceInfo#unknown()}.
     */
    DeviceInfo parse(String rawUserAgent);
}
