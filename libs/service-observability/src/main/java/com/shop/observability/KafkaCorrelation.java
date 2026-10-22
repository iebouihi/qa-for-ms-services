package com.shop.observability;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.listener.RecordInterceptor;

/** Carries the correlation id through Kafka headers, in both directions. */
public final class KafkaCorrelation {

    private KafkaCorrelation() {
    }

    /** Copies the current correlation id onto an outgoing record. */
    public static <K, V> ProducerRecord<K, V> propagate(ProducerRecord<K, V> record) {
        record.headers().add(CorrelationId.HEADER, CorrelationId.currentOrNew().getBytes(StandardCharsets.UTF_8));
        return record;
    }

    public static String from(Headers headers) {
        Header header = headers.lastHeader(CorrelationId.HEADER);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    /** Restores the correlation id into the logging context before a listener runs. */
    public static RecordInterceptor<Object, Object> recordInterceptor() {
        return new RecordInterceptor<>() {
            @Override
            public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                            Consumer<Object, Object> consumer) {
                String id = from(record.headers());
                if (id != null) {
                    CorrelationId.set(id);
                }
                return record;
            }

            @Override
            public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
                CorrelationId.clear();
            }
        };
    }
}
