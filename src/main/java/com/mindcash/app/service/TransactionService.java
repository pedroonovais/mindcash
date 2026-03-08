package com.mindcash.app.service;

import com.mindcash.app.dto.TransactionRequest;
import com.mindcash.app.model.*;
import com.mindcash.app.repository.AccountRepository;
import com.mindcash.app.repository.CategoryRepository;
import com.mindcash.app.repository.TransactionRepository;
import com.mindcash.app.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public Page<Transaction> findByUserId(Long userId, Pageable pageable) {
        return transactionRepository.findByUserIdExcludingRecurrenceTemplatesOrderByDateDesc(userId, pageable);
    }

    public Transaction findByIdAndUserId(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada"));
    }

    /** Cria transação e atualiza saldo da conta atomicamente; ou salva modelo de recorrência sem alterar saldo. */
    @Transactional
    public Transaction create(TransactionRequest request, Long userId) {
        User user = userRepository.getReferenceById(userId);

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));

        if (request.getRecurrenceType() != null) {
            return createRecurrenceTemplate(request, user, account, userId);
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));
            transaction.setCategory(category);
        }

        applyBalanceChange(account, request.getType(), request.getAmount());
        accountRepository.save(account);

        return transactionRepository.save(transaction);
    }

    private Transaction createRecurrenceTemplate(TransactionRequest request, User user, Account account, Long userId) {
        Transaction template = new Transaction();
        template.setUser(user);
        template.setAccount(account);
        template.setAmount(request.getAmount());
        template.setType(request.getType());
        template.setDate(request.getDate());
        template.setDescription(request.getDescription());
        template.setRecurrenceType(request.getRecurrenceType());
        template.setRecurrenceCount(request.getRecurrenceCount());
        template.setRecurrenceNextYm(request.getDate().withDayOfMonth(1));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));
            template.setCategory(category);
        }

        return transactionRepository.save(template);
    }

    /**
     * Cria uma transação real a partir de um modelo de recorrência (materialização).
     * Atualiza o saldo da conta. A transação gerada não tem campos de recorrência.
     */
    @Transactional
    public Transaction createFromRecurrenceTemplate(Transaction template, LocalDate targetDate) {
        Transaction child = new Transaction();
        child.setUser(template.getUser());
        child.setAccount(template.getAccount());
        child.setCategory(template.getCategory());
        child.setAmount(template.getAmount());
        child.setType(template.getType());
        child.setDescription(template.getDescription());
        child.setDate(targetDate);
        child.setRecurrenceParent(template);

        applyBalanceChange(template.getAccount(), template.getType(), template.getAmount());
        accountRepository.save(template.getAccount());

        return transactionRepository.save(child);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Transaction transaction = findByIdAndUserId(id, userId);

        if (transaction.isRecurrenceTemplate()) {
            transactionRepository.delete(transaction);
            return;
        }

        reverseBalanceChange(transaction.getAccount(), transaction.getType(), transaction.getAmount());
        accountRepository.save(transaction.getAccount());

        transactionRepository.delete(transaction);
    }

    public BigDecimal sumByPeriod(Long userId, TransactionType type, LocalDate start, LocalDate end) {
        return transactionRepository.sumByUserAndTypeAndPeriod(userId, type, start, end);
    }

    public Page<Transaction> findRecurrenceTemplatesByUserId(Long userId, Pageable pageable) {
        return transactionRepository.findRecurrenceTemplatesByUserId(userId, pageable);
    }

    @Transactional
    public void cancelRecurrence(Long id, Long userId) {
        Transaction template = findByIdAndUserId(id, userId);
        if (!template.isRecurrenceTemplate()) {
            throw new IllegalArgumentException("Apenas modelos de recorrência podem ser cancelados");
        }
        template.setRecurrenceNextYm(null);
        transactionRepository.save(template);
    }

    private void applyBalanceChange(Account account, TransactionType type, BigDecimal amount) {
        switch (type) {
            case CREDIT, RENDIMENTO -> account.setBalance(account.getBalance().add(amount));
            case DEBIT -> account.setBalance(account.getBalance().subtract(amount));
            case TRANSFER -> { /* Saldo ajustado individualmente em transferências futuras */ }
        }
    }

    private void reverseBalanceChange(Account account, TransactionType type, BigDecimal amount) {
        switch (type) {
            case CREDIT, RENDIMENTO -> account.setBalance(account.getBalance().subtract(amount));
            case DEBIT -> account.setBalance(account.getBalance().add(amount));
            case TRANSFER -> { }
        }
    }
}
