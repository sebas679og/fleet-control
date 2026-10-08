package com.fleet.control.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test-integration")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MsAuthApplicationTests {

  @Test
  void contextLoads() {}
}
