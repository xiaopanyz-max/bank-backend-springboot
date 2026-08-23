package com.example.bank.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String GLOBAL_SERIAL_NO = "globalSerialNo";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String REQUEST_PATH = "requestPath";
    public static final String HTTP_STATUS = "httpStatus";
    public static final String COST_MS = "costMs";

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        putIfPresent(TRACE_ID, firstNonBlank(request.getHeader("X-Trace-Id"), request.getHeader("traceId")));
        if (isBlank(MDC.get(TRACE_ID))) {
            MDC.put(TRACE_ID, UUID.randomUUID().toString().replace("-", ""));
        }
        putIfPresent(GLOBAL_SERIAL_NO, firstNonBlank(
                request.getHeader("X-Global-Serial-No"),
                request.getHeader("globalSerialNo"),
                request.getParameter("globalSerialNo")));
        MDC.put(HTTP_METHOD, request.getMethod());
        MDC.put(REQUEST_PATH, request.getRequestURI());
        response.setHeader("X-Trace-Id", MDC.get(TRACE_ID));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            MDC.put(HTTP_STATUS, String.valueOf(response.getStatus()));
            MDC.put(COST_MS, String.valueOf(durationMs));
            log.info("http request method={} path={} status={} durationMs={} traceId={} globalSerialNo={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    MDC.get(TRACE_ID),
                    MDC.get(GLOBAL_SERIAL_NO));
            MDC.clear();
        }
    }

    private static void putIfPresent(String key, String value) {
        if (!isBlank(value)) {
            MDC.put(key, value);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
