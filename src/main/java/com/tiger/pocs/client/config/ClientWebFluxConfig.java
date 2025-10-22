package com.tiger.pocs.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Slf4j
@Configuration
public class ClientWebFluxConfig implements WebFluxConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("Configuring CORS for Client API module");
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Content-Type", "Authorization", "X-Request-Id")
            .maxAge(3600);
    }
    
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        log.info("Configuring HTTP message codecs for Client API module");
        
        ObjectMapper objectMapper = objectMapper();
        
        configurer.defaultCodecs().jackson2JsonEncoder(
            new Jackson2JsonEncoder(objectMapper)
        );
        configurer.defaultCodecs().jackson2JsonDecoder(
            new Jackson2JsonDecoder(objectMapper)
        );
        
        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024);
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }
}