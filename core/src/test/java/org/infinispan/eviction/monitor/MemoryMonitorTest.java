/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.eviction.monitor;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

@Test(groups = "unit", testName = "monitor.MemoryMonitorTest")
public class MemoryMonitorTest {
   
   @Test
   public void testMemoryUsage() {
      MemoryMonitor monitor = MemoryMonitor.getInstance(80.0D, 200L);
      List<byte[]> list = new ArrayList<byte[]>();

      long previousVersion = monitor.getVersion();
      long previousValue = monitor.getUsedValue();
      for (int i = 0; i < 100; i++) {
         //Allocate byte array
         byte[] b = new byte[1024*1024*10];
         list.add(b);
         if (monitor.getVersion() > previousVersion && monitor.getUsedValue() > previousValue)
            break;
         if (monitor.isThresholdCrossed())
            break;
         
         previousVersion = monitor.getVersion();
         previousValue = monitor.getUsedValue();
      }
   }

}
