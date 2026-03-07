package com.mindcash.app.repository;

import com.mindcash.app.model.Transaction;
import com.mindcash.app.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserIdOrderByDateDescCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :start AND :end")
    BigDecimal sumByUserAndTypeAndPeriod(Long userId, TransactionType type, LocalDate start, LocalDate end);

    /** Top categorias por valor no período */
    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :start AND :end " +
           "AND t.category IS NOT NULL GROUP BY t.category.name ORDER BY SUM(t.amount) DESC")
    List<Object[]> findTopCategoriesByPeriod(Long userId, TransactionType type, LocalDate start, LocalDate end);

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate start, LocalDate end);
}
