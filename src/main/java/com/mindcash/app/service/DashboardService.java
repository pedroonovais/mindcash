package com.mindcash.app.service;

import com.mindcash.app.dto.DashboardData;
import com.mindcash.app.model.TransactionType;
import com.mindcash.app.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    public DashboardService(AccountService accountService, TransactionRepository transactionRepository) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
    }

    public DashboardData buildDashboard(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        BigDecimal totalBalance = accountService.getTotalBalance(userId);
        BigDecimal totalIncome = transactionRepository.sumByUserAndTypeAndPeriod(
                userId, TransactionType.CREDIT, startOfMonth, endOfMonth);
        BigDecimal totalExpense = transactionRepository.sumByUserAndTypeAndPeriod(
                userId, TransactionType.DEBIT, startOfMonth, endOfMonth);

        List<Object[]> topExpenseCategories = transactionRepository.findTopCategoriesByPeriod(
                userId, TransactionType.DEBIT, startOfMonth, endOfMonth);

        Map<String, BigDecimal> categoryBreakdown = new LinkedHashMap<>();
        int limit = Math.min(topExpenseCategories.size(), 5);
        for (int i = 0; i < limit; i++) {
            Object[] row = topExpenseCategories.get(i);
            categoryBreakdown.put((String) row[0], (BigDecimal) row[1]);
        }

        return new DashboardData(totalBalance, totalIncome, totalExpense, categoryBreakdown);
    }
}
