package com.swiftpay.ledger.service;

import com.swiftpay.ledger.kafka.PaymentInitiatedEvent;
import com.swiftpay.ledger.kafka.PaymentResultEvent;
import com.swiftpay.ledger.model.Account;
import com.swiftpay.ledger.model.Transaction;
import com.swiftpay.ledger.model.TransactionStatus;
import com.swiftpay.ledger.repository.AccountRepository;
import com.swiftpay.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Atomically debits sender and credits receiver within a single DB transaction.
     * Accounts are locked in consistent UUID order to prevent deadlocks.
     */
    @Transactional
    public PaymentResultEvent processPayment(PaymentInitiatedEvent event) {
        MDC.put("transactionId", event.getTransactionId().toString());
        try {
            Transaction tx = transactionRepository.findById(event.getTransactionId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Transaction not found: " + event.getTransactionId()));

            if (tx.getStatus() != TransactionStatus.PENDING) {
                log.warn("Transaction {} already processed with status {}", tx.getId(), tx.getStatus());
                return buildResult(event, tx.getStatus().name(), null);
            }

            // Lock accounts in deterministic order (lower UUID first) to prevent deadlocks
            List<UUID> orderedIds = List.of(event.getSenderId(), event.getReceiverId())
                    .stream()
                    .sorted(Comparator.comparing(java.util.UUID::toString))
                    .toList();

            Account first  = accountRepository.findByIdWithLock(orderedIds.get(0))
                    .orElseThrow(() -> new IllegalStateException("Account not found: " + orderedIds.get(0)));
            Account second = accountRepository.findByIdWithLock(orderedIds.get(1))
                    .orElseThrow(() -> new IllegalStateException("Account not found: " + orderedIds.get(1)));

            Account sender   = first.getId().equals(event.getSenderId())   ? first : second;
            Account receiver = first.getId().equals(event.getReceiverId()) ? first : second;

            // Re-validate balance (double-check inside the lock)
            if (sender.getBalance().compareTo(event.getAmount()) < 0) {
                tx.setStatus(TransactionStatus.FAILED);
                tx.setUpdatedAt(Instant.now());
                transactionRepository.save(tx);
                log.warn("Payment failed — insufficient funds txnId={}", tx.getId());
                return buildResult(event, "FAILED", "Insufficient funds at processing time");
            }

            // Perform the transfer
            sender.setBalance(sender.getBalance().subtract(event.getAmount()));
            receiver.setBalance(receiver.getBalance().add(event.getAmount()));
            sender.setUpdatedAt(Instant.now());
            receiver.setUpdatedAt(Instant.now());

            accountRepository.save(sender);
            accountRepository.save(receiver);

            tx.setStatus(TransactionStatus.COMPLETED);
            tx.setUpdatedAt(Instant.now());
            transactionRepository.save(tx);

            log.info("Payment completed txnId={} sender={} receiver={} amount={}",
                    tx.getId(), sender.getId(), receiver.getId(), event.getAmount());

            return buildResult(event, "COMPLETED", null);

        } finally {
            MDC.remove("transactionId");
        }
    }

    private PaymentResultEvent buildResult(PaymentInitiatedEvent event, String status, String reason) {
        return PaymentResultEvent.builder()
                .transactionId(event.getTransactionId())
                .senderId(event.getSenderId())
                .receiverId(event.getReceiverId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .status(status)
                .failureReason(reason)
                .occurredAt(Instant.now())
                .build();
    }
}
