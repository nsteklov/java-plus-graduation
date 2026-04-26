package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class StatsClient {

    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final RestTemplate restTemplate;
    private final String statsServiceId;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);
    private static final ParameterizedTypeReference<List<ViewStatsDto>> LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    public StatsClient(DiscoveryClient discoveryClient, @Value("${stats.server.id:stats-server}") String statsServiceId) {
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.restTemplate = new RestTemplate();
        this.retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        log.info("StatsClient инициализирован с id: {}", statsServiceId);
    }

    // Сохранение информации о том, что к эндпоинту был запрос
    public void hit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(FORMATTER.format(timestamp))
                .build();
        try {
            restTemplate.postForEntity(makeUri("/hit"), hitDto, Void.class);
            log.debug("Информация сохранена: app={}, uri={}, ip={}", app, uri, ip);
        } catch (Exception e) {
            log.error("Ошибка сохранения информации: {}", e.getMessage());
        }
    }

    // Получение статистики по посещениям
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        String url = UriComponentsBuilder.fromHttpUrl(makeUri("/stats"))
                .queryParam("start", start == null ? null : encode(start))
                .queryParam("end", end == null ? null : encode(end))
                .queryParamIfPresent("uris", Optional.ofNullable(uris)
                        .filter(list -> !list.isEmpty()))
                .queryParamIfPresent("unique", Optional.ofNullable(unique))
                .build(false)
                .toUriString();

        try {
            log.debug("Получена статистика для {}", url);
            return restTemplate.exchange(url, HttpMethod.GET, null, LIST_TYPE)
                    .getBody();
        } catch (Exception e) {
            log.warn("Не удалось собрать статистику: {}", e.getMessage());
            return List.of();
        }
    }

    // Кодировка даты и времени
    private String encode(LocalDateTime dateTime) {
        return URLEncoder.encode(FORMATTER.format(dateTime), StandardCharsets.UTF_8);
    }

    private String makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
        return "http://" + instance.getHost() + ":" + instance.getPort() + path;
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }
}