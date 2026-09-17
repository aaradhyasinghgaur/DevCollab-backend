package com.example.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        String redisUrl = System.getenv("REDIS_URL");
        if (redisUrl != null && !redisUrl.isBlank()) {
            try {
                URI uri = new URI(redisUrl);
                RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
                config.setHostName(uri.getHost());
                config.setPort(uri.getPort() == -1 ? 6379 : uri.getPort());
                if (uri.getUserInfo() != null) {
                    String userInfo = uri.getUserInfo();
                    String password = userInfo.contains(":") ? userInfo.split(":", 2)[1] : userInfo;
                    config.setPassword(password);
                }
                log.info("[RedisConfig] Initialized Lettuce from REDIS_URL -> {}:{}", uri.getHost(), uri.getPort());
                return new LettuceConnectionFactory(config);
            } catch (Exception e) {
                log.warn("[RedisConfig] Failed to parse REDIS_URL: {}", e.getMessage());
            }
        }

        String host = System.getenv("REDIS_HOST");
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        String portStr = System.getenv("REDIS_PORT");
        int port = 6379;
        if (portStr != null && !portStr.isBlank()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ignored) {}
        }
        String password = System.getenv("REDIS_PASSWORD");

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        log.info("[RedisConfig] Initialized Lettuce for host -> {}:{}", host, port);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
