package com.thiago.financeapi.domain.repository;

import com.thiago.financeapi.domain.model.Transaction;
import com.thiago.financeapi.domain.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    Page<Transaction> findByUserId(UUID userId, Pageable pageable);

    /**
     * Soma os lancamentos de um tipo no intervalo. Retorna null quando nao ha
     * linhas, por isso o chamador precisa tratar a ausencia.
     */
    @Query("""
            SELECT SUM(t.amount) FROM Transaction t
            WHERE t.user.id = :userId
              AND t.type = :type
              AND t.occurredOn BETWEEN :start AND :end
            """)
    BigDecimal sumAmountByType(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT c.id AS categoryId, c.name AS categoryName, SUM(t.amount) AS total
            FROM Transaction t JOIN t.category c
            WHERE t.user.id = :userId
              AND t.type = :type
              AND t.occurredOn BETWEEN :start AND :end
            GROUP BY c.id, c.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategorySummary> summarizeByCategory(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.user.id = :userId
              AND t.category.id = :categoryId
              AND t.type = com.thiago.financeapi.domain.model.TransactionType.EXPENSE
              AND t.occurredOn BETWEEN :start AND :end
            """)
    BigDecimal sumExpensesByCategory(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /** Saldo corrente da conta = saldo inicial + entradas - saidas. */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN t.type = com.thiago.financeapi.domain.model.TransactionType.INCOME
                                     THEN t.amount ELSE -t.amount END), 0)
            FROM Transaction t
            WHERE t.account.id = :accountId
            """)
    BigDecimal sumSignedAmountByAccount(@Param("accountId") UUID accountId);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByAccountId(UUID accountId);
}
