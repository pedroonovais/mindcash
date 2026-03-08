package com.mindcash.app.dto;

import com.mindcash.app.model.InvestmentType;
import com.mindcash.app.model.RentabilityKind;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Representa uma linha da composição do saldo inicial (uma posição).
 */
public class PositionLineDto {

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal amount;

    @NotNull(message = "Tipo de investimento é obrigatório")
    private InvestmentType investmentType;

    @NotNull(message = "Rentabilidade é obrigatória")
    @DecimalMin(value = "0", message = "Rentabilidade não pode ser negativa")
    private BigDecimal rentabilityValue = BigDecimal.ZERO;

    @NotNull(message = "Unidade de rentabilidade é obrigatória")
    private RentabilityKind rentabilityKind = RentabilityKind.OUTROS;

    @Size(max = 100)
    private String assetName;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public InvestmentType getInvestmentType() { return investmentType; }
    public void setInvestmentType(InvestmentType investmentType) { this.investmentType = investmentType; }

    public BigDecimal getRentabilityValue() { return rentabilityValue; }
    public void setRentabilityValue(BigDecimal rentabilityValue) { this.rentabilityValue = rentabilityValue; }

    public RentabilityKind getRentabilityKind() { return rentabilityKind; }
    public void setRentabilityKind(RentabilityKind rentabilityKind) { this.rentabilityKind = rentabilityKind; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }
}
