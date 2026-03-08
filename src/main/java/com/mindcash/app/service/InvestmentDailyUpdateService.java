package com.mindcash.app.service;

import com.mindcash.app.model.*;
import com.mindcash.app.repository.AccountRepository;
import com.mindcash.app.repository.InvestmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Atualiza diariamente o currentValue de cada investimento com base na rentabilidade
 * e recalcula o saldo das contas de investimento.
 */
@Service
public class InvestmentDailyUpdateService {

    private static final int SCALE = 10;

    private final InvestmentRepository investmentRepository;
    private final AccountRepository accountRepository;
    private final StockQuoteService stockQuoteService;

    @Value("${app.investments.cdi-annual-rate:12.0}")
    private double cdiAnnualRatePercent;

    public InvestmentDailyUpdateService(InvestmentRepository investmentRepository,
                                        AccountRepository accountRepository,
                                        StockQuoteService stockQuoteService) {
        this.investmentRepository = investmentRepository;
        this.accountRepository = accountRepository;
        this.stockQuoteService = stockQuoteService;
    }

    @Scheduled(cron = "${app.investments.daily-update-cron:0 0 2 * * ?}")
    @Transactional
    public void runDailyUpdate() {
        List<Investment> all = investmentRepository.findAll();
        for (Investment inv : all) {
            BigDecimal dailyRate = getDailyRate(inv);
            BigDecimal newValue = inv.getCurrentValue().multiply(BigDecimal.ONE.add(dailyRate)).setScale(2, RoundingMode.HALF_UP);
            inv.setCurrentValue(newValue);
            investmentRepository.save(inv);
        }

        Map<Long, BigDecimal> balanceByAccountId = all.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getDestinationAccount().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Investment::getCurrentValue, BigDecimal::add)
                ));

        for (Map.Entry<Long, BigDecimal> e : balanceByAccountId.entrySet()) {
            accountRepository.findById(e.getKey()).ifPresent(account -> {
                account.setBalance(e.getValue());
                accountRepository.save(account);
            });
        }
    }

    private BigDecimal getDailyRate(Investment inv) {
        InvestmentType type = inv.getInvestmentType();
        String assetName = inv.getAssetName();
        boolean isStockWithTicker = (type == InvestmentType.ACOES_BRASIL || type == InvestmentType.ACOES_USA)
                && assetName != null && !assetName.isBlank();

        if (isStockWithTicker) {
            boolean isBrazil = type == InvestmentType.ACOES_BRASIL;
            Optional<Double> pct = stockQuoteService.getDailyReturnPercent(assetName.trim(), isBrazil);
            if (pct.isPresent()) {
                return BigDecimal.valueOf(pct.get() / 100.0).setScale(SCALE, RoundingMode.HALF_UP);
            }
        }

        return getDailyRateByRentability(inv.getRentabilityKind(), inv.getRentabilityValue());
    }

    private BigDecimal getDailyRateByRentability(RentabilityKind kind, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        switch (kind) {
            case PERCENT_AA:
                double annual = value.doubleValue() / 100.0;
                double daily = Math.pow(1 + annual, 1.0 / 365.0) - 1.0;
                return BigDecimal.valueOf(daily).setScale(SCALE, RoundingMode.HALF_UP);
            case PERCENT_CDI:
                double cdiFraction = (cdiAnnualRatePercent / 100.0) * (value.doubleValue() / 100.0);
                double dailyCdi = Math.pow(1 + cdiFraction, 1.0 / 365.0) - 1.0;
                return BigDecimal.valueOf(dailyCdi).setScale(SCALE, RoundingMode.HALF_UP);
            default:
                return BigDecimal.ZERO;
        }
    }
}
