package com.mindcash.app.service;

import com.mindcash.app.dto.AccountRequest;
import com.mindcash.app.model.Account;
import com.mindcash.app.model.User;
import com.mindcash.app.repository.AccountRepository;
import com.mindcash.app.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public List<Account> findByUserId(Long userId) {
        return accountRepository.findByUserIdAndDeletedAtIsNullOrderByNameAsc(userId);
    }

    public Account findByIdAndUserId(Long id, Long userId) {
        return accountRepository.findByIdAndUserId(id, userId)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));
    }

    @Transactional
    public Account create(AccountRequest request, Long userId) {
        User user = userRepository.getReferenceById(userId);

        Account account = new Account();
        account.setUser(user);
        account.setName(request.getName().trim());
        account.setType(request.getType());
        account.setCurrency(request.getCurrency());
        account.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);

        return accountRepository.save(account);
    }

    @Transactional
    public Account update(Long id, AccountRequest request, Long userId) {
        Account account = findByIdAndUserId(id, userId);
        account.setName(request.getName().trim());
        account.setType(request.getType());
        account.setCurrency(request.getCurrency());
        return accountRepository.save(account);
    }

    @Transactional
    public void softDelete(Long id, Long userId) {
        Account account = findByIdAndUserId(id, userId);
        account.setDeletedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    public BigDecimal getTotalBalance(Long userId) {
        return accountRepository.sumBalanceByUserId(userId);
    }
}
