package com.mindcash.app.service;

import com.mindcash.app.dto.InvestmentRequest;
import com.mindcash.app.dto.PositionLineDto;
import com.mindcash.app.model.*;
import com.mindcash.app.repository.AccountRepository;
import com.mindcash.app.repository.InvestmentRepository;
import com.mindcash.app.repository.TransactionRepository;
import com.mindcash.app.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    /** Patrimônio investido atual (soma dos currentValue). */
    public BigDecimal totalCurrentValueByUserId(Long userId) {
        return investmentRepository.sumCurrentValueByUserId(userId);
    }

    public List<Object[]> sumAmountByType(Long userId) {
        return investmentRepository.sumAmountByUserIdGroupByInvestmentType(userId);
    }

    public List<Object[]> sumCurrentValueByType(Long userId) {
        return investmentRepository.sumCurrentValueByUserIdGroupByInvestmentType(userId);
    }

    /** Taxa anual efetiva para projeção (CDI anual usado como 12% quando PERCENT_CDI). */
    public double getEffectiveAnnualRate(RentabilityKind kind, BigDecimal value, double cdiAnnualRatePercent) {
        if (value == null) return 0.0;
        double v = value.doubleValue() / 100.0;
        return switch (kind) {
            case PERCENT_AA -> v;
            case PERCENT_CDI -> (cdiAnnualRatePercent / 100.0) * v;
            default -> 0.0;
        };
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

        Account destinationAccount = accountRepository.findByIdAndUserId(request.getDestinationAccountId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta de destino não encontrada"));

        if (destinationAccount.getType() != AccountType.CORRETORA && destinationAccount.getType() != AccountType.INVESTIMENTO) {
            throw new IllegalArgumentException("A conta de destino deve ser do tipo Corretora ou Investimento");
        }

        boolean entradaInicial = request.getSourceAccountId() == null;
        Account sourceAccount = null;

        if (!entradaInicial) {
            sourceAccount = accountRepository.findByIdAndUserId(request.getSourceAccountId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta de origem não encontrada"));
            if (sourceAccount.getId().equals(destinationAccount.getId())) {
                throw new IllegalArgumentException("Conta de origem e destino devem ser diferentes");
            }
        }

        Investment investment = new Investment();
        investment.setUser(user);
        investment.setSourceAccount(sourceAccount);
        investment.setDestinationAccount(destinationAccount);
        investment.setAmount(request.getAmount());
        investment.setCurrentValue(request.getAmount());
        investment.setDate(request.getDate());
        investment.setInvestmentType(request.getInvestmentType());
        investment.setRentabilityValue(request.getRentabilityValue());
        investment.setRentabilityKind(request.getRentabilityKind());
        investment.setDescription(request.getDescription());
        investment.setAssetName(request.getAssetName() != null && !request.getAssetName().isBlank() ? request.getAssetName().trim() : null);

        investment = investmentRepository.save(investment);

        String desc = request.getDescription() != null && !request.getDescription().isBlank()
                ? "Aplicação: " + request.getDescription()
                : (entradaInicial ? "Entrada inicial em investimento" : "Aplicação em investimento");

        if (!entradaInicial) {
            Transaction debitTx = new Transaction();
            debitTx.setUser(user);
            debitTx.setAccount(sourceAccount);
            debitTx.setAmount(request.getAmount());
            debitTx.setType(TransactionType.DEBIT);
            debitTx.setDate(request.getDate());
            debitTx.setDescription(desc);
            debitTx.setInvestment(investment);
            applyBalanceChange(sourceAccount, TransactionType.DEBIT, request.getAmount());
            accountRepository.save(sourceAccount);
            transactionRepository.save(debitTx);
        }

        Transaction creditTx = new Transaction();
        creditTx.setUser(user);
        creditTx.setAccount(destinationAccount);
        creditTx.setAmount(request.getAmount());
        creditTx.setType(TransactionType.CREDIT);
        creditTx.setDate(request.getDate());
        creditTx.setDescription(desc);
        creditTx.setInvestment(investment);
        applyBalanceChange(destinationAccount, TransactionType.CREDIT, request.getAmount());
        accountRepository.save(destinationAccount);
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

    /**
     * Registra o saldo atual da única conta investimento como uma posição inicial (quando não há posições).
     * Zera o saldo da conta, cria um investimento "entrada inicial" e uma transação de crédito.
     */
    @Transactional
    public void registerExistingBalance(Long userId) {
        List<Account> investmentAccounts = accountRepository.findByUserIdAndDeletedAtIsNullOrderByNameAsc(userId).stream()
                .filter(a -> a.getType() == AccountType.INVESTIMENTO)
                .toList();
        if (investmentAccounts.size() != 1) {
            throw new IllegalArgumentException("Registrar saldo existente só é possível quando há exatamente uma conta do tipo Investimento.");
        }
        Account account = investmentAccounts.get(0);
        if (account.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("A conta de investimento não possui saldo para registrar.");
        }
        if (!investmentRepository.findByUserIdWithAccountsOrderByDateDesc(userId).isEmpty()) {
            throw new IllegalArgumentException("Já existem posições cadastradas. Use Entrada inicial no formulário para adicionar mais.");
        }

        BigDecimal balance = account.getBalance();
        User user = userRepository.getReferenceById(userId);

        account.setBalance(BigDecimal.ZERO);
        accountRepository.save(account);

        Investment investment = new Investment();
        investment.setUser(user);
        investment.setSourceAccount(null);
        investment.setDestinationAccount(account);
        investment.setAmount(balance);
        investment.setCurrentValue(balance);
        investment.setDate(LocalDate.now());
        investment.setInvestmentType(InvestmentType.OUTROS);
        investment.setRentabilityValue(BigDecimal.ZERO);
        investment.setRentabilityKind(RentabilityKind.OUTROS);
        investment.setDescription("Saldo existente registrado");

        investment = investmentRepository.save(investment);

        Transaction creditTx = new Transaction();
        creditTx.setUser(user);
        creditTx.setAccount(account);
        creditTx.setAmount(balance);
        creditTx.setType(TransactionType.CREDIT);
        creditTx.setDate(LocalDate.now());
        creditTx.setDescription("Saldo existente registrado");
        creditTx.setInvestment(investment);
        applyBalanceChange(account, TransactionType.CREDIT, balance);
        accountRepository.save(account);
        transactionRepository.save(creditTx);
    }

    /**
     * Registra o saldo existente da conta investimento como várias posições (composição).
     * A soma dos valores das posições deve ser igual ao saldo da conta.
     */
    @Transactional
    public void registerComposition(Long userId, Long destinationAccountId, List<PositionLineDto> positions) {
        if (positions == null || positions.isEmpty()) {
            throw new IllegalArgumentException("Informe pelo menos uma posição na composição.");
        }

        Account account = accountRepository.findByIdAndUserId(destinationAccountId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta de investimento não encontrada"));
        if (account.getType() != AccountType.INVESTIMENTO) {
            throw new IllegalArgumentException("A conta deve ser do tipo Investimento.");
        }
        if (!investmentRepository.findByUserIdWithAccountsOrderByDateDesc(userId).isEmpty()) {
            throw new IllegalArgumentException("Já existem posições cadastradas. Não é possível redefinir a composição.");
        }

        BigDecimal balance = account.getBalance();
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("A conta de investimento não possui saldo para compor.");
        }

        BigDecimal sum = positions.stream()
                .map(PositionLineDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(balance) != 0) {
            throw new IllegalArgumentException(
                    "A soma dos valores das posições (" + sum + ") deve ser igual ao saldo da conta (" + balance + ").");
        }

        User user = userRepository.getReferenceById(userId);
        account.setBalance(BigDecimal.ZERO);
        accountRepository.save(account);

        LocalDate today = LocalDate.now();
        for (PositionLineDto line : positions) {
            Investment investment = new Investment();
            investment.setUser(user);
            investment.setSourceAccount(null);
            investment.setDestinationAccount(account);
            investment.setAmount(line.getAmount());
            investment.setCurrentValue(line.getAmount());
            investment.setDate(today);
            investment.setInvestmentType(line.getInvestmentType());
            investment.setRentabilityValue(line.getRentabilityValue() != null ? line.getRentabilityValue() : BigDecimal.ZERO);
            investment.setRentabilityKind(line.getRentabilityKind() != null ? line.getRentabilityKind() : RentabilityKind.OUTROS);
            investment.setDescription("Composição inicial: " + line.getInvestmentType().getLabel());
            investment.setAssetName(line.getAssetName() != null && !line.getAssetName().isBlank() ? line.getAssetName().trim() : null);

            investment = investmentRepository.save(investment);

            String desc = "Composição inicial: " + line.getInvestmentType().getLabel();
            if (investment.getAssetName() != null) {
                desc += " (" + investment.getAssetName() + ")";
            }
            Transaction creditTx = new Transaction();
            creditTx.setUser(user);
            creditTx.setAccount(account);
            creditTx.setAmount(line.getAmount());
            creditTx.setType(TransactionType.CREDIT);
            creditTx.setDate(today);
            creditTx.setDescription(desc);
            creditTx.setInvestment(investment);
            applyBalanceChange(account, TransactionType.CREDIT, line.getAmount());
            accountRepository.save(account);
            transactionRepository.save(creditTx);
        }
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
