package com.mindcash.app.service;

import java.util.Optional;

/**
 * Serviço para obter a variação diária (retorno em %) de um ativo negociado (ex.: ação).
 * Usado pelo job diário para atualizar currentValue de investimentos em ações.
 */
public interface StockQuoteService {

    /**
     * Retorna a variação diária em percentual (ex.: 1.5 para +1,5%).
     * Para ações Brasil, o símbolo pode ser informado com ou sem sufixo .SA.
     *
     * @param symbol   ticker do ativo (ex.: PETR4, PETR4.SA, NVDA)
     * @param isBrazil true para ações B3 (adiciona .SA se necessário)
     * @return variação em % ou vazio se indisponível
     */
    Optional<Double> getDailyReturnPercent(String symbol, boolean isBrazil);
}
