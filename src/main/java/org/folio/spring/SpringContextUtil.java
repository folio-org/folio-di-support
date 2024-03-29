package org.folio.spring;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import java.lang.reflect.Field;
import java.util.Objects;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxImpl;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * Utility class for creating Spring context in io.vertx.core.Context
 */
@Log4j2
public class SpringContextUtil {

  private static final String SPRING_CONFIGURATION = "spring.configuration";
  private static final String SPRING_CONTEXT_KEY = "springContext";
  private static final String DEFAULT_CONFIGURATION_PACKAGE = "org.folio.spring.config";

  private SpringContextUtil() {}

  /**
   * Creates and adds Spring context into passed io.vertx.core.Context,
   * Spring configuration class is specified using key "spring.configuration" context.config()
   * if context.config() doesn't have this key then package "org.folio.spring.config" is scanned for configuration classes
   */
  public static void init(Vertx vertx, Context context) {
    init(vertx, context, null);
  }

  /**
   * Creates and adds Spring context into passed io.vertx.core.Context,
   * Spring configuration class is specified using key "spring.configuration" context.config()
   * if context.config() doesn't have this key then defaultConfiguration is used
   */
  public static void init(Vertx vertx, Context context, Class<?> defaultConfiguration) {
    String configClassName = context.config().getString(SPRING_CONFIGURATION);
    log.debug("init:: Attempts to create spring base context");

    if (configClassName != null) {
      log.info("init:: configClassName != null");
      try {
        Class<?> springConfigClass = Class.forName(configClassName);
        AnnotationConfigApplicationContext springContext = createBaseContext(vertx, context);
        springContext.register(springConfigClass);
        springContext.refresh();
        context.put(SPRING_CONTEXT_KEY, springContext);
      } catch (ClassNotFoundException e) {
        log.warn("Failed to load configuration class, msg: {}", e.getMessage());
        throw new IllegalStateException();
      }
    } else if (defaultConfiguration != null) {
      log.info("init:: defaultConfiguration != null");
      AnnotationConfigApplicationContext springContext = createBaseContext(vertx, context);
      springContext.register(defaultConfiguration);
      springContext.refresh();
      context.put(SPRING_CONTEXT_KEY, springContext);
    } else {
      log.info("init:: configClassName & defaultConfiguration == null");
      AnnotationConfigApplicationContext springContext = createBaseContext(vertx, context);
      springContext.scan(DEFAULT_CONFIGURATION_PACKAGE);
      springContext.refresh();
      context.put(SPRING_CONTEXT_KEY, springContext);
    }
  }

  /**
   * Injects beans from Spring context associated with io.vertx.core.Context into target object
   */
  public static void autowireDependencies(Object target, Context vertxContext) {
    AbstractApplicationContext springContext = vertxContext.get(SPRING_CONTEXT_KEY);
    springContext.getAutowireCapableBeanFactory()
      .autowireBean(target);
  }

  /**
   * Finds Spring context in one of the verticles from Vertx object, and uses it to inject beans into target.
   * This method exists to be used in tests, when there is only one verticle with Spring context
   */
  public static void autowireDependenciesFromFirstContext(Object target, Vertx vertx) {
    AbstractApplicationContext springContext = getFirstContextFromVertx(vertx);
    springContext.getAutowireCapableBeanFactory().autowireBean(target);
  }

  private static AnnotationConfigApplicationContext createBaseContext(Vertx vertx, Context context) {
    AnnotationConfigApplicationContext springContext = new AnnotationConfigApplicationContext();
    springContext.registerBeanDefinition("context", genericBeanDefinition(ObjectReferenceFactoryBean.class)
      .addConstructorArgValue(context).getBeanDefinition());
    springContext.registerBeanDefinition("vertx", genericBeanDefinition(ObjectReferenceFactoryBean.class)
      .addConstructorArgValue(vertx).getBeanDefinition());
    return springContext;
  }

  private static AbstractApplicationContext getFirstContextFromVertx(Vertx vertx) {
    return (AbstractApplicationContext) vertx.deploymentIDs()
      .stream()
      .flatMap(id -> ((VertxImpl) vertx).getDeployment(id).getVerticles().stream())
      .map(verticle -> {
        log.info("getFirstContextFromVertx:: Attempts to get spring context");
        try {
          Field field = AbstractVerticle.class.getDeclaredField("context");
          field.setAccessible(true);
          return ((Context) field.get(verticle)).get(SPRING_CONTEXT_KEY);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
          log.warn("Failure on getting spring context, msg: {}", ex.getMessage());
          return null;
        }
      })
      .filter(Objects::nonNull)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Spring context was not created"));
  }

  private static class ObjectReferenceFactoryBean<T> implements FactoryBean<T> {

    private final T object;

    public ObjectReferenceFactoryBean(T object) {
      this.object = object;
    }

    @Override
    public T getObject() {
      return object;
    }

    @Override
    public Class<?> getObjectType() {
      return object.getClass();
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }
}
