package com.mindcash.app.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementação que não consulta API externa. Retorna vazio para que o job use rentabilidade fixa ou zero.
 */
@Service
@ConditionalOnProperty(name = "app.investments.stock-api-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpStockQuoteService implements StockQuoteService {

    @Override
    public Optional<Double> getDailyReturnPercent(String symbol, boolean isBrazil) {
        return Optional.empty();
    }
}
