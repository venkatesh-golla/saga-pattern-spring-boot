package com.appsdeveloperblog.orders.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {
  @Value("${orders.events.topic.name}")
  private String ordersEventsTopicName;

  private static final Integer TOPIC_REPLICATION_FACTOR = 1;
  private static final Integer TOPIC_NUM_PARTITIONS = 3;

  @Bean
  KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  NewTopic createOrdersEventsTopic() {
    return TopicBuilder.name(ordersEventsTopicName)
        .partitions(TOPIC_NUM_PARTITIONS)
        .replicas(TOPIC_REPLICATION_FACTOR)
        .build();
  }
}
