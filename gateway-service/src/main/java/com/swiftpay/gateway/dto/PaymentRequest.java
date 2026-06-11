package com.swiftpay.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Payment request payload")
public class PaymentRequest {

    @NotNull
    @Schema(description = "Idempotency key — reuse to safely retry", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID idempotencyKey;

    @NotNull
    @Schema(description = "Sender account UUID")
    private UUID senderId;

    @NotNull
    @Schema(description = "Receiver account UUID")
    private UUID receiverId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 4)
    @Schema(description = "Transfer amount", example = "100.00")
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Schema(description = "ISO 4217 currency code", example = "USD")
    private String currency;
}
