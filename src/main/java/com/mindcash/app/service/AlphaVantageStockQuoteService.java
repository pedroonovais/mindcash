package com.mindcash.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Implementação que usa a API Alpha Vantage (GLOBAL_QUOTE) para obter a variação diária de ações.
 * Requer app.investments.alpha-vantage-api-key configurado.
 */
@Service
@ConditionalOnProperty(name = "app.investments.stock-api-enabled", havingValue = "true")
public class AlphaVantageStockQuoteService implements StockQuoteService {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageStockQuoteService.class);
    private static final String BASE_URL = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.investments.alpha-vantage-api-key:}")
    private String apiKey;

    @Override
    public Optional<Double> getDailyReturnPercent(String symbol, boolean isBrazil) {
        if (symbol == null || symbol.isBlank() || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeSymbol(symbol.trim(), isBrazil);
        String url = String.format(BASE_URL, normalized, apiKey);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode quote = root.path("Global Quote");
            if (quote.isMissingNode() || quote.isEmpty()) {
                if (root.has("Note") || root.has("Information")) {
                    log.debug("Alpha Vantage rate limit or info: {}", root.has("Note") ? root.get("Note").asText() : root.get("Information").asText());
                }
                return Optional.empty();
            }

            String changePercentStr = quote.path("10. change percent").asText().trim();
            if (!changePercentStr.isEmpty()) {
                String cleaned = changePercentStr.replace("%", "").trim();
                double pct = Double.parseDouble(cleaned);
                return Optional.of(pct);
            }

            String priceStr = quote.path("05. price").asText();
            String prevCloseStr = quote.path("08. previous close").asText();
            if (!priceStr.isEmpty() && !prevCloseStr.isEmpty()) {
                double price = Double.parseDouble(priceStr);
                double prev = Double.parseDouble(prevCloseStr);
                if (prev != 0) {
                    double pct = ((price - prev) / prev) * 100.0;
                    return Optional.of(pct);
                }
            }
        } catch (Exception e) {
            log.debug("Falha ao obter cotação para {}: {}", normalized, e.getMessage());
        }
        return Optional.empty();
    }

    private static String normalizeSymbol(String symbol, boolean isBrazil) {
        if (!isBrazil) {
            return symbol;
        }
        if (!symbol.endsWith(".SA") && !symbol.endsWith(".SA.BR")) {
            return symbol + ".SA";
        }
        return symbol;
    }
}
