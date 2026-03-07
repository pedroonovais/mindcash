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
        return transactionRepository.findByUserIdOrderByDateDescCreatedAtDesc(userId, pageable);
    }

    public Transaction findByIdAndUserId(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada"));
    }

    /** Cria transação e atualiza saldo da conta atomicamente */
    @Transactional
    public Transaction create(TransactionRequest request, Long userId) {
        User user = userRepository.getReferenceById(userId);

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));

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

    @Transactional
    public void delete(Long id, Long userId) {
        Transaction transaction = findByIdAndUserId(id, userId);

        reverseBalanceChange(transaction.getAccount(), transaction.getType(), transaction.getAmount());
        accountRepository.save(transaction.getAccount());

        transactionRepository.delete(transaction);
    }

    public BigDecimal sumByPeriod(Long userId, TransactionType type, LocalDate start, LocalDate end) {
        return transactionRepository.sumByUserAndTypeAndPeriod(userId, type, start, end);
    }

    private void applyBalanceChange(Account account, TransactionType type, BigDecimal amount) {
        switch (type) {
            case CREDIT -> account.setBalance(account.getBalance().add(amount));
            case DEBIT -> account.setBalance(account.getBalance().subtract(amount));
            case TRANSFER -> { /* Saldo ajustado individualmente em transferências futuras */ }
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
