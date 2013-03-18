/**
 * Copyright (c) 2012-2012 Malhar, Inc.
 * All rights reserved.
 */
package com.malhartech.demos.mobile;

import java.net.URI;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.google.common.collect.Ranges;
import com.malhartech.api.ApplicationFactory;
import com.malhartech.api.Context.OperatorContext;
import com.malhartech.api.DAG;
import com.malhartech.api.Operator.OutputPort;
import com.malhartech.lib.io.ConsoleOutputOperator;
import com.malhartech.lib.io.HttpInputOperator;
import com.malhartech.lib.io.HttpOutputOperator;
import com.malhartech.lib.io.WebSocketInputOperator;
import com.malhartech.lib.testbench.RandomEventGenerator;

/**
 * Mobile Demo Application.<p>
 */
public class Application implements ApplicationFactory
{
  private static final Logger LOG = LoggerFactory.getLogger(Application.class);
  public static final String P_phoneRange = com.malhartech.demos.mobile.Application.class.getName() + ".phoneRange";
  private String ajaxServerAddr = null;
  private Range<Integer> phoneRange = Ranges.closed(9900000, 9999999);

  private void configure(Configuration conf)
  {

    this.ajaxServerAddr = System.getenv("MALHAR_AJAXSERVER_ADDRESS");
    LOG.debug(String.format("\n******************* Server address was %s", this.ajaxServerAddr));

    conf.set(DAG.STRAM_MAX_CONTAINERS.name(), "1");
    if (LAUNCHMODE_YARN.equals(conf.get(DAG.STRAM_LAUNCH_MODE))) {
      // settings only affect distributed mode
      conf.setIfUnset(DAG.STRAM_CONTAINER_MEMORY_MB.name(), "2048");
      conf.setIfUnset(DAG.STRAM_MASTER_MEMORY_MB.name(), "1024");
      conf.setIfUnset(DAG.STRAM_MAX_CONTAINERS.name(), "1");
    }
    else if (LAUNCHMODE_LOCAL.equals(conf.get(DAG.STRAM_LAUNCH_MODE))) {
    }

    String phoneRange = conf.get(P_phoneRange, null);
    if (phoneRange != null) {
      String[] tokens = phoneRange.split("-");
      if (tokens.length != 2) {
        throw new IllegalArgumentException("Invalid range: " + phoneRange);
      }
      this.phoneRange = Ranges.closed(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
    }
    System.out.println("Phone range: " + this.phoneRange);
  }


  @Override
  public DAG getApplication(Configuration conf)
  {
    configure(conf);
    DAG dag = new DAG(conf);
    dag.setAttribute(DAG.STRAM_APPNAME, "MobileDevApplication");
    dag.setAttribute(DAG.STRAM_DEBUG, true);

    RandomEventGenerator phones = dag.addOperator("phonegen", RandomEventGenerator.class);
    phones.setMinvalue(this.phoneRange.lowerEndpoint());
    phones.setMaxvalue(this.phoneRange.upperEndpoint());
    phones.setTuplesBlast(1000);
    phones.setTuplesBlastIntervalMillis(5);

    PhoneMovementGenerator movementGen = dag.addOperator("pmove", PhoneMovementGenerator.class);
    movementGen.setRange(20);
    movementGen.setThreshold(80);
    //dag.setAttribute(movementGen, OperatorContext.INITIAL_PARTITION_COUNT, 2);
    dag.setAttribute(movementGen, OperatorContext.PARTITION_TPS_MIN, 10000);
    dag.setAttribute(movementGen, OperatorContext.PARTITION_TPS_MAX, 50000);

    // default partitioning: first connected stream to movementGen will be partitioned
    dag.addStream("phonedata", phones.integer_data, movementGen.data).setInline(true);

    if (this.ajaxServerAddr != null) {
      HttpOutputOperator<Object> httpOut = dag.addOperator("phoneLocationQueryResult", new HttpOutputOperator<Object>());
      httpOut.setResourceURL(URI.create("http://" + this.ajaxServerAddr + "/channel/mobile/phoneLocationQueryResult"));

      dag.addStream("consoledata", movementGen.locationQueryResult, httpOut.input).setInline(true);

      OutputPort<Map<String, String>> queryPort;
      String daemonAddress = dag.getAttributes().attrValue(DAG.STRAM_DAEMON_ADDRESS, null);
      if (conf.getBoolean("demos.useWebSocket", false) && daemonAddress != null) {
        WebSocketInputOperator wsIn = dag.addOperator("phoneLocationQueryWS", new WebSocketInputOperator());
        LOG.info("WebSocket with daemon at: {}", daemonAddress);
        wsIn.setUrl(URI.create("ws://" + daemonAddress  + "/channel/mobile/phoneLocationQuery"));
        queryPort = wsIn.outputPort;
      } else {
        HttpInputOperator phoneLocationQuery = dag.addOperator("phoneLocationQuery", HttpInputOperator.class);
        phoneLocationQuery.setUrl(URI.create("http://" + ajaxServerAddr + "/channel/mobile/phoneLocationQuery"));
        queryPort = phoneLocationQuery.outputPort;
      }
      dag.addStream("query", queryPort, movementGen.locationQuery);
    }
    else {
      // for testing purposes without server
      movementGen.phone_register.put("q1", 9994995);
      movementGen.phone_register.put("q3", 9996101);
      ConsoleOutputOperator out = dag.addOperator("phoneLocationQueryResult", new ConsoleOutputOperator());
      out.setStringFormat("phoneLocationQueryResult" + ": %s");
      dag.addStream("consoledata", movementGen.locationQueryResult, out.input).setInline(true);
    }
    return dag;
  }
}
