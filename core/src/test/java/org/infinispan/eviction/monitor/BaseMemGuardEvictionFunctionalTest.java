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

import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.eviction.BaseEvictionFunctionalTest;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryMemoryGuardEvicted;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryMemoryGuardEvictionEvent;
import org.infinispan.notifications.cachelistener.event.Event;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "monitor.BaseMemGuardEvictionFunctionalTest")
public abstract class BaseMemGuardEvictionFunctionalTest extends BaseEvictionFunctionalTest {

   private long evictions = 0L;
   
   @Override
   protected void setup() throws Exception {
      super.setup();
      evictions = 0L;
   }

   @Override
   protected EmbeddedCacheManager createCacheManager() throws Exception {
      GlobalConfiguration global = new GlobalConfiguration();
      //Enable Memory Guard
      global.setMemoryGuardEnabled(true);
      //Set the threshold low to ensure that it gets invoked
      global.setMemoryGuardThreshold(10.0D);
      global.setMemoryGuardPollInterval(500);
      global.setMemoryGuardEvictionsPerCycle(250);
      Configuration cfg = new Configuration();
      cfg.setEvictionStrategy(getEvictionStrategy());
      cfg.setEvictionWakeUpInterval(100);
      cfg.setEvictionMaxEntries(128); // 128 max entries
      cfg.setUseLockStriping(false); // to minimize chances of deadlock in the unit test
      EmbeddedCacheManager cm = TestCacheManagerFactory.createCacheManager(global, cfg);
      cache = cm.getCache();
      cache.addListener(new EvictionListener());
      return cm;
   }

   @Listener
   public class EvictionListener {
      
      @CacheEntryEvicted
      public void nodeEvicted(CacheEntryEvictedEvent e){
         assert e.isPre() || !e.isPre();
         assert e.getKey() != null;
         assert e.getCache() != null;
         assert e.getType() == Event.Type.CACHE_ENTRY_EVICTED;         
      }
      @CacheEntryMemoryGuardEvicted
      public void nodeEvictedByMemoryGuard(CacheEntryMemoryGuardEvictionEvent e) {
         assert e.isPre() || !e.isPre();
         assert e.getKey() != null;
         assert e.getCache() != null;
         assert e.getType() == Event.Type.CACHE_ENTRY_MEMORY_GUARD_EVICTED;
      }
   }

}
