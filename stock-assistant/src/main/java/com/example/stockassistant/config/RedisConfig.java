package com.example.stockassistant.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

// 데이터 성격별로 TTL을 다르게 적용하는 Redis 캐시 설정
@EnableCaching
@Configuration
public class RedisConfig {

	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(5))
				.serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

		Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
		// 실시간 주가: 1~5분
		cacheConfigurations.put("stockPrice", defaultConfig.entryTtl(Duration.ofMinutes(3)));
		// 공시 정보: 30분~1시간
		cacheConfigurations.put("disclosure", defaultConfig.entryTtl(Duration.ofMinutes(30)));
		// 재무제표: 하루~일주일
		cacheConfigurations.put("financials", defaultConfig.entryTtl(Duration.ofDays(1)));
		// Top20 마스터 캐시
		cacheConfigurations.put("top20Master", defaultConfig.entryTtl(Duration.ofHours(6)));

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(defaultConfig)
				.withInitialCacheConfigurations(cacheConfigurations)
				.build();
	}
}
