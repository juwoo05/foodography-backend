package kopo.poly.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

@Configuration
public class RedisConfiguration {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.username}")
    private String redisUsername;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    // ── yml: spring.data.redis.timeout ───────────────────────────────────────
    @Value("${spring.data.redis.timeout:2s}")
    private Duration redisTimeout;

    // ── yml: spring.data.redis.lettuce.pool.* ────────────────────────────────
    @Value("${spring.data.redis.lettuce.pool.max-active:32}")
    private int poolMaxActive;

    @Value("${spring.data.redis.lettuce.pool.max-idle:16}")
    private int poolMaxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:4}")
    private int poolMinIdle;

    @Value("${spring.data.redis.lettuce.pool.max-wait:5s}")
    private Duration poolMaxWait;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {

        // 접속 정보
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);
        serverConfig.setUsername(redisUsername);
        serverConfig.setPassword(redisPassword);

        // 커넥션 풀 (commons-pool2 필요)
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(poolMaxActive);
        poolConfig.setMaxIdle(poolMaxIdle);
        poolConfig.setMinIdle(poolMinIdle);
        poolConfig.setMaxWait(poolMaxWait);

        LettucePoolingClientConfiguration lettuceConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(redisTimeout)
                .poolConfig(poolConfig)
                .build();

        return new LettuceConnectionFactory(serverConfig, lettuceConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        return redisTemplate;
    }
}
