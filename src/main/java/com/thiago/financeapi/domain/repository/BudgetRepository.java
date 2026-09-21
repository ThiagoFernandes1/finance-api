package com.thiago.financeapi.domain.repository;

import com.thiago.financeapi.domain.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdAndReferenceMonth(UUID userId, LocalDate referenceMonth);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    Optional<Budget> findByUserIdAndCategoryIdAndReferenceMonth(
            UUID userId, UUID categoryId, LocalDate referenceMonth);
}
