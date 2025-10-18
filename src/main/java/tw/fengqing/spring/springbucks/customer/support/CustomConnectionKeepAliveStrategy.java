package tw.fengqing.spring.springbucks.customer.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.core5.util.TimeValue;

import java.util.Arrays;

public class CustomConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {
    private final long DEFAULT_SECONDS = 30;

    @Override
    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
        // 取得所有 "Connection" header，過濾出名稱為 "timeout" 且值為數字的 header
        long milliseconds = Arrays.stream(response.getHeaders("Connection"))
                .filter(h -> StringUtils.equalsIgnoreCase(h.getName(), "timeout")
                        && StringUtils.isNumeric(h.getValue()))
                .findFirst()
                .map(h -> NumberUtils.toLong(h.getValue(), DEFAULT_SECONDS))
                .orElse(DEFAULT_SECONDS) * 1000;
        return TimeValue.ofMilliseconds(milliseconds);
    }
}
