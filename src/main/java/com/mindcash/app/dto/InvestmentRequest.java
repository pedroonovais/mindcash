package com.mindcash.app.dto;

import com.mindcash.app.model.InvestmentType;
import com.mindcash.app.model.RentabilityKind;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class InvestmentRequest {

    @NotNull(message = "Conta de origem é obrigatória")
    private Long sourceAccountId;

    @NotNull(message = "Conta de destino é obrigatória")
    private Long destinationAccountId;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal amount;

    @NotNull(message = "Data é obrigatória")
    private LocalDate date;

    @NotNull(message = "Tipo de investimento é obrigatório")
    private InvestmentType investmentType;

    @NotNull(message = "Rentabilidade é obrigatória")
    @DecimalMin(value = "0", message = "Rentabilidade não pode ser negativa")
    private BigDecimal rentabilityValue;

    @NotNull(message = "Unidade de rentabilidade é obrigatória")
    private RentabilityKind rentabilityKind;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String description;

    public Long getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(Long sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public Long getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public InvestmentType getInvestmentType() { return investmentType; }
    public void setInvestmentType(InvestmentType investmentType) { this.investmentType = investmentType; }

    public BigDecimal getRentabilityValue() { return rentabilityValue; }
    public void setRentabilityValue(BigDecimal rentabilityValue) { this.rentabilityValue = rentabilityValue; }

    public RentabilityKind getRentabilityKind() { return rentabilityKind; }
    public void setRentabilityKind(RentabilityKind rentabilityKind) { this.rentabilityKind = rentabilityKind; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
