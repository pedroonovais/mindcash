package com.mindcash.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Request para registrar a composição do saldo existente da conta investimento em várias posições.
 */
public class InitialCompositionRequest {

    @NotNull(message = "Conta de destino é obrigatória")
    private Long destinationAccountId;

    @Valid
    @Size(min = 1, message = "Adicione pelo menos uma posição")
    private List<PositionLineDto> positions = new ArrayList<>();

    public Long getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public List<PositionLineDto> getPositions() { return positions; }
    public void setPositions(List<PositionLineDto> positions) { this.positions = positions != null ? positions : new ArrayList<>(); }
}
