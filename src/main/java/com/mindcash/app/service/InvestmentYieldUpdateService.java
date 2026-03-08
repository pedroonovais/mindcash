package com.mindcash.app.service;

import com.mindcash.app.model.*;
import com.mindcash.app.repository.AccountRepository;
import com.mindcash.app.repository.InvestmentRepository;
import com.mindcash.app.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Atualiza rendimentos de investimentos de rentabilidade fixa (CDI / % a.a.) ao abrir a página
 * de investimentos: verifica dias sem rendimento registrado, cria uma transação RENDIMENTO por dia,
 * atualiza currentValue e saldo da conta. Rendimentos ficam visíveis na listagem de transações.
 */
@Service
public class InvestmentYieldUpdateService {

    private static final int SCALE = 10;

    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Value("${app.investments.cdi-annual-rate:12.0}")
    private double cdiAnnualRatePercent;

    public InvestmentYieldUpdateService(InvestmentRepository investmentRepository,
                                         TransactionRepository transactionRepository,
                                         AccountRepository accountRepository) {
        this.investmentRepository = investmentRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Garante que existam transações de rendimento para todos os dias desde o último registro
     * até hoje, apenas para investimentos elegíveis (PERCENT_CDI ou PERCENT_AA com valor > 0).
     */
    @Transactional
    public void ensureYieldTransactionsUpToToday(Long userId) {
        List<Investment> eligible = investmentRepository.findByUserIdWithAccountsOrderByDateDesc(userId)
                .stream()
                .filter(inv -> inv.getRentabilityKind() == RentabilityKind.PERCENT_CDI
                        || inv.getRentabilityKind() == RentabilityKind.PERCENT_AA)
                .filter(inv -> inv.getRentabilityValue() != null
                        && inv.getRentabilityValue().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        LocalDate today = LocalDate.now();
        for (Investment inv : eligible) {
            Optional<LocalDate> lastYieldDateOpt = transactionRepository
                    .findMaxDateByInvestmentIdAndType(inv.getId(), TransactionType.RENDIMENTO);
            LocalDate lastYieldDate = lastYieldDateOpt.orElse(inv.getDate());
            LocalDate startDate = lastYieldDate.plusDays(1);
            BigDecimal initialValue = lastYieldDateOpt.isEmpty()
                    ? inv.getAmount()
                    : inv.getCurrentValue();
            if (!startDate.isAfter(today)) {
                backfillYieldTransactions(inv, startDate, today, initialValue);
            }
        }
    }

    private void backfillYieldTransactions(Investment inv, LocalDate startDate, LocalDate endDate, BigDecimal valueAtStartOfPeriod) {
        BigDecimal dailyRate = getDailyRateByRentability(inv.getRentabilityKind(), inv.getRentabilityValue());
        if (dailyRate.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal runningValue = valueAtStartOfPeriod;
        LocalDate day = startDate;
        while (!day.isAfter(endDate)) {
            BigDecimal yieldAmount = runningValue.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
            if (yieldAmount.compareTo(BigDecimal.ZERO) > 0) {
                Transaction tx = new Transaction();
                tx.setUser(inv.getUser());
                tx.setAccount(inv.getDestinationAccount());
                tx.setAmount(yieldAmount);
                tx.setType(TransactionType.RENDIMENTO);
                tx.setDate(day);
                tx.setDescription(buildYieldDescription(inv));
                tx.setInvestment(inv);

                applyBalanceChange(inv.getDestinationAccount(), yieldAmount);
                accountRepository.save(inv.getDestinationAccount());

                runningValue = runningValue.add(yieldAmount);
                inv.setCurrentValue(runningValue);
                investmentRepository.save(inv);

                transactionRepository.save(tx);
            }
            day = day.plusDays(1);
        }
    }

    private String buildYieldDescription(Investment inv) {
        String base = inv.getDescription() != null && !inv.getDescription().isBlank()
                ? inv.getDescription()
                : (inv.getAssetName() != null && !inv.getAssetName().isBlank()
                ? inv.getAssetName()
                : inv.getInvestmentType().getLabel());
        return "Rendimento - " + base;
    }

    private BigDecimal getDailyRateByRentability(RentabilityKind kind, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        switch (kind) {
            case PERCENT_AA: {
                double annual = value.doubleValue() / 100.0;
                double daily = Math.pow(1 + annual, 1.0 / 365.0) - 1.0;
                return BigDecimal.valueOf(daily).setScale(SCALE, RoundingMode.HALF_UP);
            }
            case PERCENT_CDI: {
                double cdiFraction = (cdiAnnualRatePercent / 100.0) * (value.doubleValue() / 100.0);
                double dailyCdi = Math.pow(1 + cdiFraction, 1.0 / 365.0) - 1.0;
                return BigDecimal.valueOf(dailyCdi).setScale(SCALE, RoundingMode.HALF_UP);
            }
            default:
                return BigDecimal.ZERO;
        }
    }

    private void applyBalanceChange(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
    }
}
