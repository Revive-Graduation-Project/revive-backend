package com.restaurant.auth;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration / context-load test.
 *
 * RabbitTemplate and ConnectionFactory are mocked so the full Spring context
 * can start without a live RabbitMQ broker. The "test" profile activates
 * application-test.properties which wires an in-memory H2 database and
 * supplies the JWT secret.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

	@MockitoBean
	private RabbitTemplate rabbitTemplate;

	@MockitoBean
	private ConnectionFactory connectionFactory;

	@Test
	void contextLoads() {
		// Passes if the Spring application context starts without errors.
	}
}
