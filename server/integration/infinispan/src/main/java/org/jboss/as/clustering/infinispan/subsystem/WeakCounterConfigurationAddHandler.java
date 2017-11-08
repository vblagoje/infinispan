/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.counter.api.CounterConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Add operation handler for /subsystem=infinispan/cache-container=clustered/counter=*
 * 
 * @author Vladimir Blagojevic
 * 
 */
public class WeakCounterConfigurationAddHandler extends CounterConfigurationAddHandler {

   @Override
   protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
         throws OperationFailedException {
      super.performRuntime(context, operation, model);
   }

   void populate(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
      super.populate(fromModel, toModel);
      for (AttributeDefinition attr : WeakCounterConfigurationResource.WEAK_ATTRIBUTES) {
         attr.validateAndSet(fromModel, toModel);
      }
   }

   /**
    * Implementation of abstract method processModelNode
    *
    */
   @Override
   void processModelNode(OperationContext context, String containerName, ModelNode counter,
         CounterConfiguration.Builder builder) throws OperationFailedException {
      super.processModelNode(context, containerName, counter, builder);

      Integer concurrency = WeakCounterConfigurationResource.CONCURRENCY.resolveModelAttribute(context, counter).asInt();
      builder.concurrencyLevel(concurrency);
   }
}
