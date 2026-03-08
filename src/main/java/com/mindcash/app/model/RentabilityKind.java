package com.mindcash.app.model;

public enum RentabilityKind {
    PERCENT_CDI("% CDI"),
    PERCENT_AA("% a.a."),
    OUTROS("Outros");

    private final String label;

    RentabilityKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
