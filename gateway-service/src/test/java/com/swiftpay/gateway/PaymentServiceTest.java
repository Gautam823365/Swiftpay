package com.swiftpay.gateway;

import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.kafka.PaymentEventProducer;
import com.swiftpay.gateway.model.Account;
import com.swiftpay.gateway.model.Transaction;
import com.swiftpay.gateway.model.TransactionStatus;
import com.swiftpay.gateway.repository.AccountRepository;
import com.swiftpay.gateway.repository.TransactionRepository;
import com.swiftpay.gateway.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AccountRepository accountRepository;
    @Mock PaymentEventProducer eventProducer;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks PaymentService paymentService;

    private Account senderAccount;
    private Account receiverAccount;

    @BeforeEach
    void setUp() {
        senderAccount = new Account();
        senderAccount.setId(UUID.randomUUID());
        senderAccount.setBalance(new BigDecimal("1000.00"));
        senderAccount.setCurrency("USD");

        receiverAccount = new Account();
        receiverAccount.setId(UUID.randomUUID());
        receiverAccount.setBalance(new BigDecimal("500.00"));
        receiverAccount.setCurrency("USD");

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void initiatePayment_success() {

        when(valueOps.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                .thenReturn(true);

        when(accountRepository.findById(senderAccount.getId()))
                .thenReturn(Optional.of(senderAccount));

        when(accountRepository.findById(receiverAccount.getId()))
                .thenReturn(Optional.of(receiverAccount));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var request = buildRequest(senderAccount.getId(), receiverAccount.getId(), "100.00");

        var response = paymentService.initiatePayment(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.PENDING, response.getStatus());

        verify(eventProducer, times(1))
                        .publishPaymentInitiated(any(UUID.class), anyString());
    }

    @Test
    void initiatePayment_insufficientFunds_throws() {

        when(valueOps.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                .thenReturn(true);

        senderAccount.setBalance(new BigDecimal("10.00"));

        when(accountRepository.findById(senderAccount.getId()))
                .thenReturn(Optional.of(senderAccount));

        var request = buildRequest(senderAccount.getId(), receiverAccount.getId(), "500.00");

        assertThrows(PaymentService.InsufficientFundsException.class,
                () -> paymentService.initiatePayment(request));

        verify(eventProducer, never())
                .publishPaymentInitiated(any(UUID.class), anyString());
    }

    @Test
    void initiatePayment_duplicate_throws() {

        when(valueOps.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                .thenReturn(false);

        when(transactionRepository.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());

        var request = buildRequest(senderAccount.getId(), receiverAccount.getId(), "100.00");

        assertThrows(PaymentService.DuplicatePaymentException.class,
                () -> paymentService.initiatePayment(request));
    }

    @Test
    void initiatePayment_senderNotFound_throws() {

        when(valueOps.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                .thenReturn(true);

        when(accountRepository.findById(senderAccount.getId()))
                .thenReturn(Optional.empty());

        var request = buildRequest(senderAccount.getId(), receiverAccount.getId(), "100.00");

        assertThrows(PaymentService.AccountNotFoundException.class,
                () -> paymentService.initiatePayment(request));
    }

    private PaymentRequest buildRequest(UUID sender, UUID receiver, String amount) {
        var r = new PaymentRequest();
        r.setIdempotencyKey(UUID.randomUUID());
        r.setSenderId(sender);
        r.setReceiverId(receiver);
        r.setAmount(new BigDecimal(amount));
        r.setCurrency("USD");
        return r;
    }
}