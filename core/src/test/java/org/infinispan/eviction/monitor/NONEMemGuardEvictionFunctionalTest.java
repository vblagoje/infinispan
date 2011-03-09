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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.infinispan.eviction.EvictionStrategy;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "monitor.NONEMemGuardEvictionFunctionalTest")
public class NONEMemGuardEvictionFunctionalTest extends BaseMemGuardEvictionFunctionalTest {

   @Override
   protected EvictionStrategy getEvictionStrategy() {
      return EvictionStrategy.NONE;
   }
   
   @Override
   public void testSimpleEvictionMaxEntries() {
      for (int i = 0; i < 512; i++) {
         cache.put("key-" + (i + 1), "value-" + (i + 1), 1, TimeUnit.MINUTES);
      }
      //No eviction should take place, there should be 512 entries unless they
      //were evicted by the memory guard. How many were evicted by the memory guard
      long memoryGuardEvictions = cache.getAdvancedCache().getDataContainer().getMemoryGuardEvictions();
      assert cache.size() == (512 - memoryGuardEvictions) : "cache size not correct: " + cache.size() + " should be " + (512 - memoryGuardEvictions);
   }

   @Override
   public void testSimpleExpirationMaxIdle() throws Exception {

      for (int i = 0; i < 512; i++) {
         cache.put("key-" + (i + 1), "value-" + (i + 1), 1, TimeUnit.MILLISECONDS);
      }
      //No eviction should take place, there should be 512 entries unless they
      //were evicted by the memory guard. How many were evicted by the memory guard
      long memoryGuardEvictions = cache.getAdvancedCache().getDataContainer().getMemoryGuardEvictions();
      assert cache.size() == (512 - memoryGuardEvictions) : "cache size not correct: " + cache.size() + " should be " + (512 - memoryGuardEvictions);
   }

   @Override
   public void testMultiThreaded() throws InterruptedException {
      int NUM_THREADS = 20;
      Writer[] w = new Writer[NUM_THREADS];
      CountDownLatch startLatch = new CountDownLatch(1);

      for (int i = 0; i < NUM_THREADS; i++) w[i] = new Writer(i, startLatch);
      for (Writer writer : w) writer.start();

      startLatch.countDown();

      Thread.sleep(250);

      // now stop writers
      for (Writer writer : w) writer.running = false;

      //Wait for threads to end
      Thread.sleep(10000);
      //How many entries were written?
      long written = 0L;
      for (Writer writer : w) written += writer.getCount();
      
      for (Writer writer : w) writer.join();

      //The writer threads are putting entries in the cache, but the only eviction that should take
      //place is from the MemoryGuard. How many entries did the memory guard evict?
      long memoryGuardEvictions = cache.getAdvancedCache().getDataContainer().getMemoryGuardEvictions();
      // Check to see if the sizes add up
      assert cache.getAdvancedCache().getDataContainer().size() == (written - memoryGuardEvictions) : "Expected "+ (written - memoryGuardEvictions) +", was " + cache.size();      
   }


}
