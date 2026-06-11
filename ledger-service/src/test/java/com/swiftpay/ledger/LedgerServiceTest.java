package com.swiftpay.ledger;

import com.swiftpay.ledger.kafka.PaymentInitiatedEvent;
import com.swiftpay.ledger.model.Account;
import com.swiftpay.ledger.model.Transaction;
import com.swiftpay.ledger.model.TransactionStatus;
import com.swiftpay.ledger.repository.AccountRepository;
import com.swiftpay.ledger.repository.TransactionRepository;
import com.swiftpay.ledger.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock TransactionRepository transactionRepository;

    @InjectMocks LedgerService ledgerService;

    private UUID senderId;
    private UUID receiverId;
    private UUID txnId;
    private Account sender;
    private Account receiver;
    private Transaction pendingTx;

    @BeforeEach
    void setUp() {
        senderId   = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        receiverId = UUID.fromString("b0000000-0000-0000-0000-000000000002");
        txnId      = UUID.randomUUID();

        sender = new Account();
        sender.setId(senderId);
        sender.setBalance(new BigDecimal("1000.00"));
        sender.setCurrency("USD");

        receiver = new Account();
        receiver.setId(receiverId);
        receiver.setBalance(new BigDecimal("200.00"));
        receiver.setCurrency("USD");

        pendingTx = new Transaction();
        pendingTx.setId(txnId);
        pendingTx.setSenderId(senderId);
        pendingTx.setReceiverId(receiverId);
        pendingTx.setAmount(new BigDecimal("300.00"));
        pendingTx.setStatus(TransactionStatus.PENDING);
    }

    @Test
    void processPayment_success_debitsAndCredits() {
        when(transactionRepository.findById(txnId)).thenReturn(Optional.of(pendingTx));
        // Return accounts in UUID-sorted order
        when(accountRepository.findByIdWithLock(senderId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdWithLock(receiverId)).thenReturn(Optional.of(receiver));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = ledgerService.processPayment(buildEvent());

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(new BigDecimal("700.00"), sender.getBalance());
        assertEquals(new BigDecimal("500.00"), receiver.getBalance());
    }

    @Test
    void processPayment_insufficientFundsAtProcessingTime_fails() {
        sender.setBalance(new BigDecimal("50.00"));
        when(transactionRepository.findById(txnId)).thenReturn(Optional.of(pendingTx));
        when(accountRepository.findByIdWithLock(senderId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdWithLock(receiverId)).thenReturn(Optional.of(receiver));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = ledgerService.processPayment(buildEvent());

        assertEquals("FAILED", result.getStatus());
        assertNotNull(result.getFailureReason());
    }

    @Test
    void processPayment_alreadyCompleted_isIdempotent() {
        pendingTx.setStatus(TransactionStatus.COMPLETED);
        when(transactionRepository.findById(txnId)).thenReturn(Optional.of(pendingTx));

        var result = ledgerService.processPayment(buildEvent());

        assertEquals("COMPLETED", result.getStatus());
        verify(accountRepository, never()).findByIdWithLock(any());
    }

    private PaymentInitiatedEvent buildEvent() {
        return PaymentInitiatedEvent.builder()
                .transactionId(txnId)
                .senderId(senderId)
                .receiverId(receiverId)
                .amount(new BigDecimal("300.00"))
                .currency("USD")
                .occurredAt(Instant.now())
                .build();
    }
}
