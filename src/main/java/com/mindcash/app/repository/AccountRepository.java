package com.mindcash.app.repository;

import com.mindcash.app.model.Account;
import com.mindcash.app.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserIdAndDeletedAtIsNullOrderByNameAsc(Long userId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    BigDecimal sumBalanceByUserId(Long userId);

    /** Saldo total excluindo contas dos tipos informados (ex.: investimento). */
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.user.id = :userId AND a.deletedAt IS NULL AND a.type NOT IN :excludeTypes")
    BigDecimal sumBalanceByUserIdExcludingTypes(Long userId, List<AccountType> excludeTypes);

    boolean existsByUserIdAndTypeAndDeletedAtIsNull(Long userId, AccountType type);
}
