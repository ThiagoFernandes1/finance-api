package com.thiago.financeapi.application.service;

import com.thiago.financeapi.api.exception.BusinessException;
import com.thiago.financeapi.api.exception.ResourceNotFoundException;
import com.thiago.financeapi.application.dto.BudgetDtos.BudgetRequest;
import com.thiago.financeapi.application.dto.BudgetDtos.BudgetResponse;
import com.thiago.financeapi.application.dto.BudgetDtos.BudgetStatus;
import com.thiago.financeapi.domain.model.Budget;
import com.thiago.financeapi.domain.model.Category;
import com.thiago.financeapi.domain.model.TransactionType;
import com.thiago.financeapi.domain.repository.BudgetRepository;
import com.thiago.financeapi.domain.repository.CategoryRepository;
import com.thiago.financeapi.domain.repository.TransactionRepository;
import com.thiago.financeapi.domain.repository.UserRepository;
import com.thiago.financeapi.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("80");
    private static final BigDecimal EXCEEDED_THRESHOLD = new BigDecimal("100");

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BudgetResponse> listByMonth(YearMonth referenceMonth) {
        return budgetRepository
                .findByUserIdAndReferenceMonth(CurrentUser.requireId(), referenceMonth.atDay(1))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse findById(UUID id) {
        return toResponse(requireOwned(id));
    }

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        UUID userId = CurrentUser.requireId();
        Category category = requireExpenseCategory(request.categoryId(), userId);
        LocalDate reference = request.referenceMonth().atDay(1);

        budgetRepository
                .findByUserIdAndCategoryIdAndReferenceMonth(userId, category.getId(), reference)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Ja existe orcamento para esta categoria no mes informado");
                });

        Budget budget = Budget.builder()
                .category(category)
                .limitAmount(request.limitAmount())
                .referenceMonth(reference)
                .user(userRepository.getReferenceById(userId))
                .build();

        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse update(UUID id, BudgetRequest request) {
        UUID userId = CurrentUser.requireId();
        Budget budget = requireOwned(id);
        Category category = requireExpenseCategory(request.categoryId(), userId);
        LocalDate reference = request.referenceMonth().atDay(1);

        budgetRepository
                .findByUserIdAndCategoryIdAndReferenceMonth(userId, category.getId(), reference)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Ja existe orcamento para esta categoria no mes informado");
                });

        budget.setCategory(category);
        budget.setLimitAmount(request.limitAmount());
        budget.setReferenceMonth(reference);

        return toResponse(budget);
    }

    @Transactional
    public void delete(UUID id) {
        budgetRepository.delete(requireOwned(id));
    }

    private BudgetResponse toResponse(Budget budget) {
        YearMonth month = budget.referenceYearMonth();
        BigDecimal spent = transactionRepository.sumExpensesByCategory(
                budget.getUser().getId(),
                budget.getCategory().getId(),
                month.atDay(1),
                month.atEndOfMonth());

        BigDecimal limit = budget.getLimitAmount();
        BigDecimal usage = limit.signum() == 0
                ? BigDecimal.ZERO
                : spent.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP);

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                month,
                limit,
                spent,
                limit.subtract(spent),
                usage,
                statusOf(usage));
    }

    private BudgetStatus statusOf(BigDecimal usagePercentage) {
        if (usagePercentage.compareTo(EXCEEDED_THRESHOLD) > 0) {
            return BudgetStatus.EXCEEDED;
        }
        if (usagePercentage.compareTo(WARNING_THRESHOLD) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.ON_TRACK;
    }

    private Category requireExpenseCategory(UUID categoryId, UUID userId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria"));

        if (category.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("Orcamento so pode ser definido para categorias de despesa");
        }
        return category;
    }

    private Budget requireOwned(UUID id) {
        return budgetRepository.findByIdAndUserId(id, CurrentUser.requireId())
                .orElseThrow(() -> new ResourceNotFoundException("Orcamento"));
    }
}
