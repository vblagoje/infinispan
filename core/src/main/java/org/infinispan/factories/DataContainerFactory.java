/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.infinispan.factories;

import org.infinispan.config.ConfigurationException;
import org.infinispan.config.GlobalConfiguration.MemoryGuardType;
import org.infinispan.container.DataContainer;
import org.infinispan.container.DefaultDataContainer;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionThreadPolicy;
import org.infinispan.factories.annotations.DefaultFactoryFor;

/**
 * Constructs the data container
 * 
 * @author Manik Surtani (<a href="mailto:manik@jboss.org">manik@jboss.org</a>)
 * @author Vladimir Blagojevic
 * @author Dave Marion
 * @since 4.0
 */
@DefaultFactoryFor(classes = DataContainer.class)
public class DataContainerFactory extends AbstractNamedCacheComponentFactory implements
         AutoInstantiableFactory {

   @SuppressWarnings("unchecked")
   public <T> T construct(Class<T> componentType) {

      EvictionStrategy st = configuration.getEvictionStrategy();

      boolean found = false;
      for (EvictionStrategy strategy : EvictionStrategy.values()) {
         if (st.equals(strategy))
            found = true;
      }
      if (!found)
         throw new ConfigurationException("Unknown eviction strategy " + st);

      int level = configuration.getConcurrencyLevel();
      int maxEntries = configuration.getEvictionMaxEntries();
      EvictionThreadPolicy policy = configuration.getEvictionThreadPolicy();

      if (null == policy)
         policy = EvictionThreadPolicy.PIGGYBACK;

      MemoryGuardType memoryGuardConfiguration = configuration.getGlobalConfiguration().getMemoryGuardConfiguration();

      if (maxEntries < 0)
         return (T) DefaultDataContainer.unBoundedDataContainer(level, memoryGuardConfiguration);
      else
         return (T) DefaultDataContainer.boundedDataContainer(level, maxEntries, st, policy,
                  memoryGuardConfiguration);

   }
}
