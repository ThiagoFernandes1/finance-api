package com.thiago.financeapi.application.service;

import com.thiago.financeapi.api.exception.BusinessException;
import com.thiago.financeapi.api.exception.ResourceNotFoundException;
import com.thiago.financeapi.application.dto.AccountDtos.AccountRequest;
import com.thiago.financeapi.application.dto.AccountDtos.AccountResponse;
import com.thiago.financeapi.domain.model.Account;
import com.thiago.financeapi.domain.repository.AccountRepository;
import com.thiago.financeapi.domain.repository.TransactionRepository;
import com.thiago.financeapi.domain.repository.UserRepository;
import com.thiago.financeapi.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        return accountRepository.findByUserIdOrderByNameAsc(CurrentUser.requireId()).stream()
                .map(account -> AccountResponse.from(account, currentBalanceOf(account)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        Account account = requireOwned(id);
        return AccountResponse.from(account, currentBalanceOf(account));
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        UUID userId = CurrentUser.requireId();
        if (accountRepository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new BusinessException("Ja existe uma conta com este nome");
        }

        Account account = Account.builder()
                .name(request.name())
                .institution(request.institution())
                .initialBalance(request.initialBalance())
                .user(userRepository.getReferenceById(userId))
                .build();

        Account saved = accountRepository.save(account);
        return AccountResponse.from(saved, saved.getInitialBalance());
    }

    @Transactional
    public AccountResponse update(UUID id, AccountRequest request) {
        Account account = requireOwned(id);

        boolean nameChanged = !account.getName().equalsIgnoreCase(request.name());
        if (nameChanged && accountRepository.existsByUserIdAndNameIgnoreCase(
                CurrentUser.requireId(), request.name())) {
            throw new BusinessException("Ja existe uma conta com este nome");
        }

        account.setName(request.name());
        account.setInstitution(request.institution());
        account.setInitialBalance(request.initialBalance());

        return AccountResponse.from(account, currentBalanceOf(account));
    }

    @Transactional
    public void delete(UUID id) {
        Account account = requireOwned(id);
        if (transactionRepository.existsByAccountId(id)) {
            throw new BusinessException(
                    "Conta possui lancamentos vinculados e nao pode ser removida");
        }
        accountRepository.delete(account);
    }

    private BigDecimal currentBalanceOf(Account account) {
        BigDecimal movements = transactionRepository.sumSignedAmountByAccount(account.getId());
        return account.getInitialBalance().add(movements);
    }

    private Account requireOwned(UUID id) {
        return accountRepository.findByIdAndUserId(id, CurrentUser.requireId())
                .orElseThrow(() -> new ResourceNotFoundException("Conta"));
    }
}
