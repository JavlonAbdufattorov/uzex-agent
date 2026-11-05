
package uz.hack.uzex_agent.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AnalyticsService analyticsService;

    private final String apiKey;
    private final String model;

    public GeminiService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.1-flash-lite}") String model,
            AnalyticsService analyticsService,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    // Разрешённые функции, которые Gemini может вызывать
    private final List<Map<String, Object>> tools = List.of(

        // 1. Крупнейшие покупатели
        Map.of(
                "type", "function",
                "name", "getTopBuyers",
                "description", "Возвращает крупнейших покупателей UZEX по количеству сделок и общей сумме.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "limit", Map.of(
                                        "type", "integer",
                                        "description", "Количество покупателей от 1 до 100"
                                )
                        ),
                        "required", List.of("limit")
                )
        ),

        // 2. Крупнейшие продавцы
        Map.of(
                "type", "function",
                "name", "getTopSellers",
                "description", "Возвращает крупнейших продавцов UZEX по количеству сделок и общей сумме.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "limit", Map.of(
                                        "type", "integer",
                                        "description", "Количество продавцов от 1 до 100"
                                )
                        ),
                        "required", List.of("limit")
                )
        ),

        // 3. Общая статистика
        Map.of(
                "type", "function",
                "name", "getOverview",
                "description", "Возвращает общую статистику: количество сделок, контрактов, участников и брокеров.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of()
                )
        ),

        // 4. Популярные товары
        Map.of(
                "type", "function",
                "name", "getTopProducts",
                "description", "Возвращает наиболее торгуемые товары UZEX.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "limit", Map.of(
                                        "type", "integer",
                                        "description", "Количество товаров от 1 до 100"
                                )
                        ),
                        "required", List.of("limit")
                )
        ),

        // 5. Статистика по регионам
        Map.of(
                "type", "function",
                "name", "getRegions",
                "description", "Возвращает статистику биржевых сделок по регионам.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of()
                )
        ),

        // 6. Статистика по годам
        Map.of(
                "type", "function",
                "name", "getYearlyAnalytics",
                "description", "Возвращает количество сделок и другие показатели по годам.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of()
                )
        ),

        // 7. Помесячная статистика за весь период
        Map.of(
                "type", "function",
                "name", "getMonthlyAnalytics",
                "description", "Возвращает помесячную статистику сделок за весь доступный период.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of()
                )
        ),

        // 8. Помесячная статистика за конкретный год
        Map.of(
                "type", "function",
                "name", "getMonthlyAnalyticsByYear",
                "description", "Возвращает помесячную статистику сделок за указанный год. Доступны годы с 2022 по 2026.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "year", Map.of(
                                        "type", "integer",
                                        "description", "Год от 2022 до 2026"
                                )
                        ),
                        "required", List.of("year")
                )
        ),
        Map.of(
                "type", "function",
                "name", "getTopProductsByDeals",
                "description", "Возвращает товары, отсортированные по количеству сделок от большего к меньшему.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "limit", Map.of(
                                        "type", "integer",
                                        "description", "Количество товаров от 1 до 100"
                                )
                        ),
                        "required", List.of("limit")
                )
        ),

        Map.of(
                "type", "function",
                "name", "getTopProductsByAmount",
                "description", "Возвращает товары, отсортированные по общей сумме сделок от большей к меньшей.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "limit", Map.of(
                                        "type", "integer",
                                        "description", "Количество товаров от 1 до 100"
                                )
                        ),
                        "required", List.of("limit")
                )
        ),
        Map.of(
        "type", "function",
        "name", "getCompanyAnalytics",
        "description",
        """
        Ищет конкретную компанию UZEX по названию или ИНН.
        Возвращает информацию о компании как покупателе и/или продавце:
        количество сделок, общую сумму, лоты и количество контрагентов.
        Используй эту функцию, когда пользователь спрашивает
        о конкретной компании или организации.
        """,
        "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of(
                                "type", "string",
                                "description",
                                "Название компании или ИНН"
                        ),
                        "limit", Map.of(
                                "type", "integer",
                                "description",
                                "Количество результатов от 1 до 100"
                        )
                ),
                "required", List.of("query", "limit")
        )
),

