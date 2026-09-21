package com.thiago.financeapi.application.service;

import com.thiago.financeapi.api.exception.BusinessException;
import com.thiago.financeapi.api.exception.ResourceNotFoundException;
import com.thiago.financeapi.application.dto.TransactionDtos.TransactionFilter;
import com.thiago.financeapi.application.dto.TransactionDtos.TransactionRequest;
import com.thiago.financeapi.application.dto.TransactionDtos.TransactionResponse;
import com.thiago.financeapi.domain.model.Account;
import com.thiago.financeapi.domain.model.Category;
import com.thiago.financeapi.domain.model.Transaction;
import com.thiago.financeapi.domain.repository.AccountRepository;
import com.thiago.financeapi.domain.repository.CategoryRepository;
import com.thiago.financeapi.domain.repository.TransactionRepository;
import com.thiago.financeapi.domain.repository.UserRepository;
import com.thiago.financeapi.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> search(TransactionFilter filter, Pageable pageable) {
        return transactionRepository
                .findAll(TransactionSpecifications.of(CurrentUser.requireId(), filter), pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID id) {
        return TransactionResponse.from(requireOwned(id));
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        UUID userId = CurrentUser.requireId();
        Account account = requireAccount(request.accountId(), userId);
        Category category = requireCategory(request.categoryId(), userId);
        assertCategoryMatchesType(category, request);

        Transaction transaction = Transaction.builder()
                .description(request.description())
                .amount(request.amount())
                .type(request.type())
                .occurredOn(request.occurredOn())
                .account(account)
                .category(category)
                .user(userRepository.getReferenceById(userId))
                .build();

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(UUID id, TransactionRequest request) {
        UUID userId = CurrentUser.requireId();
        Transaction transaction = requireOwned(id);
        Account account = requireAccount(request.accountId(), userId);
        Category category = requireCategory(request.categoryId(), userId);
        assertCategoryMatchesType(category, request);

        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setOccurredOn(request.occurredOn());
        transaction.setAccount(account);
        transaction.setCategory(category);

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(UUID id) {
        transactionRepository.delete(requireOwned(id));
    }

    /**
     * Impede classificar uma despesa em categoria de receita e vice-versa, o que
     * distorceria silenciosamente todos os relatorios.
     */
    private void assertCategoryMatchesType(Category category, TransactionRequest request) {
        if (category.getType() != request.type()) {
            throw new BusinessException(
                    "Categoria '%s' e do tipo %s e nao aceita lancamentos do tipo %s"
                            .formatted(category.getName(), category.getType(), request.type()));
        }
    }

    private Transaction requireOwned(UUID id) {
        return transactionRepository.findByIdAndUserId(id, CurrentUser.requireId())
                .orElseThrow(() -> new ResourceNotFoundException("Lancamento"));
    }

    private Account requireAccount(UUID accountId, UUID userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta"));
    }

    private Category requireCategory(UUID categoryId, UUID userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria"));
    }
}
