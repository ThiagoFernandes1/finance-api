package com.thiago.financeapi.api.controller;

import com.thiago.financeapi.application.dto.CategoryDtos.CategoryRequest;
import com.thiago.financeapi.application.dto.CategoryDtos.CategoryResponse;
import com.thiago.financeapi.application.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categorias", description = "Classificacao de receitas e despesas")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Lista as categorias do usuario")
    public List<CategoryResponse> list() {
        return categoryService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma categoria pelo id")
    public CategoryResponse findById(@PathVariable UUID id) {
        return categoryService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cria uma categoria")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma categoria")
    public CategoryResponse update(
            @PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma categoria sem lancamentos vinculados")
    public void delete(@PathVariable UUID id) {
        categoryService.delete(id);
    }
}
