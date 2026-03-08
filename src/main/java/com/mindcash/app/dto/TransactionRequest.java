package com.mindcash.app.dto;

import com.mindcash.app.model.RecurrenceType;
import com.mindcash.app.model.TransactionType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionRequest {

    @NotNull(message = "Conta é obrigatória")
    private Long accountId;

    private Long categoryId;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal amount;

    @NotNull(message = "Tipo é obrigatório")
    private TransactionType type;

    @NotNull(message = "Data é obrigatória")
    private LocalDate date;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String description;

    private RecurrenceType recurrenceType;

    @Min(value = 2, message = "Quantidade de meses deve ser no mínimo 2")
    @Max(value = 120, message = "Quantidade de meses deve ser no máximo 120")
    private Integer recurrenceCount;

    @AssertTrue(message = "Para recorrência por X meses, informe a quantidade de meses")
    public boolean isRecurrenceCountValid() {
        if (recurrenceType != RecurrenceType.LIMITED_MONTHS) {
            return true;
        }
        return recurrenceCount != null && recurrenceCount >= 2 && recurrenceCount <= 120;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RecurrenceType getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(RecurrenceType recurrenceType) { this.recurrenceType = recurrenceType; }

    public Integer getRecurrenceCount() { return recurrenceCount; }
    public void setRecurrenceCount(Integer recurrenceCount) { this.recurrenceCount = recurrenceCount; }
}
