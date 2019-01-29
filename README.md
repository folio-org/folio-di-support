# folio-di-support

Copyright (C) 2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Dependency Injection support for FOLIO backend modules

## Overview

### Creating Spring context
SpringContextUtil#init method initializes Spring context and adds it to Vertx Context object.

[InitAPI hook](https://github.com/folio-org/raml-module-builder#adding-an-init-implementation) can be used to initialize context during start up:

```
public class InitAPIImpl implements InitAPI {
 @Override
 public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    vertx.executeBlocking(
      future -> {
        SpringContextUtil.init(vertx, context, ApplicationConfig.class);
        future.complete();
      },
      result -> {
        if (result.succeeded()) {
          handler.handle(Future.succeededFuture(true));
        } else {
          handler.handle(Future.failedFuture(result.cause()));
        }
      });
  }
}
```
### Accessing Spring context from endpoint classes
SpringContextUtil#autowireDependencies method gets Spring context from Vertx context and uses it to inject beans into target object. 
Example of injecting Spring beans into API object:
```
public class EholdingsProxyTypesImpl implements EholdingsProxyTypes {

 private final Logger logger = LoggerFactory.getLogger(EholdingsProxyTypesImpl.class);

 @Autowired
 private RMAPIConfigurationService configurationService;
 @Autowired
 private ProxyConverter converter;
 @Autowired
 private HeaderValidator headerValidator;

 public EholdingsProxyTypesImpl() {
  SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
 }
```

### Declaring Spring configuration
SpringContextUtil#init uses Spring configuration class. 

Detailed documentation on how to declare Spring configuration can be found here:
https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-java.

One of the approaches is to use @ComponentScan to automatically detect beans and add them to context:
```
@Configuration
@ComponentScan(basePackages = {
  "org.folio.rest.converter",
  "org.folio.rest.parser",
  "org.folio.rest.validator",
  "org.folio.http",
  "org.folio.config.impl",
  "org.folio.config.cache"})
public class ApplicationConfig {
  @Bean
 public PropertySourcesPlaceholderConfigurer placeholderConfigurer(){
    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setLocation(new ClassPathResource("application.properties"));
    return configurer;
  }
}
```
basePackages parameter specifies list of packages that will be searched. Any class in those packages that is annotated with @Component will be added to context.

PropertySourcesPlaceholderConfigurer class allows to use properties from file application.properties on classpath.

Example of bean:
```

@Component
public class RMAPIConfigurationCache {

  private Vertx vertx;
  private long expirationTime;

 @Autowired
 public RMAPIConfigurationCache(Vertx vertx, @Value("${configuration.cache.expire}") long expirationTime) {
    this.vertx = vertx;
    this.expirationTime = expirationTime;
  }

```
vertx and expirationTime parameters will be automatically injected by Spring, expirationTime will be set to the value of  "configuration.cache.expire" property from application.properties file.

## Additional information
Example of module that uses dependency injection support: 
https://github.com/folio-org/mod-kb-ebsco-java
* Other FOLIO Developer documentation is at [dev.folio.org](https://dev.folio.org/)
