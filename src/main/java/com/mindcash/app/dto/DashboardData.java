package com.mindcash.app.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardData(
        BigDecimal totalBalance,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        Map<String, BigDecimal> topExpenseCategories
) {
    public BigDecimal monthlyBalance() {
        return totalIncome.subtract(totalExpense);
    }
}
