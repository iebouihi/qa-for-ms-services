package com.shop.observability;

import java.util.UUID;

import org.slf4j.MDC;

/**
 * One correlation id follows a business journey across HTTP calls and Kafka events.
 * Tests send their own id, so a failing journey can be traced end to end in the logs.
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationId() {
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }

    public static String currentOrNew() {
        String id = current();
        return id != null ? id : UUID.randomUUID().toString();
    }

    public static void set(String id) {
        MDC.put(MDC_KEY, id);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
