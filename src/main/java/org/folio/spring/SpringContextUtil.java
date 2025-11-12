package org.folio.spring;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxImpl;
import java.lang.reflect.Field;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.lang.Nullable;

/**
 * Utility class for creating Spring context in io.vertx.core.Context
 */
@Log4j2
public class SpringContextUtil {

  private static final String SPRING_CONFIGURATION = "spring.configuration";
  private static final String SPRING_CONTEXT_KEY = "springContext";
  private static final String DEFAULT_CONFIGURATION_PACKAGE = "org.folio.spring.config";
  private static final String CONTEXT_BEAN_NAME = "context";
  private static final String VERTX_BEAN_NAME = "vertx";

  private SpringContextUtil() { }

  /**
   * Creates and adds Spring context into passed io.vertx.core.Context,
   * Spring configuration class is specified using the key "spring.configuration" context.config()
   * if context.config() doesn't have this key, then package "org.folio.spring.config" is scanned for configuration classes
   */
  public static void init(Vertx vertx, Context context) {
    init(vertx, context, null);
  }

  /**
   * Creates and adds Spring context into passed io.vertx.core.Context,
   * Spring configuration class is specified using the key "spring.configuration" context.config()
   * if context.config() doesn't have this key, then the defaultConfiguration is used
   */
  public static void init(Vertx vertx, Context context, Class<?> defaultConfiguration) {
    String configClassName = context.config().getString(SPRING_CONFIGURATION);
    log.info("init:: Creating spring context");

    var springContext = createBaseContext(vertx, context);

    if (configClassName != null) {
      log.info("init:: Loading configuration class from context config: {}", configClassName);
      try {
        Class<?> springConfigClass = Class.forName(configClassName);
        springContext.register(springConfigClass);
      } catch (ClassNotFoundException e) {
        log.error("Failed to load configuration class: {}", configClassName, e);
        throw new IllegalStateException("Configuration class not found: " + configClassName, e);
      }
    } else if (defaultConfiguration != null) {
      log.info("init:: Using provided default configuration: {}", defaultConfiguration.getName());
      springContext.register(defaultConfiguration);
    } else {
      log.info("init:: Scanning package for configuration: {}", DEFAULT_CONFIGURATION_PACKAGE);
      springContext.scan(DEFAULT_CONFIGURATION_PACKAGE);
    }

    springContext.refresh();
    context.put(SPRING_CONTEXT_KEY, springContext);
  }

  /**
   * Injects beans from Spring context associated with io.vertx.core.Context into a target object
   */
  public static void autowireDependencies(Object target, Context vertxContext) {
    AbstractApplicationContext springContext = vertxContext.get(SPRING_CONTEXT_KEY);
    if (springContext == null) {
      throw new IllegalStateException("Spring context not initialized for this Vert.x context");
    }
    springContext.getAutowireCapableBeanFactory().autowireBean(target);
  }

  /**
   * Finds Spring context in one of the verticles from Vertx object, and uses it to inject beans into the target.
   * This method exists to be used in tests when there is only one verticle with Spring context
   */
  public static void autowireDependenciesFromFirstContext(Object target, Vertx vertx) {
    var springContext = getFirstContextFromVertx(vertx);
    springContext.getAutowireCapableBeanFactory().autowireBean(target);
  }

  private static AnnotationConfigApplicationContext createBaseContext(Vertx vertx, Context context) {
    var springContext = new AnnotationConfigApplicationContext();
    springContext.registerBeanDefinition(CONTEXT_BEAN_NAME, genericBeanDefinition(ObjectReferenceFactoryBean.class)
      .addConstructorArgValue(context).getBeanDefinition());
    springContext.registerBeanDefinition(VERTX_BEAN_NAME, genericBeanDefinition(ObjectReferenceFactoryBean.class)
      .addConstructorArgValue(vertx).getBeanDefinition());
    return springContext;
  }

  private static AbstractApplicationContext getFirstContextFromVertx(Vertx vertx) {
    return vertx.deploymentIDs()
      .stream()
      .flatMap(id -> ((VertxImpl) vertx).deploymentManager().deployment(id).deployment().instances().stream())
      .map(SpringContextUtil::extractSpringContext)
      .filter(Objects::nonNull)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Spring context was not created"));
  }

  @SuppressWarnings("java:S3011")
  private static AbstractApplicationContext extractSpringContext(Object deployable) {
    log.debug("extractSpringContext:: Attempting to get spring context from deployable");
    try {
      var field = AbstractVerticle.class.getDeclaredField(CONTEXT_BEAN_NAME);
      field.setAccessible(true);
      var context = (Context) field.get(deployable);
      return context.get(SPRING_CONTEXT_KEY);
    } catch (IllegalAccessException | NoSuchFieldException ex) {
      log.warn("Failed to extract spring context: {}", ex.getMessage());
      return null;
    }
  }

  private record ObjectReferenceFactoryBean<T>(T object) implements FactoryBean<T> {

    @Override
    @Nullable
    public T getObject() {
      return object;
    }

    @Override
    @Nullable
    @SuppressWarnings("java:S2638")
    public Class<?> getObjectType() {
      if (object == null) {
        return null;
      }
      return object.getClass();
    }
  }
}
