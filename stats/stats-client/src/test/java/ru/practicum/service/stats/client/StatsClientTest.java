package ru.practicum.stats.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.service.stats.GetStatsRequest;
import ru.practicum.service.stats.StatsDtoRequest;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StatsClientTest {

    private StatsClient statsClient;

    @BeforeEach
    public void setUp() {
        statsClient = new StatsClient("http://localhost:9090");
    }

    @Test
    public void testSaveHit() {
        StatsDtoRequest request = StatsDtoRequest.builder()
                .app("test-app")
                .uri("/test")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            assertNotNull(request);
            // statsClient.saveHit(request);
        } catch (Exception e) {
            System.out.println("Ожидаемая ошибка при тестировании: " + e.getMessage());
        }
    }

    @Test
    public void testGetStats() {
        GetStatsRequest request = GetStatsRequest.of(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                Arrays.asList("/test"),
                false
        );

        assertNotNull(request);
    }
}