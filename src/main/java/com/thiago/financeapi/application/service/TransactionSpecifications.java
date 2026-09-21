package com.thiago.financeapi.application.service;

import com.thiago.financeapi.application.dto.TransactionDtos.TransactionFilter;
import com.thiago.financeapi.domain.model.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Monta o WHERE do extrato a partir dos filtros efetivamente preenchidos. */
final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    static Specification<Transaction> of(UUID userId, TransactionFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("user").get("id"), userId));

            if (filter.startDate() != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("occurredOn"), filter.startDate()));
            }
            if (filter.endDate() != null) {
                predicates.add(builder.lessThanOrEqualTo(
                        root.get("occurredOn"), filter.endDate()));
            }
            if (filter.accountId() != null) {
                predicates.add(builder.equal(root.get("account").get("id"), filter.accountId()));
            }
            if (filter.categoryId() != null) {
                predicates.add(builder.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.type() != null) {
                predicates.add(builder.equal(root.get("type"), filter.type()));
            }
            if (filter.description() != null && !filter.description().isBlank()) {
                predicates.add(builder.like(
                        builder.lower(root.get("description")),
                        "%" + filter.description().toLowerCase() + "%"));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
