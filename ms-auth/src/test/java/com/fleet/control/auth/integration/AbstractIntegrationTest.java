package com.fleet.control.auth.integration;

import com.fleet.control.auth.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests backed by a real PostgreSQL Testcontainer. The container is
 * provided by {@link TestcontainersConfiguration} and wired into the datasource automatically via
 * service connection, so no manual container lifecycle or property registration is needed here.
 */
@ActiveProfiles("test-integration")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {}
