package com.mindcash.app.model;

public enum InvestmentType {
    RENDA_FIXA_CDI("Renda Fixa CDI"),
    RENDA_FIXA_PREFIXADO("Renda Fixa Prefixado"),
    ACOES_BRASIL("Ações Brasil"),
    ACOES_USA("Stocks EUA"),
    CRIPTO("Criptoativos"),
    OUTROS("Outros");

    private final String label;

    InvestmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
