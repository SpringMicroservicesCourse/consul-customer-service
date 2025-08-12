package tw.fengqing.spring.springbucks.customer.support;

import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;

public class CustomConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {
    private final long DEFAULT_SECONDS = 30;

    @Override
    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
        // 簡化實現：直接返回預設的 Keep-Alive 時間
        return TimeValue.ofSeconds(DEFAULT_SECONDS);
    }
}
