package com.thiago.financeapi.application.service;

import com.thiago.financeapi.api.exception.BusinessException;
import com.thiago.financeapi.application.dto.ReportDtos.CashFlowReport;
import com.thiago.financeapi.application.dto.ReportDtos.CategoryBreakdown;
import com.thiago.financeapi.application.dto.ReportDtos.MonthlyPoint;
import com.thiago.financeapi.application.dto.ReportDtos.MonthlyReport;
import com.thiago.financeapi.domain.model.TransactionType;
import com.thiago.financeapi.domain.repository.TransactionRepository;
import com.thiago.financeapi.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    /** Teto de meses por consulta de fluxo de caixa, para limitar o custo da query. */
    private static final int MAX_CASH_FLOW_MONTHS = 24;

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public MonthlyReport monthlyReport(YearMonth referenceMonth) {
        UUID userId = CurrentUser.requireId();
        LocalDate start = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.atEndOfMonth();

        BigDecimal income = sum(userId, TransactionType.INCOME, start, end);
        BigDecimal expense = sum(userId, TransactionType.EXPENSE, start, end);
        BigDecimal balance = income.subtract(expense);

        return new MonthlyReport(
                referenceMonth,
                income,
                expense,
                balance,
                savingsRate(income, balance),
                breakdown(userId, TransactionType.EXPENSE, start, end, expense),
                breakdown(userId, TransactionType.INCOME, start, end, income));
    }

    @Transactional(readOnly = true)
    public CashFlowReport cashFlow(YearMonth start, YearMonth end) {
        if (start.isAfter(end)) {
            throw new BusinessException("Mes inicial deve ser anterior ou igual ao mes final");
        }

        long months = java.time.temporal.ChronoUnit.MONTHS.between(start, end) + 1;
        if (months > MAX_CASH_FLOW_MONTHS) {
            throw new BusinessException(
                    "Intervalo maximo permitido e de %d meses".formatted(MAX_CASH_FLOW_MONTHS));
        }

        UUID userId = CurrentUser.requireId();
        List<MonthlyPoint> points = new ArrayList<>();

        for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();

            BigDecimal income = sum(userId, TransactionType.INCOME, monthStart, monthEnd);
            BigDecimal expense = sum(userId, TransactionType.EXPENSE, monthStart, monthEnd);

            points.add(new MonthlyPoint(month, income, expense, income.subtract(expense)));
        }

        return new CashFlowReport(start, end, points);
    }

    private List<CategoryBreakdown> breakdown(
            UUID userId, TransactionType type, LocalDate start, LocalDate end, BigDecimal total) {

        return transactionRepository.summarizeByCategory(userId, type, start, end).stream()
                .map(row -> new CategoryBreakdown(
                        row.getCategoryId(),
                        row.getCategoryName(),
                        row.getTotal(),
                        percentageOf(row.getTotal(), total)))
                .toList();
    }

    /** Proporcao do total poupado sobre a receita; zero quando nao houve receita. */
    private BigDecimal savingsRate(BigDecimal income, BigDecimal balance) {
        if (income.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return balance.multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentageOf(BigDecimal value, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(UUID userId, TransactionType type, LocalDate start, LocalDate end) {
        BigDecimal result = transactionRepository.sumAmountByType(userId, type, start, end);
        return result != null ? result : BigDecimal.ZERO;
    }
}