Map.of(
    "type", "function",
    "name", "getBrokerAnalytics",
    "description", "Search a UZEX broker by name, INN or broker code and retrieve its deal statistics by buyer and seller roles, unique deals, amounts and counterparties.",
    "parameters", Map.of(
        "type", "object",
        "properties", Map.of(
            "query", Map.of(
                "type", "string",
                "description", "Broker name, INN or broker code"
            ),
            "limit", Map.of(
                "type", "integer",
                "description", "Maximum number of brokers to return"
            )
        ),
        "required", List.of("query", "limit")
    )
)



);

    public String ask(String question) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not configured. Set it before calling /api/ai/ask."
            );
        }

        Map<String, Object> request = new HashMap<>();

        request.put("model", model);
        request.put("tools", tools);

        request.put(
                "input",
                """
Ты AI-аналитик проекта UZEX.

Ты работаешь с реальными биржевыми данными из DuckDB.

Для аналитических вопросов обязательно используй доступные функции.

Правила:

1. Для вопросов о покупателях используй getTopBuyers.
2. Для вопросов о продавцах используй getTopSellers.
3. Для общей статистики используй getOverview.
4. Для вопросов о товарах используй getTopProducts.
5. Для региональной аналитики используй getRegions.
6. Для годовой динамики используй getYearlyAnalytics.
7. Для помесячной динамики за весь период используй getMonthlyAnalytics.
8. Для конкретного года используй getMonthlyAnalyticsByYear.
АНАЛИТИКА ТОВАРОВ:

1. Если пользователь спрашивает о самых часто продаваемых,
   часто покупаемых или популярных по количеству сделок товарах,
   вызывай getTopProductsByDeals.

2. Если пользователь спрашивает о наибольшем обороте,
   общей сумме или крупнейших товарных категориях по стоимости,
   вызывай getTopProductsByAmount.

3. Не называй денежную сумму в сумах, долларах или другой валюте,
   если валюта явно не указана в данных функции.

4. Не смешивай понятия количества сделок, количества лотов
   и денежного оборота.

5. Не объединяй похожие названия товаров без подтверждения,
   что это одна и та же товарная позиция.q


ПРАВИЛА РАБОТЫ С АНАЛИТИКОЙ ПО БРОКЕРАМ:

- Если пользователь запрашивает информацию о брокере на UZEX, используйте функцию `getBrokerAnalytics`.
- Поиск брокеров осуществляйте по их официальному наименованию, ИНН или коду брокера.
- Не используйте `getCompanyAnalytics` вместо аналитики по брокерам.
- Отображайте активность покупателей и продавцов раздельно.
- Разграничивайте количество сделок по ролям (покупатель/продавец) и общее количество уникальных сделок.
- Для расчета общей суммы по уникальным сделкам используйте показатель `totalUniqueAmount`.
- Для определения общего числа уникальных контрагентов используйте показатель `totalUniqueCounterparties`.
- Никогда не суммируйте показатели покупателей и продавцов для получения «общего уникального» итога.
- Если брокер не найден, сообщите, что в базе данных брокеров не обнаружено подходящих записей.
- Категорически запрещается выдумывать или фальсифицировать статистические данные о брокерах.

АНАЛИТИКА КОМПАНИЙ:

1. Если пользователь спрашивает о конкретной компании,
   организации или участнике биржи, обязательно вызывай
   getCompanyAnalytics.

2. Используй название компании или ИНН из вопроса
   в параметре query.

3. Не используй getTopBuyers или getTopSellers
   вместо поиска конкретной компании.

4. Если найдено несколько компаний с похожими названиями,
   покажи варианты и попроси пользователя уточнить.

5. Если компания найдена среди покупателей,
   объясни её показатели как покупателя.

6. Если компания найдена среди продавцов,
   объясни её показатели как продавца.

7. Если компания найдена в обеих ролях,
   покажи обе роли отдельно.

8. Если результатов нет, честно сообщи,
   что компания не найдена в доступной базе данных.

9. Не выдумывай показатели и не приписывай компании
   данные другой организации.

Не выдумывай числа, названия компаний и статистику.

Используй только данные, полученные через функции.

Если функция вернула ошибку, сообщи об этом пользователю.

Не утверждай, что данные относятся к конкретному периоду,
если функция не предоставляет такую информацию.

Учитывай, что данные за 2026 год могут быть неполными.

Не складывай суммы в разных валютах без подтверждённой конвертации.

Отвечай на русском языке, структурированно и понятно.
""" + question
        );

        JsonNode interaction = sendWithRetry(request);

        // Поддерживаем последовательные вызовы функций
        for (int round = 0; round < 3; round++) {

            List<Object> functionResults = new ArrayList<>();

            for (JsonNode step : interaction.path("steps")) {

                if (!"function_call".equals(
                        step.path("type").asText()
                )) {
                    continue;
                }

                String functionName = step.path("name").asText();
                String callId = step.path("id").asText();

                JsonNode arguments = step.path("arguments");

                Object result = executeFunction(
                        functionName,
                        arguments
                );

                Map<String, Object> resultContent = Map.of(
                        "type", "text",
                        "text", toJson(result)
                );

                Map<String, Object> functionResult = Map.of(
                        "type", "function_result",
                        "name", functionName,
                        "call_id", callId,
                        "result", List.of(resultContent)
                );

                functionResults.add(functionResult);
            }

            // Gemini не запросил функцию — значит, готов ответ
            if (functionResults.isEmpty()) {
                return extractText(interaction);
            }

            // Передаём результаты функций обратно Gemini
            Map<String, Object> followUp = new HashMap<>();

            followUp.put("model", model);
            followUp.put(
                    "previous_interaction_id",
                    interaction.path("id").asText()
            );
            followUp.put("tools", tools);
            followUp.put("input", functionResults);

            interaction = sendWithRetry(followUp);
        }

        throw new IllegalStateException(
                "Gemini превысил допустимое количество вызовов функций"
        );
    }

    // Выполняем только разрешённые Java-функции
    private Object executeFunction(
        String functionName,
        JsonNode arguments
) {

    try {

        return switch (functionName) {
            case "getTopProductsByDeals" -> {
                int limit = arguments.path("limit").asInt(5);
                yield analyticsService.getTopProductsByDeals(limit);
                }

                case "getTopProductsByAmount" -> {
                int limit = arguments.path("limit").asInt(5);
                yield analyticsService.getTopProductsByAmount(limit);
                }
            case "getTopBuyers" -> {
                int limit = arguments.path("limit").asInt(5);
                yield analyticsService.getTopBuyers(limit);
            }
            case "getCompanyAnalytics" -> {

                String query = arguments.path("query").asText("");

                int limit = arguments.path("limit").asInt(10);

                yield analyticsService.getCompanyAnalytics(
                        query,
                        limit
                );
                }

            case "getTopSellers" -> {
                int limit = arguments.path("limit").asInt(5);
                yield analyticsService.getTopSellers(limit);
            }

            case "getTopProducts" -> {
                int limit = arguments.path("limit").asInt(5);
                yield analyticsService.getTopProducts(limit);
            }

            case "getOverview" ->
                    analyticsService.getOverview();

            case "getRegions" ->
                    analyticsService.getRegionsAnalytics();

            case "getYearlyAnalytics" ->
                    analyticsService.getYearlyAnalytics();

            case "getMonthlyAnalytics" ->
                    analyticsService.getMonthlyAnalytics();
            case "getBrokerAnalytics" -> {
                String query = arguments.path("query").asText("");
                int limit = arguments.path("limit").asInt(10);

                yield analyticsService.getBrokerAnalytics(query, limit);
                }
            case "getMonthlyAnalyticsByYear" -> {
                if (!arguments.has("year")
                        || !arguments.path("year").canConvertToInt()) {
                    yield Map.of("error", "Необходимо указать корректный год");
                }

                int year = arguments.path("year").asInt();

                yield analyticsService.getMonthlyAnalyticsByYear(year);
            }

            default -> Map.of(
                    "error",
                    "Unknown function: " + functionName
            );
        };

    } catch (IllegalArgumentException e) {

        return Map.of(
                "error",
                e.getMessage()
        );
    }
}

    // Преобразуем Java-объекты в JSON
    private String toJson(Object object) {

        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Не удалось преобразовать результат в JSON",
                    e
            );
        }
    }

    // Извлекаем финальный текстовый ответ Gemini
    private String extractText(JsonNode response) {

        StringBuilder answer = new StringBuilder();

        for (JsonNode step : response.path("steps")) {

            if (!"model_output".equals(
                    step.path("type").asText()
            )) {
                continue;
            }

            for (JsonNode item : step.path("content")) {

                if (item.has("text")) {
                    answer.append(item.path("text").asText());
                }
            }
        }

        if (answer.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini не вернул текстовый ответ"
            );
        }

        return answer.toString();
    }

    // HTTP-запрос с повторными попытками при 503
    private JsonNode sendWithRetry(
            Map<String, Object> request
    ) {

        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                return restClient.post()
                        .uri(
                                "https://generativelanguage.googleapis.com/v1beta/interactions"
                        )
                        .header("x-goog-api-key", apiKey)
                        .body(request)
                        .retrieve()
                        .body(JsonNode.class);

            } catch (
                    HttpServerErrorException.ServiceUnavailable e
            ) {

                if (attempt == maxAttempts) {
                    throw new IllegalStateException(
                            "Gemini временно перегружен",
                            e
                    );
                }

                try {
                    Thread.sleep(
                            1000L * (1L << (attempt - 1))
                    );
                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(
                            "Ожидание Gemini прервано",
                            interruptedException
                    );
                }
            }
        }

        throw new IllegalStateException(
                "Не удалось получить ответ Gemini"
        );
    }
}
