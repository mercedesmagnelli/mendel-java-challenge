package com.mendel.transactions.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransactionRequest(
        @NotNull(message = "is required") Double amount,
        @NotBlank(message = "is required") String type,
        @JsonProperty("parent_id") Long parentId) {
}
