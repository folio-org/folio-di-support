package org.folio.util;

import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class TestAutowireTarget {
  @Autowired
  private Context context;
  @Autowired
  private Vertx vertx;
  @Autowired(required = false)
  private TestBean testBean;

  public Context getContext() {
    return context;
  }

  public Vertx getVertx() {
    return vertx;
  }

  public TestBean getTestBean() {
    return testBean;
  }
}
