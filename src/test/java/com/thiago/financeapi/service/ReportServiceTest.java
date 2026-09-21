package com.thiago.financeapi.service;

import com.thiago.financeapi.api.exception.BusinessException;
import com.thiago.financeapi.application.dto.ReportDtos.CashFlowReport;
import com.thiago.financeapi.application.dto.ReportDtos.MonthlyReport;
import com.thiago.financeapi.application.service.ReportService;
import com.thiago.financeapi.domain.model.TransactionType;
import com.thiago.financeapi.domain.model.User;
import com.thiago.financeapi.domain.repository.TransactionRepository;
import com.thiago.financeapi.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private TransactionRepository transactionRepository;
    private ReportService reportService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        reportService = new ReportService(transactionRepository);

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Thiago")
                .email("t@example.com")
                .passwordHash("hash")
                .build();
        userId = user.getId();

        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(transactionRepository.summarizeByCategory(any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("consolida saldo e taxa de poupanca do mes")
    void shouldComputeMonthlyBalanceAndSavingsRate() {
        stubTotals(new BigDecimal("5000"), new BigDecimal("3500"));

        MonthlyReport report = reportService.monthlyReport(YearMonth.of(2026, 9));

        assertThat(report.totalIncome()).isEqualByComparingTo("5000");
        assertThat(report.totalExpense()).isEqualByComparingTo("3500");
        assertThat(report.balance()).isEqualByComparingTo("1500");
        // 1500 / 5000 = 30%
        assertThat(report.savingsRate()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("trata mes sem lancamentos sem divisao por zero")
    void shouldHandleEmptyMonth() {
        stubTotals(null, null);

        MonthlyReport report = reportService.monthlyReport(YearMonth.of(2026, 9));

        assertThat(report.totalIncome()).isEqualByComparingTo("0");
        assertThat(report.totalExpense()).isEqualByComparingTo("0");
        assertThat(report.balance()).isEqualByComparingTo("0");
        assertThat(report.savingsRate()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("taxa de poupanca fica negativa quando o mes fecha no vermelho")
    void shouldReturnNegativeSavingsRateWhenOverspending() {
        stubTotals(new BigDecimal("2000"), new BigDecimal("2500"));

        MonthlyReport report = reportService.monthlyReport(YearMonth.of(2026, 9));

        assertThat(report.balance()).isEqualByComparingTo("-500");
        assertThat(report.savingsRate()).isEqualByComparingTo("-25.00");
    }

    @Test
    @DisplayName("gera um ponto por mes no fluxo de caixa, inclusive os extremos")
    void shouldBuildOnePointPerMonth() {
        stubTotals(new BigDecimal("1000"), new BigDecimal("400"));

        CashFlowReport report = reportService.cashFlow(YearMonth.of(2026, 1), YearMonth.of(2026, 6));

        assertThat(report.months()).hasSize(6);
        assertThat(report.months().get(0).referenceMonth()).isEqualTo(YearMonth.of(2026, 1));
        assertThat(report.months().get(5).referenceMonth()).isEqualTo(YearMonth.of(2026, 6));
        assertThat(report.months().get(0).balance()).isEqualByComparingTo("600");
    }

    @Test
    @DisplayName("rejeita intervalo invertido")
    void shouldRejectInvertedRange() {
        assertThatThrownBy(() ->
                reportService.cashFlow(YearMonth.of(2026, 6), YearMonth.of(2026, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("anterior ou igual");
    }

    @Test
    @DisplayName("rejeita intervalo maior que o teto de 24 meses")
    void shouldRejectRangeAboveLimit() {
        assertThatThrownBy(() ->
                reportService.cashFlow(YearMonth.of(2024, 1), YearMonth.of(2026, 6)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("24 meses");
    }

    private void stubTotals(BigDecimal income, BigDecimal expense) {
        when(transactionRepository.sumAmountByType(
                eq(userId), eq(TransactionType.INCOME), any(), any())).thenReturn(income);
        when(transactionRepository.sumAmountByType(
                eq(userId), eq(TransactionType.EXPENSE), any(), any())).thenReturn(expense);
    }
}
