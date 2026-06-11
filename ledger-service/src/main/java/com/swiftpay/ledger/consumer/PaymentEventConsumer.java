package com.swiftpay.ledger.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.ledger.kafka.PaymentInitiatedEvent;
import com.swiftpay.ledger.kafka.PaymentResultEvent;
import com.swiftpay.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${swiftpay.kafka.topics.payment-completed}")
    private String paymentCompletedTopic;

    @Value("${swiftpay.kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    @KafkaListener(
            topics = "${swiftpay.kafka.topics.payment-initiated}",
            groupId = "ledger-service-group"
    )
    public void consume(String message) {

        try {
            PaymentInitiatedEvent event =
                    objectMapper.readValue(message, PaymentInitiatedEvent.class);

            log.info("Received txnId={}", event.getTransactionId());

            PaymentResultEvent result = ledgerService.processPayment(event);

            String topic = "COMPLETED".equals(result.getStatus())
                    ? paymentCompletedTopic
                    : paymentFailedTopic;

            kafkaTemplate.send(
                    topic,
                    event.getTransactionId().toString(),
                    objectMapper.writeValueAsString(result)
            );

            log.info("Processed txnId={} status={}",
                    event.getTransactionId(), result.getStatus());

        } catch (Exception e) {
            log.error("Failed to process message: {}", message, e);
        }
    }
}