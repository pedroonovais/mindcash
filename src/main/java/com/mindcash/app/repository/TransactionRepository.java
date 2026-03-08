package com.mindcash.app.repository;

import com.mindcash.app.model.Transaction;
import com.mindcash.app.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category", "account"})
    Page<Transaction> findByUserIdOrderByDateDescCreatedAtDesc(Long userId, Pageable pageable);

    /** Lista apenas transações reais (avulsas ou geradas por recorrência), excluindo modelos. */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category", "account"})
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND (t.recurrenceParent IS NOT NULL OR t.recurrenceType IS NULL) " +
           "ORDER BY t.date DESC, t.createdAt DESC")
    Page<Transaction> findByUserIdExcludingRecurrenceTemplatesOrderByDateDesc(Long userId, Pageable pageable);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :start AND :end " +
           "AND (t.recurrenceParent IS NOT NULL OR t.recurrenceType IS NULL)")
    BigDecimal sumByUserAndTypeAndPeriod(Long userId, TransactionType type, LocalDate start, LocalDate end);

    /** Soma para dashboard: exclui transações vinculadas a investimentos (receitas/despesas do mês = fluxo do dia a dia). */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :start AND :end " +
           "AND (t.recurrenceParent IS NOT NULL OR t.recurrenceType IS NULL) AND t.investment IS NULL")
    BigDecimal sumByUserAndTypeAndPeriodExcludingInvestments(Long userId, TransactionType type, LocalDate start, LocalDate end);

    /** Top categorias por valor no período (apenas transações reais). */
    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :start AND :end " +
           "AND t.category IS NOT NULL " +
           "AND (t.recurrenceParent IS NOT NULL OR t.recurrenceType IS NULL) " +
           "GROUP BY t.category.name ORDER BY SUM(t.amount) DESC")
    List<Object[]> findTopCategoriesByPeriod(Long userId, TransactionType type, LocalDate start, LocalDate end);

    /** Top categorias para dashboard: exclui transações vinculadas a investimentos. */
    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :start AND :end " +
           "AND t.category IS NOT NULL " +
           "AND (t.recurrenceParent IS NOT NULL OR t.recurrenceType IS NULL) AND t.investment IS NULL " +
           "GROUP BY t.category.name ORDER BY SUM(t.amount) DESC")
    List<Object[]> findTopCategoriesByPeriodExcludingInvestments(Long userId, TransactionType type, LocalDate start, LocalDate end);

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate start, LocalDate end);

    /** Modelos de recorrência elegíveis para materialização no mês (recurrence_next_ym <= firstDayOfMonth). */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.recurrenceType IS NOT NULL AND t.recurrenceParent IS NULL AND t.recurrenceNextYm IS NOT NULL " +
           "AND t.recurrenceNextYm <= :firstDayOfMonth ORDER BY t.recurrenceNextYm ASC")
    List<Transaction> findRecurrenceTemplatesEligibleForMonth(Long userId, LocalDate firstDayOfMonth);

    long countByRecurrenceParentId(Long recurrenceParentId);

    boolean existsByRecurrenceParentIdAndDateBetween(Long recurrenceParentId, LocalDate start, LocalDate end);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category", "account"})
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.recurrenceType IS NOT NULL AND t.recurrenceParent IS NULL " +
           "ORDER BY t.recurrenceNextYm ASC NULLS LAST, t.createdAt DESC")
    Page<Transaction> findRecurrenceTemplatesByUserId(Long userId, Pageable pageable);

    List<Transaction> findByInvestmentId(Long investmentId);

    /** Última data em que existe transação de rendimento para o investimento (para backfill de dias faltantes). */
    @Query("SELECT MAX(t.date) FROM Transaction t WHERE t.investment.id = :investmentId AND t.type = :type")
    Optional<LocalDate> findMaxDateByInvestmentIdAndType(@Param("investmentId") Long investmentId, @Param("type") TransactionType type);
}
