package com.mindcash.app.service;

import com.mindcash.app.dto.InvestmentRequest;
import com.mindcash.app.model.*;
import com.mindcash.app.repository.AccountRepository;
import com.mindcash.app.repository.InvestmentRepository;
import com.mindcash.app.repository.TransactionRepository;
import com.mindcash.app.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public InvestmentService(
            InvestmentRepository investmentRepository,
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            UserRepository userRepository) {
        this.investmentRepository = investmentRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public List<Investment> findByUserId(Long userId) {
        return investmentRepository.findByUserIdWithAccountsOrderByDateDesc(userId);
    }

    public Investment findByIdAndUserId(Long id, Long userId) {
        return investmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Investimento não encontrado"));
    }

    public BigDecimal totalInvestedByUserId(Long userId) {
        return investmentRepository.sumAmountByUserId(userId);
    }

    public List<Object[]> sumAmountByType(Long userId) {
        return investmentRepository.sumAmountByUserIdGroupByInvestmentType(userId);
    }

    /**
     * Retorna dados para gráfico de evolução: lista de [label (ex. "Jan"), valor acumulado].
     * Meses sem investimento têm valor acumulado do mês anterior.
     */
    public List<Map.Entry<String, BigDecimal>> getEvolutionData(Long userId) {
        List<Investment> investments = investmentRepository.findByUserIdOrderByDateDesc(userId);
        if (investments.isEmpty()) {
            return List.of();
        }
        TreeMap<YearMonth, BigDecimal> byMonth = new TreeMap<>();
        for (Investment i : investments) {
            YearMonth ym = YearMonth.from(i.getDate());
            byMonth.merge(ym, i.getAmount(), BigDecimal::add);
        }
        String[] monthLabels = {"Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};
        BigDecimal cumulative = BigDecimal.ZERO;
        List<Map.Entry<String, BigDecimal>> result = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal> e : byMonth.entrySet()) {
            cumulative = cumulative.add(e.getValue());
            String label = monthLabels[e.getKey().getMonthValue() - 1] + " " + e.getKey().getYear();
            result.add(Map.entry(label, cumulative));
        }
        return result;
    }

    @Transactional
    public Investment create(InvestmentRequest request, Long userId) {
        User user = userRepository.getReferenceById(userId);

        Account sourceAccount = accountRepository.findByIdAndUserId(request.getSourceAccountId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta de origem não encontrada"));

        Account destinationAccount = accountRepository.findByIdAndUserId(request.getDestinationAccountId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta de destino não encontrada"));

        if (sourceAccount.getId().equals(destinationAccount.getId())) {
            throw new IllegalArgumentException("Conta de origem e destino devem ser diferentes");
        }

        if (destinationAccount.getType() != AccountType.CORRETORA && destinationAccount.getType() != AccountType.INVESTIMENTO) {
            throw new IllegalArgumentException("A conta de destino deve ser do tipo Corretora ou Investimento");
        }

        Investment investment = new Investment();
        investment.setUser(user);
        investment.setSourceAccount(sourceAccount);
        investment.setDestinationAccount(destinationAccount);
        investment.setAmount(request.getAmount());
        investment.setDate(request.getDate());
        investment.setInvestmentType(request.getInvestmentType());
        investment.setRentabilityValue(request.getRentabilityValue());
        investment.setRentabilityKind(request.getRentabilityKind());
        investment.setDescription(request.getDescription());

        investment = investmentRepository.save(investment);

        String descDebit = request.getDescription() != null && !request.getDescription().isBlank()
                ? "Aplicação: " + request.getDescription()
                : "Aplicação em investimento";

        Transaction debitTx = new Transaction();
        debitTx.setUser(user);
        debitTx.setAccount(sourceAccount);
        debitTx.setAmount(request.getAmount());
        debitTx.setType(TransactionType.DEBIT);
        debitTx.setDate(request.getDate());
        debitTx.setDescription(descDebit);
        debitTx.setInvestment(investment);

        Transaction creditTx = new Transaction();
        creditTx.setUser(user);
        creditTx.setAccount(destinationAccount);
        creditTx.setAmount(request.getAmount());
        creditTx.setType(TransactionType.CREDIT);
        creditTx.setDate(request.getDate());
        creditTx.setDescription(descDebit);
        creditTx.setInvestment(investment);

        applyBalanceChange(sourceAccount, TransactionType.DEBIT, request.getAmount());
        applyBalanceChange(destinationAccount, TransactionType.CREDIT, request.getAmount());

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        transactionRepository.save(debitTx);
        transactionRepository.save(creditTx);

        return investment;
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Investment investment = findByIdAndUserId(id, userId);
        List<Transaction> transactions = transactionRepository.findByInvestmentId(investment.getId());

        for (Transaction tx : transactions) {
            reverseBalanceChange(tx.getAccount(), tx.getType(), tx.getAmount());
            accountRepository.save(tx.getAccount());
            transactionRepository.delete(tx);
        }

        investmentRepository.delete(investment);
    }

    private void applyBalanceChange(Account account, TransactionType type, BigDecimal amount) {
        switch (type) {
            case CREDIT -> account.setBalance(account.getBalance().add(amount));
            case DEBIT -> account.setBalance(account.getBalance().subtract(amount));
            case TRANSFER -> { }
        }
    }

    private void reverseBalanceChange(Account account, TransactionType type, BigDecimal amount) {
        switch (type) {
            case CREDIT -> account.setBalance(account.getBalance().subtract(amount));
            case DEBIT -> account.setBalance(account.getBalance().add(amount));
            case TRANSFER -> { }
        }
    }
}
