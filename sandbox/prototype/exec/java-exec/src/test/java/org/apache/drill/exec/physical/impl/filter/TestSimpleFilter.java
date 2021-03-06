package org.apache.drill.exec.physical.impl.filter;

import static org.junit.Assert.*;
import mockit.Injectable;
import mockit.NonStrictExpectations;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.util.FileUtils;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.impl.ImplCreator;
import org.apache.drill.exec.physical.impl.SimpleRootExec;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.rpc.user.UserServer.UserClientConnection;
import org.apache.drill.exec.server.DrillbitContext;
import org.junit.After;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.yammer.metrics.MetricRegistry;

public class TestSimpleFilter {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestSimpleFilter.class);
  DrillConfig c = DrillConfig.create();
  
  
  @Test
  public void testFilter(@Injectable final DrillbitContext bitContext, @Injectable UserClientConnection connection) throws Throwable{
//    System.out.println(System.getProperty("java.class.path"));

    
    new NonStrictExpectations(){{
      bitContext.getMetrics(); result = new MetricRegistry("test");
      bitContext.getAllocator(); result = BufferAllocator.getAllocator(c);
    }};
    
    
    PhysicalPlanReader reader = new PhysicalPlanReader(c, c.getMapper(), CoordinationProtos.DrillbitEndpoint.getDefaultInstance());
    PhysicalPlan plan = reader.readPhysicalPlan(Files.toString(FileUtils.getResourceAsFile("/filter/test1.json"), Charsets.UTF_8));
    FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    FragmentContext context = new FragmentContext(bitContext, FragmentHandle.getDefaultInstance(), connection, null, registry);
    SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));
    while(exec.next()){
      assertEquals(50, exec.getSelectionVector2().getCount());
    }
    
    if(context.getFailureCause() != null){
      throw context.getFailureCause();
    }
    assertTrue(!context.isFailed());

  }
  
  @After
  public void tearDown() throws Exception{
    // pause to get logger to catch up.
    Thread.sleep(1000);
  }
}
