package org.phoebus.olog.ai;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
public class JacksonConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES);
        return RestClient.builder()
            .messageConverters(converters -> {
                converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                converters.add(new MappingJackson2HttpMessageConverter(mapper));
            });
    }
}