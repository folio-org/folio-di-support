package org.folio.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.folio.spring.config.TestConfiguration2;
import org.folio.util.TestAutowireTarget;
import org.folio.util.TestConfiguration;
import org.junit.Before;
import org.junit.Test;

public class SpringContextUtilTest {

  private Vertx vertx;
  private Context context;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
    context = vertx.getOrCreateContext();
  }

  @Test
  public void shouldInjectVertxAndContextBeans() {
    SpringContextUtil.init(vertx, context);

    TestAutowireTarget autowireTarget = new TestAutowireTarget();
    SpringContextUtil.autowireDependencies(autowireTarget, context);

    assertEquals(vertx, autowireTarget.getVertx());
    assertEquals(context, autowireTarget.getContext());
  }

  @Test
  public void shouldInjectBeanWhenConfigurationHasIt() {
    SpringContextUtil.init(vertx, context, TestConfiguration.class);

    TestAutowireTarget autowireTarget = new TestAutowireTarget();
    SpringContextUtil.autowireDependencies(autowireTarget, context);

    assertEquals(TestConfiguration.BEAN_VALUE, autowireTarget.getTestBean().getValue());
  }

  @Test
  public void shouldGetConfigurationFromVertxContextConfig() {
    context.config().put("spring.configuration", "org.folio.util.TestConfiguration");
    SpringContextUtil.init(vertx, context);

    TestAutowireTarget autowireTarget = new TestAutowireTarget();
    SpringContextUtil.autowireDependencies(autowireTarget, context);

    assertEquals(TestConfiguration.BEAN_VALUE, autowireTarget.getTestBean().getValue());
  }

  /**
   * Tests that package org.folio.spring.config is scanned and TestConfiguration2 class is found
   * when Spring configuration is not specified
   */
  @Test
  public void shouldGetConfigurationByScanningPackage() {
    SpringContextUtil.init(vertx, context);

    TestAutowireTarget autowireTarget = new TestAutowireTarget();
    SpringContextUtil.autowireDependencies(autowireTarget, context);

    assertEquals(TestConfiguration2.BEAN_VALUE, autowireTarget.getTestBean().getValue());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionWhenConfigurationClassNotFound() {
    context.config().put("spring.configuration", "org.folio.NonExistentConfiguration");
    SpringContextUtil.init(vertx, context);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionWhenSpringContextNotInitialized() {
    TestAutowireTarget autowireTarget = new TestAutowireTarget();
    SpringContextUtil.autowireDependencies(autowireTarget, context);
  }

  @Test
  public void shouldAutowireDependenciesFromFirstContext() throws Exception {
    DeploymentOptions options = new DeploymentOptions().setConfig(context.config());

    vertx.deployVerticle(new AbstractVerticle() {
      @Override
      public void start() {
        SpringContextUtil.init(vertx, context, TestConfiguration.class);
      }
    }, options).toCompletionStage().toCompletableFuture().get();

    TestAutowireTarget autowireTarget = new TestAutowireTarget();
    SpringContextUtil.autowireDependenciesFromFirstContext(autowireTarget, vertx);

    assertNotNull(autowireTarget.getTestBean());
    assertEquals(TestConfiguration.BEAN_VALUE, autowireTarget.getTestBean().getValue());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionWhenNoSpringContextInVertx() {
    TestAutowireTarget autowireTarget = new TestAutowireTarget();
    SpringContextUtil.autowireDependenciesFromFirstContext(autowireTarget, vertx);
  }
}
