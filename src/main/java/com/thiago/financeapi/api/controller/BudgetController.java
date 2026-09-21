package com.thiago.financeapi.api.controller;

import com.thiago.financeapi.application.dto.BudgetDtos.BudgetRequest;
import com.thiago.financeapi.application.dto.BudgetDtos.BudgetResponse;
import com.thiago.financeapi.application.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Orcamentos", description = "Limites mensais de gasto por categoria")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @Operation(summary = "Lista os orcamentos do mes com o consumo ja calculado")
    public List<BudgetResponse> listByMonth(
            @Parameter(description = "Mes de referencia, formato yyyy-MM", example = "2026-09")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return budgetService.listByMonth(month);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um orcamento pelo id")
    public BudgetResponse findById(@PathVariable UUID id) {
        return budgetService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Define um orcamento para uma categoria de despesa")
    public ResponseEntity<BudgetResponse> create(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um orcamento")
    public BudgetResponse update(
            @PathVariable UUID id, @Valid @RequestBody BudgetRequest request) {
        return budgetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um orcamento")
    public void delete(@PathVariable UUID id) {
        budgetService.delete(id);
    }
}
