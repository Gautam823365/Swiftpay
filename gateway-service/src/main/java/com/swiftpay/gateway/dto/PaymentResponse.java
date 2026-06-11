package com.swiftpay.gateway.dto;

import com.swiftpay.gateway.model.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Payment initiation response")
public class PaymentResponse {

    @Schema(description = "Transaction ID")
    private UUID transactionId;

    @Schema(description = "Current status")
    private TransactionStatus status;

    private UUID senderId;
    private UUID receiverId;
    private BigDecimal amount;
    private String currency;
    private Instant createdAt;
    private String message;
}
