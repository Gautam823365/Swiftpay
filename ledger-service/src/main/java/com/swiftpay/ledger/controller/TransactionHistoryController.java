package com.swiftpay.ledger.controller;

import com.swiftpay.ledger.model.Transaction;
import com.swiftpay.ledger.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/ledger")
@RequiredArgsConstructor
@Tag(name = "Ledger", description = "Transaction history and reporting")
public class TransactionHistoryController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<Page<Transaction>> getHistory(
            @PathVariable("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> history = transactionRepository.findByUserId(userId, pageable);

        return ResponseEntity.ok(history);
    }
}
