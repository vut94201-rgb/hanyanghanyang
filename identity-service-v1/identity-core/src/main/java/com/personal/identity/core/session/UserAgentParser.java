package com.personal.identity.core.session;

/**
 * <b>PORT</b>: parse User-Agent header thành {@link DeviceInfo}.
 *
 * <p>Implementation mặc định: {@code YauaaUserAgentParser} dùng thư viện
 * <a href="https://github.com/nielsbasjes/yauaa">yauaa</a> ở infrastructure.
 *
 * <p>Vì sao là port? Để có thể swap sang implementation khác (vd: API ngoài,
 * hoặc một parser nhẹ hơn) mà không phải sửa core.
 */
public interface UserAgentParser {

    /**
     * Parse raw User-Agent. KHÔNG throw - nếu parse fail hoặc input null/blank,
     * trả về {@link DeviceInfo#unknown()}.
     */
    DeviceInfo parse(String rawUserAgent);
}
