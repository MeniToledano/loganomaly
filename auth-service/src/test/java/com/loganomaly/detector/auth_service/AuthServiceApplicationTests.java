package com.loganomaly.detector.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:testdb",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"jwt.secret=test-secret-key-for-testing-must-be-at-least-32-characters-long-for-hs256",
	"jwt.expiration=3600000"
})
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
