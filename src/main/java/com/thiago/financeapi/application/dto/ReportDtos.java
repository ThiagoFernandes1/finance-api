package com.thiago.financeapi.application.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record CategoryBreakdown(
            UUID categoryId,
            String categoryName,
            BigDecimal total,
            BigDecimal percentage) {
    }

    public record MonthlyReport(
            YearMonth referenceMonth,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal balance,
            BigDecimal savingsRate,
            List<CategoryBreakdown> expensesByCategory,
            List<CategoryBreakdown> incomeByCategory) {
    }

    public record MonthlyPoint(
            YearMonth referenceMonth,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal balance) {
    }

    public record CashFlowReport(
            YearMonth start,
            YearMonth end,
            List<MonthlyPoint> months) {
    }
}
