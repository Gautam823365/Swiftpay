package com.swiftpay.gateway.service;

import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.dto.PaymentResponse;
import com.swiftpay.gateway.kafka.PaymentEventProducer;
import com.swiftpay.gateway.model.Account;
import com.swiftpay.gateway.model.Transaction;
import com.swiftpay.gateway.model.TransactionStatus;
import com.swiftpay.gateway.repository.AccountRepository;
import com.swiftpay.gateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PaymentEventProducer eventProducer;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {

        String idempotencyKey = request.getIdempotencyKey().toString();
        MDC.put("transactionId", idempotencyKey);

        try {

            // 1. Idempotency check
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(IDEMPOTENCY_PREFIX + idempotencyKey, "processing", IDEMPOTENCY_TTL);

            if (Boolean.FALSE.equals(isNew)) {
                return transactionRepository.findByIdempotencyKey(idempotencyKey)
                        .map(tx -> buildResponse(tx, "Duplicate request"))
                        .orElseThrow(() ->
                                new DuplicatePaymentException("Duplicate idempotency key"));
            }

            // 2. Sender validation
            Account sender = accountRepository.findById(request.getSenderId())
                    .orElseThrow(() -> new AccountNotFoundException("Sender not found"));

            if (sender.getBalance().compareTo(request.getAmount()) < 0) {
                redisTemplate.delete(IDEMPOTENCY_PREFIX + idempotencyKey);
                throw new InsufficientFundsException("Insufficient funds");
            }

            // 3. Receiver validation
            accountRepository.findById(request.getReceiverId())
                    .orElseThrow(() -> new AccountNotFoundException("Receiver not found"));

            // 4. Save transaction
            Transaction tx = new Transaction();
            tx.setId(UUID.randomUUID());
            tx.setSenderId(request.getSenderId());
            tx.setReceiverId(request.getReceiverId());
            tx.setAmount(request.getAmount());
            tx.setCurrency(request.getCurrency());
            tx.setStatus(TransactionStatus.PENDING);
            tx.setIdempotencyKey(idempotencyKey);
            tx.setCreatedAt(Instant.now());
            tx.setUpdatedAt(Instant.now());

            transactionRepository.save(tx);

            // 5. Build JSON event (NO DTO)
            String eventJson = """
            {
              "transactionId": "%s",
              "senderId": "%s",
              "receiverId": "%s",
              "amount": %s,
              "currency": "%s",
              "occurredAt": "%s"
            }
            """.formatted(
                    tx.getId(),
                    tx.getSenderId(),
                    tx.getReceiverId(),
                    tx.getAmount(),
                    tx.getCurrency(),
                    Instant.now()
            );

            // 6. Send to Kafka
            eventProducer.publishPaymentInitiated(tx.getId(), eventJson);

            log.info("Payment initiated txnId={}", tx.getId());

            return buildResponse(tx, "Payment queued");

        } finally {
            MDC.remove("transactionId");
        }
    }

    private PaymentResponse buildResponse(Transaction tx, String message) {
        return PaymentResponse.builder()
                .transactionId(tx.getId())
                .status(tx.getStatus())
                .senderId(tx.getSenderId())
                .receiverId(tx.getReceiverId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .createdAt(tx.getCreatedAt())
                .message(message)
                .build();
    }

    // exceptions
    public static class DuplicatePaymentException extends RuntimeException {
        public DuplicatePaymentException(String msg) { super(msg); }
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String msg) { super(msg); }
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String msg) { super(msg); }
    }
}