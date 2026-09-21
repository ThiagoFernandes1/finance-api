package com.thiago.financeapi.application.service;

import com.thiago.financeapi.api.exception.BusinessException;
import com.thiago.financeapi.api.exception.ResourceNotFoundException;
import com.thiago.financeapi.application.dto.CategoryDtos.CategoryRequest;
import com.thiago.financeapi.application.dto.CategoryDtos.CategoryResponse;
import com.thiago.financeapi.domain.model.Category;
import com.thiago.financeapi.domain.repository.CategoryRepository;
import com.thiago.financeapi.domain.repository.TransactionRepository;
import com.thiago.financeapi.domain.repository.UserRepository;
import com.thiago.financeapi.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findByUserIdOrderByNameAsc(CurrentUser.requireId()).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        return CategoryResponse.from(requireOwned(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        UUID userId = CurrentUser.requireId();
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new BusinessException("Ja existe uma categoria com este nome");
        }

        Category category = Category.builder()
                .name(request.name())
                .type(request.type())
                .color(request.color())
                .user(userRepository.getReferenceById(userId))
                .build();

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = requireOwned(id);

        boolean nameChanged = !category.getName().equalsIgnoreCase(request.name());
        if (nameChanged && categoryRepository.existsByUserIdAndNameIgnoreCase(
                CurrentUser.requireId(), request.name())) {
            throw new BusinessException("Ja existe uma categoria com este nome");
        }

        category.setName(request.name());
        category.setType(request.type());
        category.setColor(request.color());

        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = requireOwned(id);
        // Preserva o historico: categoria em uso nao pode sumir do extrato.
        if (transactionRepository.existsByCategoryId(id)) {
            throw new BusinessException(
                    "Categoria possui lancamentos vinculados e nao pode ser removida");
        }
        categoryRepository.delete(category);
    }

    private Category requireOwned(UUID id) {
        return categoryRepository.findByIdAndUserId(id, CurrentUser.requireId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria"));
    }
}
