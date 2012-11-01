/**
 * Copyright (c) 2012-2012 Malhar, Inc. All rights reserved.
 */
package com.malhartech.lib.math;

import com.malhartech.api.Context.OperatorContext;
import com.malhartech.api.Sink;
import com.malhartech.dag.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Functional tests for {@link com.malhartech.lib.math.Sum}<p>
 *
 */
public class SumTest
{
  private static Logger LOG = LoggerFactory.getLogger(Sum.class);

  class TestSink implements Sink
  {
    List<Object> collectedTuples = new ArrayList<Object>();

    @Override
    public void process(Object payload)
    {
      if (payload instanceof Tuple) {
      }
      else {
        collectedTuples.add(payload);
      }
    }
  }

  /**
   * Test oper logic emits correct results
   */
  @Test
  public void testNodeProcessing()
  {
    testNodeSchemaProcessing(true, false);
    testNodeSchemaProcessing(true, true);
    testNodeSchemaProcessing(false, true);
  }

  public void testNodeSchemaProcessing(boolean sum, boolean count)
  {

    Sum<String, Double> oper = new Sum<String, Double>();
    oper.setType(Double.class);
    TestSink sumSink = new TestSink();
    TestSink countSink = new TestSink();
    TestSink averageSink = new TestSink();
    if (sum) {
      oper.sum.setSink(sumSink);
      oper.average.setSink(averageSink);
    }
    if (count) {
      oper.count.setSink(countSink);
    }


    // Not needed, but still setup is being called as a matter of discipline
    oper.setup(new com.malhartech.dag.OperatorContext("irrelevant", null));
    oper.beginWindow(); //

    HashMap<String, Double> input = new HashMap<String, Double>();

    input.put("a", 2.0);
    input.put("b", 20.0);
    input.put("c", 1000.0);
    oper.data.process(input);
    input.clear();
    input.put("a", 1.0);
    oper.data.process(input);
    input.clear();
    input.put("a", 10.0);
    input.put("b", 5.0);
    oper.data.process(input);
    input.clear();
    input.put("d", 55.0);
    input.put("b", 12.0);
    oper.data.process(input);
    input.clear();
    input.put("d", 22.0);
    oper.data.process(input);
    input.clear();
    input.put("d", 14.2);
    oper.data.process(input);
    input.clear();

    // Mix integers and doubles
    HashMap<String, Double> inputi = new HashMap<String, Double>();
    inputi.put("d", 46.0);
    inputi.put("e", 2.0);
    oper.data.process(inputi);
    inputi.clear();
    inputi.put("a", 23.0);
    inputi.put("d", 4.0);
    oper.data.process(inputi);
    inputi.clear();

    oper.endWindow(); //

    if (sum) {
      // payload should be 1 bag of tuples with keys "a", "b", "c", "d", "e"
      Assert.assertEquals("number emitted tuples", 1, sumSink.collectedTuples.size());
      for (Object o: sumSink.collectedTuples) {
        HashMap<String, Object> output = (HashMap<String, Object>)o;
        for (Map.Entry<String, Object> e: output.entrySet()) {
          Double val = (Double)e.getValue();
          if (e.getKey().equals("a")) {
            Assert.assertEquals("emitted value for 'a' was ", new Double(36), val);
          }
          else if (e.getKey().equals("b")) {
            Assert.assertEquals("emitted tuple for 'b' was ", new Double(37), val);
          }
          else if (e.getKey().equals("c")) {
            Assert.assertEquals("emitted tuple for 'c' was ", new Double(1000), val);
          }
          else if (e.getKey().equals("d")) {
            Assert.assertEquals("emitted tuple for 'd' was ", new Double(141.2), val);
          }
          else if (e.getKey().equals("e")) {
            Assert.assertEquals("emitted tuple for 'e' was ", new Double(2), val);
          }
        }
      }

      Assert.assertEquals("number emitted tuples", 1, averageSink.collectedTuples.size());
      for (Object o: averageSink.collectedTuples) {
        HashMap<String, Object> output = (HashMap<String, Object>)o;
        for (Map.Entry<String, Object> e: output.entrySet()) {
          Double val = (Double)e.getValue();
          if (e.getKey().equals("a")) {
            Assert.assertEquals("emitted value for 'a' was ", new Double(36/4.0), val);
          }
          else if (e.getKey().equals("b")) {
            Assert.assertEquals("emitted tuple for 'b' was ", new Double(37/3.0), val);
          }
          else if (e.getKey().equals("c")) {
            Assert.assertEquals("emitted tuple for 'c' was ", new Double(1000/1.0), val);
          }
          else if (e.getKey().equals("d")) {
            Assert.assertEquals("emitted tuple for 'd' was ", new Double(141.2/5), val);
          }
          else if (e.getKey().equals("e")) {
            Assert.assertEquals("emitted tuple for 'e' was ", new Double(2/1.0), val);
          }
        }
      }
    }
    if (count) {
      // payload should be 1 bag of tuples with keys "a", "b", "c", "d", "e"
      Assert.assertEquals("number emitted tuples", 1, countSink.collectedTuples.size());
      for (Object o: countSink.collectedTuples) {
        HashMap<String, Object> output = (HashMap<String, Object>)o;
        for (Map.Entry<String, Object> e: output.entrySet()) {
          Integer val = (Integer)e.getValue();
          if (e.getKey().equals("a")) {
            Assert.assertEquals("emitted value for 'a' was ", 4, val.intValue());
          }
          else if (e.getKey().equals("b")) {
            Assert.assertEquals("emitted tuple for 'b' was ", 3, val.intValue());
          }
          else if (e.getKey().equals("c")) {
            Assert.assertEquals("emitted tuple for 'c' was ", 1, val.intValue());
          }
          else if (e.getKey().equals("d")) {
            Assert.assertEquals("emitted tuple for 'd' was ", 5, val.intValue());
          }
          else if (e.getKey().equals("e")) {
            Assert.assertEquals("emitted tuple for 'e' was ", 1, val.intValue());
          }
        }
      }
    }

  }
}
