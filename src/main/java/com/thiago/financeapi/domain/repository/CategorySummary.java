package com.thiago.financeapi.domain.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Projecao de agregacao por categoria, preenchida diretamente pelo JPQL. */
public interface CategorySummary {

    UUID getCategoryId();

    String getCategoryName();

    BigDecimal getTotal();
}
