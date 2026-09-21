package com.thiago.financeapi.api.controller;

import com.thiago.financeapi.application.dto.ReportDtos.CashFlowReport;
import com.thiago.financeapi.application.dto.ReportDtos.MonthlyReport;
import com.thiago.financeapi.application.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Relatorios", description = "Consolidacoes mensais e fluxo de caixa")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    @Operation(summary = "Resumo do mes com receitas, despesas, saldo, taxa de poupanca e quebra por categoria")
    public MonthlyReport monthly(
            @Parameter(description = "Mes de referencia, formato yyyy-MM", example = "2026-09")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return reportService.monthlyReport(month);
    }

    @GetMapping("/cash-flow")
    @Operation(summary = "Serie mensal de entradas, saidas e saldo no intervalo, maximo 24 meses")
    public CashFlowReport cashFlow(
            @Parameter(description = "Mes inicial, formato yyyy-MM", example = "2026-01")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth start,

            @Parameter(description = "Mes final, formato yyyy-MM", example = "2026-09")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth end) {
        return reportService.cashFlow(start, end);
    }
}
