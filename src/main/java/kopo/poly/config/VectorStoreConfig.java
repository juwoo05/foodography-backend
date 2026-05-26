package kopo.poly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring AI VectorStore + 스케줄링 설정
 *
 * <p><b>Spring AI 자동 구성 (auto-configuration)</b></p>
 * <ul>
 *   <li>{@code BedrockTitanEmbeddingModel} —
 *       {@code spring.ai.bedrock.titan.embedding.enabled=true} 시 자동 등록</li>
 *   <li>{@code PineconeVectorStore} —
 *       {@code spring.ai.vectorstore.pinecone.*} 프로퍼티 + EmbeddingModel 로 자동 등록</li>
 *   <li>{@code BedrockAnthropicChatModel} —
 *       {@code spring.ai.bedrock.anthropic.chat.enabled=true} 시 자동 등록</li>
 * </ul>
 *
 * <p>위 Bean 들은 {@code application.yml} 의 {@code spring.ai.*} 섹션으로 제어합니다.
 * 별도 {@code @Bean} 선언 없이 자동 구성에 위임합니다.</p>
 *
 * <p>{@code @EnableScheduling} 으로 {@link kopo.poly.scheduler.FoodVectorDbScheduler} 를
 * 활성화합니다.</p>
 */
@Configuration
@EnableScheduling
public class VectorStoreConfig {
    // 자동 구성에 위임 — application.yml spring.ai.* 섹션 참조
}
