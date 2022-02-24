package io.vertx.resourceadapter.examples.mdb;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BridgeSwitch {

  public static ConcurrentMap<String, String> bridges = new ConcurrentHashMap<>();

  class BridgeInfo {
    public String jti;
    public String bridgeId;
  }
}
