package com.thiago.financeapi.service;

import com.thiago.financeapi.application.dto.BudgetDtos.BudgetResponse;
import com.thiago.financeapi.application.dto.BudgetDtos.BudgetStatus;
import com.thiago.financeapi.application.service.BudgetService;
import com.thiago.financeapi.domain.model.Budget;
import com.thiago.financeapi.domain.model.Category;
import com.thiago.financeapi.domain.model.TransactionType;
import com.thiago.financeapi.domain.model.User;
import com.thiago.financeapi.domain.repository.BudgetRepository;
import com.thiago.financeapi.domain.repository.CategoryRepository;
import com.thiago.financeapi.domain.repository.TransactionRepository;
import com.thiago.financeapi.domain.repository.UserRepository;
import com.thiago.financeapi.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Cobre a classificacao de consumo do orcamento, que e a regra de negocio mais
 * sensivel a erro de arredondamento e de limite de faixa.
 */
class BudgetStatusTest {

    private BudgetRepository budgetRepository;
    private TransactionRepository transactionRepository;
    private BudgetService budgetService;
    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        budgetRepository = Mockito.mock(BudgetRepository.class);
        transactionRepository = Mockito.mock(TransactionRepository.class);
        budgetService = new BudgetService(
                budgetRepository,
                Mockito.mock(CategoryRepository.class),
                transactionRepository,
                Mockito.mock(UserRepository.class));

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Thiago")
                .email("t@example.com")
                .passwordHash("hash")
                .build();

        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Alimentacao")
                .type(TransactionType.EXPENSE)
                .user(user)
                .build();

        authenticate(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "gasto {0} de limite {1} deve ser {2}")
    @CsvSource({
            "0,    1000, ON_TRACK",
            "500,  1000, ON_TRACK",
            "799,  1000, ON_TRACK",
            "800,  1000, WARNING",
            "1000, 1000, WARNING",
            "1001, 1000, EXCEEDED",
            "2500, 1000, EXCEEDED"
    })
    @DisplayName("classifica o status a partir do percentual consumido")
    void shouldClassifyStatusByUsage(String spent, String limit, BudgetStatus expected) {
        BudgetResponse response = responseFor(new BigDecimal(spent), new BigDecimal(limit));
        assertThat(response.status()).isEqualTo(expected);
    }

    @Test
    @DisplayName("calcula saldo restante e percentual de uso")
    void shouldComputeRemainingAndUsage() {
        BudgetResponse response = responseFor(new BigDecimal("250"), new BigDecimal("1000"));

        assertThat(response.spentAmount()).isEqualByComparingTo("250");
        assertThat(response.remainingAmount()).isEqualByComparingTo("750");
        assertThat(response.usagePercentage()).isEqualByComparingTo("25.00");
        assertThat(response.referenceMonth()).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    @DisplayName("saldo restante fica negativo quando o limite e estourado")
    void shouldReturnNegativeRemainingWhenExceeded() {
        BudgetResponse response = responseFor(new BigDecimal("1200"), new BigDecimal("1000"));

        assertThat(response.remainingAmount()).isEqualByComparingTo("-200");
        assertThat(response.status()).isEqualTo(BudgetStatus.EXCEEDED);
    }

    private BudgetResponse responseFor(BigDecimal spent, BigDecimal limit) {
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder()
                .id(budgetId)
                .category(category)
                .user(user)
                .limitAmount(limit)
                .referenceMonth(LocalDate.of(2026, 9, 1))
                .build();

        when(budgetRepository.findByIdAndUserId(budgetId, user.getId()))
                .thenReturn(Optional.of(budget));
        when(transactionRepository.sumExpensesByCategory(
                eq(user.getId()), eq(category.getId()), any(), any()))
                .thenReturn(spent);

        return budgetService.findById(budgetId);
    }

    private void authenticate(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
