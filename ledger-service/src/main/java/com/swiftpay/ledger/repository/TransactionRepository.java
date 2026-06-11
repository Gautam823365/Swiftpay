package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.model.Transaction;
import com.swiftpay.ledger.model.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT t FROM Transaction t WHERE t.senderId = :userId OR t.receiverId = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    Page<Transaction> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(UUID senderId, UUID receiverId, Pageable pageable);
}
