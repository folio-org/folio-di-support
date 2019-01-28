package org.folio.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

  public static final String BEAN_VALUE = "TestBean";

  @Bean
  public TestBean testBean() {
    return new TestBean(BEAN_VALUE);
  }
}
