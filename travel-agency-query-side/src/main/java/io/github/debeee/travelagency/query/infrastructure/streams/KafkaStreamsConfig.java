package io.github.debeee.travelagency.query.infrastructure.streams;

import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {
    @Bean
    public StreamsBuilderFactoryBeanConfigurer streamsBuilderFactoryBeanConfigurer() {
        return factoryBean -> factoryBean.setStreamsUncaughtExceptionHandler(exception ->
                StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD);
    }
}