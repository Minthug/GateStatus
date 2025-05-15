package com.example.GateStatus.global.config.redis;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RepresentationModel;

import java.io.IOException;
import java.io.Serial;
import java.util.HashMap;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Java 8 날짜/시간 모듈 등록 (필요)
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 추가 설정
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // HATEOAS 관련 클래스는 직렬화에서 제외 (중요)
        SimpleModule module = new SimpleModule();
        module.addSerializer(Links.class, new LinksIgnoringSerializer());
        module.addSerializer(RepositoryLinksResource.class, new RepositoryLinksIgnoringSerializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        HateoasAwareRedisSerializer serializer = new HateoasAwareRedisSerializer(redisObjectMapper());

        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer);
        return redisTemplate;
    }


    @Bean
    public RedisTemplate<String, Long> stringLongRedisTemplate() {
        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        return redisTemplate;
    }

    @Bean
    public ListOperations<String, String> listOperations(RedisTemplate<String, String> redisTemplate) {
        return redisTemplate.opsForList();
    }

    @Bean
    public RedisTemplate<String, Object> businessObjectRedisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // GenericJackson2JsonRedisSerializer 사용 (HATEOAS 모듈 없이)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer);
        return redisTemplate;
    }

    static class LinksIgnoringSerializer extends StdSerializer<Links> {
        private static final long serialVersionUID = 1L;

        public LinksIgnoringSerializer() {
            super(Links.class);
        }

        @Override
        public void serialize(Links value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeEndObject();
        }
    }

    static class RepositoryLinksIgnoringSerializer extends StdSerializer<RepositoryLinksResource> {
        private static final long serialVersionUID = 1L;

        public RepositoryLinksIgnoringSerializer() {
            super(RepositoryLinksResource.class);
        }

        @Override
        public void serialize(RepositoryLinksResource value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeEndObject();
        }
    }

    static class HateoasAwareRedisSerializer extends GenericJackson2JsonRedisSerializer {
        public HateoasAwareRedisSerializer(ObjectMapper mapper) {
            super(mapper);
        }

        @Override
        public byte[] serialize(Object source) throws SerializationException {
            if (source instanceof Links ||
                source instanceof RepositoryLinksResource ||
                source instanceof RepresentationModel) {
                return super.serialize(new HashMap<>());
            }
            return super.serialize(source);
        }
    }
}

