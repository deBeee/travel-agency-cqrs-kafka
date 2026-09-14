package io.github.debeee.travelagency.query.infrastructure.kafka;

import io.github.debeee.travelagency.query.infrastructure.configuration.properties.AppTopicsProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DltTopicResolver {
    private final Map<String, String> sourceToDlt;

    public DltTopicResolver(AppTopicsProperties topicsProperties) {
        this.sourceToDlt = Map.of(
                topicsProperties.availability(), topicsProperties.availabilityDlt()
        );
    }

    public TopicPartition resolve(ConsumerRecord<?, ?> consumerRecord) {
        String dltTopic = sourceToDlt.get(consumerRecord.topic());
        
        if (dltTopic == null) {
            throw new IllegalStateException("No DLT mapping configured for topic: %s".formatted(consumerRecord.topic()));
        }
        
        return new TopicPartition(dltTopic, consumerRecord.partition());
    }
}
