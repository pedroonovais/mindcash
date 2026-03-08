package com.mindcash.app.service;

import com.mindcash.app.model.RecurrenceType;
import com.mindcash.app.model.Transaction;
import com.mindcash.app.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Responsável por materializar transações recorrentes no mês atual ao abrir o dashboard.
 * Não conhece HTTP nem dashboard; apenas recebe userId e persiste as transações devidas.
 */
@Service
public class RecurrenceMaterializationService {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public RecurrenceMaterializationService(
            TransactionRepository transactionRepository,
            TransactionService transactionService) {
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }

    /**
     * Garante que todas as recorrências devidas ao mês atual estejam materializadas (uma transação real por modelo).
     * Idempotente: não duplica se já existir transação daquele modelo naquele mês.
     */
    @Transactional
    public void materializeForCurrentMonth(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        List<Transaction> templates = transactionRepository.findRecurrenceTemplatesEligibleForMonth(userId, firstDayOfMonth);

        for (Transaction template : templates) {
            if (template.getRecurrenceNextYm() == null) {
                continue;
            }

            if (template.getRecurrenceType() == RecurrenceType.LIMITED_MONTHS) {
                long generatedCount = transactionRepository.countByRecurrenceParentId(template.getId());
                if (generatedCount >= template.getRecurrenceCount()) {
                    template.setRecurrenceNextYm(null);
                    transactionRepository.save(template);
                    continue;
                }
            }

            LocalDate monthStart = template.getRecurrenceNextYm();
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            boolean alreadyExists = transactionRepository.existsByRecurrenceParentIdAndDateBetween(
                    template.getId(), monthStart, monthEnd);
            if (alreadyExists) {
                continue;
            }

            int dayOfMonth = Math.min(template.getDate().getDayOfMonth(), monthStart.lengthOfMonth());
            LocalDate targetDate = monthStart.withDayOfMonth(dayOfMonth);

            transactionService.createFromRecurrenceTemplate(template, targetDate);

            LocalDate nextMonth = monthStart.plusMonths(1);
            if (template.getRecurrenceType() == RecurrenceType.LIMITED_MONTHS) {
                long newCount = transactionRepository.countByRecurrenceParentId(template.getId());
                if (newCount >= template.getRecurrenceCount()) {
                    nextMonth = null;
                }
            }
            template.setRecurrenceNextYm(nextMonth);
            transactionRepository.save(template);
        }
    }
}
