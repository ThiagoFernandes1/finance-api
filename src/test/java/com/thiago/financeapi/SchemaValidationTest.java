package com.thiago.financeapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sobe o contexto completo contra o banco apontado por DB_URL, sem o profile de
 * teste. Como o perfil padrao usa ddl-auto=validate, qualquer divergencia entre
 * as migrations Flyway e as entidades JPA impede a inicializacao e reprova aqui.
 *
 * <p>So roda quando DB_URL aponta para um PostgreSQL real, o que acontece no job
 * "migrations" da CI. No build local a condicao e falsa e o teste e ignorado,
 * evitando exigir um banco na maquina do desenvolvedor.
 */
@SpringBootTest
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(
        named = "DB_URL", matches = "jdbc:postgresql:.*")
class SchemaValidationTest {

    @Test
    void contextLoadsAgainstMigratedSchema() {
        // O proprio start do contexto e a assercao: ddl-auto=validate falha o boot
        // se alguma tabela ou coluna mapeada nao existir no schema migrado.
    }
}
