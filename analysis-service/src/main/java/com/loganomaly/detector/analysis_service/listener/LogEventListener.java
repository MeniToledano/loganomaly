package com.loganomaly.detector.analysis_service.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class LogEventListener {

    private static final Logger logger = LoggerFactory.getLogger(LogEventListener.class);

    @KafkaListener(topics = "${spring.kafka.topic.log-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        logger.info("Received log event from Kafka: {}", message);
        // TODO: Process the log event and perform analysis
        // This is where you would add your anomaly detection logic
    }
}

