package com.thiago.financeapi.api.controller;

import com.thiago.financeapi.application.dto.TransactionDtos.TransactionFilter;
import com.thiago.financeapi.application.dto.TransactionDtos.TransactionRequest;
import com.thiago.financeapi.application.dto.TransactionDtos.TransactionResponse;
import com.thiago.financeapi.application.service.TransactionService;
import com.thiago.financeapi.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Lancamentos", description = "Extrato de receitas e despesas")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "Lista lancamentos com filtros opcionais e paginacao")
    public Page<TransactionResponse> search(
            @Parameter(description = "Data inicial inclusive, formato yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "Data final inclusive, formato yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) TransactionType type,

            @Parameter(description = "Busca parcial na descricao")
            @RequestParam(required = false) String description,

            @PageableDefault(size = 20, sort = "occurredOn", direction = Sort.Direction.DESC)
            Pageable pageable) {

        TransactionFilter filter = new TransactionFilter(
                startDate, endDate, accountId, categoryId, type, description);

        return transactionService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um lancamento pelo id")
    public TransactionResponse findById(@PathVariable UUID id) {
        return transactionService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Registra um lancamento")
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um lancamento")
    public TransactionResponse update(
            @PathVariable UUID id, @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um lancamento")
    public void delete(@PathVariable UUID id) {
        transactionService.delete(id);
    }
}
