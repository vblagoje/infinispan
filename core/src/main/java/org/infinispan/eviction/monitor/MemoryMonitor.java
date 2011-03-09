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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * 
 * Object that monitors memory usage in the JVM to determine whether or not memory based eviction should occur. This object polls
 * the MemoryMXBEan due to the complexity of having many HEAP MemoryPoolMXBeans which may need to be monitored or listened to.
 * 
 * @author Dave Marion
 * @since 5.0
 */
public class MemoryMonitor {
   
   private class MemoryMonitorTask extends TimerTask {
      @Override
      public void run() {
         MemoryUsage usage = memoryMBean.getHeapMemoryUsage();
         //calculate the amount of committed memory that is used. We are using committed memory because it is 
         //guaranteed to be available to the VM.
         used = usage.getUsed();
         usedPercentage = (((double) used / (double) usage.getCommitted()) * 100.0);
         version.addAndGet(1);
         if (log.isDebugEnabled())
            log.debug("MemoryMonitor: used= " + used + ", committed= " + usage.getCommitted() + ", pct used = " + usedPercentage);
      }
   }
      
   private static final Log log = LogFactory.getLog(MemoryMonitor.class);
            
   private static MemoryMonitor monitor = null;
   
   private static Object lock = new Object();
   
   private MemoryMXBean memoryMBean = null;
   
   private Timer timer = null;
   
   private double threshold = 0D;
   
   private volatile double usedPercentage = 0D;
   
   private volatile long used = 0L;
   
   private volatile AtomicLong version = new AtomicLong(0);
      
   /**
    * 
    * Create a new MemoryMonitor.
    * 
    * @param threshold percentage of committed memory.
    * @param pollInterval ms between polling MemoryMXBean
    */
   private MemoryMonitor(double threshold, long pollInterval) {
      this.threshold = threshold;
      memoryMBean = ManagementFactory.getMemoryMXBean();
      timer = new Timer("MemoryMonitorThread", true);
      timer.scheduleAtFixedRate(new MemoryMonitorTask(), pollInterval, pollInterval);
   }
   
   public void stop() {
      timer.cancel();
      timer = null;
      memoryMBean = null;
   }
   
   public boolean isThresholdCrossed() {
      return (usedPercentage > threshold);
   }
   
   public long getUsedValue() {
      return used;
   }
   
   public long getVersion() {
      return version.get();
   }
   
   /**
    * 
    * Static method for retrieving the MemoryMonitor.
    * 
    * @param threshold 
    * @param pollInterval ms between memory usage checks
    * @return memory monitor object
    */
   public static MemoryMonitor getInstance(double threshold, long pollInterval) {
      if (null == monitor) {
         synchronized(lock) {
            //short circuit all waiters after the first has succeeded.
            if (null == monitor) {
               monitor = new MemoryMonitor(threshold, pollInterval);
            }
         }
      }
      return monitor;
   }
     
}
