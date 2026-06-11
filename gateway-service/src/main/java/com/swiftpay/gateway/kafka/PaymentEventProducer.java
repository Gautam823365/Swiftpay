package com.swiftpay.gateway.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${swiftpay.kafka.topics.payment-initiated}")
    private String paymentInitiatedTopic;

    public void publishPaymentInitiated(UUID transactionId, String eventJson) {

        kafkaTemplate.send(paymentInitiatedTopic, transactionId.toString(), eventJson)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed txnId={}", transactionId, ex);
                    } else {
                        log.info("Published txnId={} partition={} offset={}",
                                transactionId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}