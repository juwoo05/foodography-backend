package kopo.poly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * FastAPI 서버 호출용 WebClient 설정
 * - WebConfig(MVC 라우팅)와 관심사가 다르므로 분리 유지
 * - Netty 저수준 API(ChannelOption) 미사용 → 모듈 접근성 경고 없음
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {

        HttpClient httpClient = HttpClient.create(
                        // 커넥션 풀 설정 — 연결 수립 타임아웃 포함
                        ConnectionProvider.builder("fastapi-pool")
                                .maxConnections(50)
                                .pendingAcquireTimeout(Duration.ofSeconds(5))  // 연결 수립 대기 타임아웃
                                .build()
                )
                .responseTimeout(Duration.ofSeconds(95));  // 읽기 타임아웃 — VLM 처리 시간 고려

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // FastAPI 응답 크기 대비 (기본 256KB → 10MB)
                .codecs(config -> config.defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}