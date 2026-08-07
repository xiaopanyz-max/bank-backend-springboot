package com.example.bank.gateway.filter;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Logs the route selected by the gateway and the final load-balanced downstream address. */
@Component
public class GatewayRequestLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayRequestLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startedAt = System.nanoTime();
        return chain.filter(exchange).doFinally(signalType -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            URI downstreamUri = exchange.getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("gateway request method={} path={} routeId={} downstream={} status={} durationMs={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getRawPath(),
                    route == null ? "unmatched" : route.getId(),
                    downstreamUri,
                    status == null ? "not-committed" : status.value(),
                    durationMs);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
