package com.example.bank.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Limits simultaneous requests handled by this gateway instance.
 * This is intentionally in-memory: use a shared limiter such as Redis when running multiple gateway instances.
 */
@Component
public class ConcurrentRequestLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentRequestLimitFilter.class);

    private final Semaphore permits;
    private final int maxInFlight;

    public ConcurrentRequestLimitFilter(@Value("${gateway.concurrency.max-in-flight:2}") int maxInFlight) {
        if (maxInFlight < 1) {
            throw new IllegalArgumentException("gateway.concurrency.max-in-flight must be at least 1");
        }
        this.maxInFlight = maxInFlight;
        this.permits = new Semaphore(maxInFlight, true);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!permits.tryAcquire()) {
            log.warn("gateway request rejected reason=concurrency_limit method={} path={} maxInFlight={}",
                    exchange.getRequest().getMethod(), exchange.getRequest().getURI().getRawPath(), maxInFlight);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, "1");
            exchange.getResponse().getHeaders().set("X-Gateway-Concurrency-Limit", String.valueOf(maxInFlight));
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DataBuffer body = exchange.getResponse().bufferFactory().wrap(
                    "{\"code\":\"429\",\"message\":\"Gateway concurrent request limit exceeded\"}"
                            .getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(body));
        }
        return chain.filter(exchange).doFinally(signalType -> permits.release());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
