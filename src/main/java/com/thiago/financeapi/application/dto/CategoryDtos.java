package com.thiago.financeapi.application.dto;

import com.thiago.financeapi.domain.model.Category;
import com.thiago.financeapi.domain.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 80) String name,
            @NotNull TransactionType type,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar no formato #RRGGBB")
            String color) {
    }

    public record CategoryResponse(
            UUID id,
            String name,
            TransactionType type,
            String color) {

        public static CategoryResponse from(Category category) {
            return new CategoryResponse(
                    category.getId(),
                    category.getName(),
                    category.getType(),
                    category.getColor());
        }
    }
}
