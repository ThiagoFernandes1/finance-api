package com.thiago.financeapi.application.service;

import com.thiago.financeapi.api.exception.BusinessException;
import com.thiago.financeapi.application.dto.AuthDtos.LoginRequest;
import com.thiago.financeapi.application.dto.AuthDtos.RegisterRequest;
import com.thiago.financeapi.application.dto.AuthDtos.TokenResponse;
import com.thiago.financeapi.domain.model.Category;
import com.thiago.financeapi.domain.model.TransactionType;
import com.thiago.financeapi.domain.model.User;
import com.thiago.financeapi.domain.repository.CategoryRepository;
import com.thiago.financeapi.domain.repository.UserRepository;
import com.thiago.financeapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** Categorias criadas junto com a conta para que o usuario ja consiga lancar. */
    private static final List<DefaultCategory> DEFAULT_CATEGORIES = List.of(
            new DefaultCategory("Salario", TransactionType.INCOME, "#2E7D32"),
            new DefaultCategory("Investimentos", TransactionType.INCOME, "#00838F"),
            new DefaultCategory("Moradia", TransactionType.EXPENSE, "#6A1B9A"),
            new DefaultCategory("Alimentacao", TransactionType.EXPENSE, "#EF6C00"),
            new DefaultCategory("Transporte", TransactionType.EXPENSE, "#1565C0"),
            new DefaultCategory("Saude", TransactionType.EXPENSE, "#C62828"),
            new DefaultCategory("Lazer", TransactionType.EXPENSE, "#AD1457"),
            new DefaultCategory("Educacao", TransactionType.EXPENSE, "#4527A0"),
            new DefaultCategory("Outros", TransactionType.EXPENSE, "#546E7A"));

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Ja existe uma conta com este email");
        }

        User user = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build());

        seedDefaultCategories(user);

        return issueToken(user.getEmail());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // Lanca BadCredentialsException, traduzida para 401 pelo handler global.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        return issueToken(request.email());
    }

    private TokenResponse issueToken(String email) {
        return TokenResponse.bearer(jwtService.generateToken(email), jwtService.expirationSeconds());
    }

    private void seedDefaultCategories(User user) {
        List<Category> categories = DEFAULT_CATEGORIES.stream()
                .map(item -> Category.builder()
                        .name(item.name())
                        .type(item.type())
                        .color(item.color())
                        .user(user)
                        .build())
                .toList();
        categoryRepository.saveAll(categories);
    }

    private record DefaultCategory(String name, TransactionType type, String color) {
    }
}
