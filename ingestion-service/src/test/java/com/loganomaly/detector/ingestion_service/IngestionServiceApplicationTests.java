package com.loganomaly.detector.ingestion_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
@TestPropertySource(properties = {
		"spring.kafka.bootstrap-servers=localhost:9092",
		"spring.kafka.topic.log-events=log-events",
		"ingestion.api-key=test-api-key"
})
class IngestionServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
