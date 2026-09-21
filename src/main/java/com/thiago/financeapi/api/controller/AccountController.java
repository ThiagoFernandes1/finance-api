package com.thiago.financeapi.api.controller;

import com.thiago.financeapi.application.dto.AccountDtos.AccountRequest;
import com.thiago.financeapi.application.dto.AccountDtos.AccountResponse;
import com.thiago.financeapi.application.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Contas", description = "Contas bancarias, carteiras e cartoes")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "Lista as contas com o saldo corrente calculado")
    public List<AccountResponse> list() {
        return accountService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma conta pelo id")
    public AccountResponse findById(@PathVariable UUID id) {
        return accountService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cria uma conta")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma conta")
    public AccountResponse update(
            @PathVariable UUID id, @Valid @RequestBody AccountRequest request) {
        return accountService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma conta sem lancamentos vinculados")
    public void delete(@PathVariable UUID id) {
        accountService.delete(id);
    }
}
