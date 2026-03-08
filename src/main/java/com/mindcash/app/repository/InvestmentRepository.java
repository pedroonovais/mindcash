package com.mindcash.app.repository;

import com.mindcash.app.model.Investment;
import com.mindcash.app.model.InvestmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sourceAccount", "destinationAccount"})
    List<Investment> findByUserIdOrderByDateDesc(Long userId);

    @Query("SELECT i FROM Investment i JOIN FETCH i.sourceAccount JOIN FETCH i.destinationAccount WHERE i.user.id = :userId ORDER BY i.date DESC")
    List<Investment> findByUserIdWithAccountsOrderByDateDesc(Long userId);

    Optional<Investment> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Investment i WHERE i.user.id = :userId")
    BigDecimal sumAmountByUserId(Long userId);

    @Query("SELECT i.investmentType, SUM(i.amount) FROM Investment i WHERE i.user.id = :userId GROUP BY i.investmentType")
    List<Object[]> sumAmountByUserIdGroupByInvestmentType(Long userId);
}
