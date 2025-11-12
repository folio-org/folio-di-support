# folio-di-support

[![FOLIO](https://img.shields.io/badge/FOLIO-Library-green)](https://www.folio.org/)
[![Release Version](https://img.shields.io/github/v/release/folio-org/folio-di-support?sort=semver&label=Latest%20Release)](https://github.com/folio-org/mod-quick-marc/releases)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.folio%3Afolio-di-support&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=org.folio%3Amod-quick-marc)
[![Java Version](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)

Copyright © 2018–2025 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Dependency injection support for FOLIO backend modules using Spring Framework and Vert.x.

<!-- TOC -->
* [folio-di-support](#folio-di-support)
  * [Introduction](#introduction)
  * [Overview](#overview)
    * [Creating a Spring Context](#creating-a-spring-context)
    * [Accessing the Spring Context from Endpoint Classes](#accessing-the-spring-context-from-endpoint-classes)
    * [Declaring Spring Configuration](#declaring-spring-configuration)
      * [Example Bean](#example-bean)
  * [Additional Information](#additional-information)
    * [Issue tracker](#issue-tracker)
    * [Code analysis](#code-analysis)
    * [Contributing](#contributing)
<!-- TOC -->

**Requirements:**
- Vert.x 5.0.x+
- Spring Framework 6.2.x+
- Java 21+

## Overview

### Creating a Spring Context
The `SpringContextUtil.init()` method initializes a Spring application context and adds it to the Vert.x `Context` object.

An [InitAPI hook](https://github.com/folio-org/raml-module-builder#adding-an-init-implementation) can be used to initialize the context during startup:

```java
public class InitAPIImpl implements InitAPI {
  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    vertx.executeBlocking(promise -> {
      SpringContextUtil.init(vertx, context, ApplicationConfig.class);
      promise.complete();
    }).onComplete(result -> {
      if (result.succeeded()) {
        handler.handle(Future.succeededFuture(true));
      } else {
        handler.handle(Future.failedFuture(result.cause()));
      }
    });
  }
}
```

### Accessing the Spring Context from Endpoint Classes

The `SpringContextUtil.autowireDependencies()` method retrieves the Spring context from the Vert.x context and uses it to inject beans into the target object.

Example of injecting Spring beans into an API class:
```java
public class EholdingsProxyTypesImpl implements EholdingsProxyTypes {

  @Autowired
  private RMAPIConfigurationService configurationService;
  @Autowired
  private ProxyConverter converter;
  @Autowired
  private HeaderValidator headerValidator;

  public EholdingsProxyTypesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }
}
```

### Declaring Spring Configuration

The `SpringContextUtil.init()` method requires a Spring configuration class.

For detailed documentation on declaring Spring configuration, see the [Spring Framework Reference](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-java).

One approach is to use `@ComponentScan` to automatically detect beans and add them to the context:
```java
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
  public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setLocation(new ClassPathResource("application.properties"));
    return configurer;
  }
}
```

The `basePackages` parameter specifies the list of packages to scan. Any class in those packages annotated with `@Component` will be added to the context.

The `PropertySourcesPlaceholderConfigurer` bean allows the use of properties from the `application.properties` file on the classpath.

#### Example Bean

```java
@Component
public class RMAPIConfigurationCache {

  private Vertx vertx;
  private long expirationTime;

  @Autowired
  public RMAPIConfigurationCache(Vertx vertx, @Value("${configuration.cache.expire}") long expirationTime) {
    this.vertx = vertx;
    this.expirationTime = expirationTime;
  }
}
```

The `vertx` and `expirationTime` parameters are automatically injected by Spring. The `expirationTime` value is set from the `configuration.cache.expire` property in the `application.properties` file.

## Additional Information

Example module using this dependency injection support: [mod-kb-ebsco-java](https://github.com/folio-org/mod-kb-ebsco-java)

For more FOLIO developer documentation, visit [dev.folio.org](https://dev.folio.org/)

### Issue tracker

See project [FDIS](https://folio-org.atlassian.net/browse/FDIS)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker/).

### Code analysis

[SonarQube analysis](https://sonarcloud.io/project/overview?id=org.folio:folio-di-support).

### Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.
