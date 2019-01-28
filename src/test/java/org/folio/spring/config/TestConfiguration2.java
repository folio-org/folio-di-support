package org.folio.spring.config;

import org.folio.util.TestBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration2 {

  public static final String BEAN_VALUE = "TestBean2";

  @Bean
  public TestBean testBean() {
    return new TestBean(BEAN_VALUE);
  }
}
