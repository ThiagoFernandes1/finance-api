package com.thiago.financeapi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Percorre o fluxo real da API sobre o contexto Spring completo: cadastro,
 * autenticacao por JWT, criacao de conta e lancamentos, e leitura do relatorio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void registerUser() throws Exception {
        String email = "usuario-%s@example.com".formatted(UUID.randomUUID());
        String payload = """
                {"name":"Thiago","email":"%s","password":"senhaForte123"}
                """.formatted(email);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        token = read(result).get("accessToken").asText();
    }

    @Test
    @DisplayName("bloqueia acesso sem token")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("bloqueia acesso com token invalido")
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("cria categorias padrao no cadastro")
    void shouldSeedDefaultCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9));
    }

    @Test
    @DisplayName("recusa cadastro com email ja existente")
    void shouldRejectDuplicateEmail() throws Exception {
        String payload = """
                {"name":"Outro","email":"duplicado@example.com","password":"senhaForte123"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ja existe uma conta com este email"));
    }

    @Test
    @DisplayName("recusa cadastro com senha curta e detalha o campo invalido")
    void shouldRejectShortPassword() throws Exception {
        String payload = """
                {"name":"Thiago","email":"curta@example.com","password":"123"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("password"));
    }

    @Test
    @DisplayName("calcula o saldo da conta a partir dos lancamentos")
    void shouldDeriveAccountBalanceFromTransactions() throws Exception {
        String accountId = createAccount("Conta Corrente", "1000.00");
        String salaryId = categoryIdByName("Salario");
        String foodId = categoryIdByName("Alimentacao");

        createTransaction("Salario de setembro", "5000.00", "INCOME", "2026-09-05", accountId, salaryId);
        createTransaction("Supermercado", "450.50", "EXPENSE", "2026-09-10", accountId, foodId);

        // 1000 + 5000 - 450.50
        mockMvc.perform(get("/api/v1/accounts/" + accountId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(5549.50));
    }

    @Test
    @DisplayName("recusa lancamento cujo tipo diverge do tipo da categoria")
    void shouldRejectTypeMismatchAgainstCategory() throws Exception {
        String accountId = createAccount("Carteira", "0.00");
        String foodId = categoryIdByName("Alimentacao");

        String payload = """
                {"description":"Errado","amount":100.00,"type":"INCOME",
                 "occurredOn":"2026-09-10","accountId":"%s","categoryId":"%s"}
                """.formatted(accountId, foodId);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value(Matchers.containsString("nao aceita lancamentos")));
    }

    @Test
    @DisplayName("recusa lancamento com valor zero ou negativo")
    void shouldRejectNonPositiveAmount() throws Exception {
        String accountId = createAccount("Poupanca", "0.00");
        String foodId = categoryIdByName("Alimentacao");

        String payload = """
                {"description":"Invalido","amount":0,"type":"EXPENSE",
                 "occurredOn":"2026-09-10","accountId":"%s","categoryId":"%s"}
                """.formatted(accountId, foodId);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("amount"));
    }

    @Test
    @DisplayName("filtra o extrato por periodo e tipo")
    void shouldFilterStatement() throws Exception {
        String accountId = createAccount("Conta Filtro", "0.00");
        String salaryId = categoryIdByName("Salario");
        String foodId = categoryIdByName("Alimentacao");

        createTransaction("Salario agosto", "4000.00", "INCOME", "2026-08-05", accountId, salaryId);
        createTransaction("Salario setembro", "4200.00", "INCOME", "2026-09-05", accountId, salaryId);
        createTransaction("Mercado setembro", "300.00", "EXPENSE", "2026-09-12", accountId, foodId);

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", bearer())
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-30")
                        .param("type", "INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Salario setembro"));
    }

    @Test
    @DisplayName("consolida o relatorio mensal com quebra por categoria")
    void shouldBuildMonthlyReport() throws Exception {
        String accountId = createAccount("Conta Relatorio", "0.00");
        String salaryId = categoryIdByName("Salario");
        String foodId = categoryIdByName("Alimentacao");

        createTransaction("Salario", "5000.00", "INCOME", "2026-09-05", accountId, salaryId);
        createTransaction("Mercado", "1000.00", "EXPENSE", "2026-09-10", accountId, foodId);

        mockMvc.perform(get("/api/v1/reports/monthly")
                        .header("Authorization", bearer())
                        .param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.totalExpense").value(1000.00))
                .andExpect(jsonPath("$.balance").value(4000.00))
                .andExpect(jsonPath("$.savingsRate").value(80.00))
                .andExpect(jsonPath("$.expensesByCategory[0].categoryName").value("Alimentacao"))
                .andExpect(jsonPath("$.expensesByCategory[0].percentage").value(100.00));
    }

    @Test
    @DisplayName("acompanha o consumo do orcamento do mes")
    void shouldTrackBudgetUsage() throws Exception {
        String accountId = createAccount("Conta Orcamento", "0.00");
        String foodId = categoryIdByName("Alimentacao");

        createTransaction("Mercado", "850.00", "EXPENSE", "2026-09-10", accountId, foodId);

        String payload = """
                {"categoryId":"%s","limitAmount":1000.00,"referenceMonth":"2026-09"}
                """.formatted(foodId);

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spentAmount").value(850.00))
                .andExpect(jsonPath("$.remainingAmount").value(150.00))
                .andExpect(jsonPath("$.usagePercentage").value(85.00))
                .andExpect(jsonPath("$.status").value("WARNING"));
    }

    @Test
    @DisplayName("nao permite orcamento em categoria de receita")
    void shouldRejectBudgetOnIncomeCategory() throws Exception {
        String salaryId = categoryIdByName("Salario");

        String payload = """
                {"categoryId":"%s","limitAmount":1000.00,"referenceMonth":"2026-09"}
                """.formatted(salaryId);

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("impede remover categoria com lancamentos")
    void shouldBlockDeletingCategoryInUse() throws Exception {
        String accountId = createAccount("Conta Delete", "0.00");
        String foodId = categoryIdByName("Alimentacao");

        createTransaction("Mercado", "100.00", "EXPENSE", "2026-09-10", accountId, foodId);

        mockMvc.perform(delete("/api/v1/categories/" + foodId).header("Authorization", bearer()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("isola os dados entre usuarios diferentes")
    void shouldIsolateDataBetweenUsers() throws Exception {
        String accountId = createAccount("Conta Privada", "500.00");

        // Segundo usuario nao deve enxergar a conta do primeiro.
        String otherToken = registerAnotherUser();

        mockMvc.perform(get("/api/v1/accounts/" + accountId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("autentica com as credenciais cadastradas e recusa senha errada")
    void shouldLoginAndRejectWrongPassword() throws Exception {
        String email = "login-%s@example.com".formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Thiago","email":"%s","password":"senhaForte123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"senhaForte123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"senhaErrada1"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private String bearer() {
        return "Bearer " + token;
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String registerAnotherUser() throws Exception {
        String payload = """
                {"name":"Outro","email":"outro-%s@example.com","password":"senhaForte123"}
                """.formatted(UUID.randomUUID());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return read(result).get("accessToken").asText();
    }

    private String createAccount(String name, String initialBalance) throws Exception {
        String payload = """
                {"name":"%s","institution":"Banco Teste","initialBalance":%s}
                """.formatted(name, initialBalance);

        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return read(result).get("id").asText();
    }

    private void createTransaction(String description, String amount, String type,
                                   String occurredOn, String accountId, String categoryId)
            throws Exception {
        String payload = """
                {"description":"%s","amount":%s,"type":"%s",
                 "occurredOn":"%s","accountId":"%s","categoryId":"%s"}
                """.formatted(description, amount, type, occurredOn, accountId, categoryId);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private String categoryIdByName(String name) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/v1/categories").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();

        for (JsonNode node : read(result)) {
            if (node.get("name").asText().equals(name)) {
                return node.get("id").asText();
            }
        }
        throw new AssertionError("Categoria padrao nao encontrada: " + name);
    }
}
