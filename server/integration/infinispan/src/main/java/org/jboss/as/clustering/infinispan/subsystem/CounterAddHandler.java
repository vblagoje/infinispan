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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.server.infinispan.spi.service.CacheContainerServiceName;
import org.jboss.as.clustering.infinispan.DefaultCacheContainer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Add operation handler for /subsystem=infinispan/cache-container=clustered/counter=*
 *
 * @author Vladimir Blagojevic
 *
 */
public class CounterAddHandler extends AbstractAddStepHandler {

    CounterAddHandler() {
        super();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        String counterName = getCounterName(operation);
        String containerName = getContainerName(operation);
        String counterType = getCounterType(operation);

        String configurationName = operation.get(CounterResource.CONFIGURATION.getName()).asString();
        boolean weakCounter = ModelKeys.WEAK_COUNTER.equals(counterType);

        ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        ServiceName serviceName = CacheContainerServiceName.CACHE_CONTAINER.getServiceName(containerName);

        final ServiceController<?> controller = serviceRegistry.getService(serviceName);
        DefaultCacheContainer cacheManager = (DefaultCacheContainer) controller.getValue();
        CounterManager counterManager = EmbeddedCounterManagerFactory.asCounterManager(cacheManager);
        CounterConfiguration configuration = counterManager.getConfiguration(configurationName);
        if (configuration == null) {
            throw new OperationFailedException("Counter configuration " + configurationName + " is not defined");
        }

        counterManager.defineCounter(counterName, configuration);

        // we could have invalid configuration or use wrong configuration for weak/strong counter
        //wrap with OperationFailedException
        try {
            if (weakCounter) {
                counterManager.getWeakCounter(counterName);
            } else {
                counterManager.getStrongCounter(counterName);
            }
        } catch (Exception e) {
            throw new OperationFailedException("Could not create counter " + counterName, e);
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        this.populate(operation, model);
    }

    /**
     * Transfer elements common to both operations and models
     *
     * @param fromModel
     * @param toModel
     */
    void populate(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
        for (AttributeDefinition attr : CounterResource.ATTRIBUTES) {
            attr.validateAndSet(fromModel, toModel);
        }
    }

    private String getCounterName(ModelNode operation) {
        PathAddress counterAddress = getCounterAddressFromOperation(operation);
        return counterAddress.getLastElement().getValue();
    }

    private String getContainerName(ModelNode operation) {
        PathAddress containerAddress = getCacheContainerAddressFromOperation(operation);
        return containerAddress.getLastElement().getValue();
    }

    private PathAddress getCacheContainerAddressFromOperation(ModelNode operation) {
        PathAddress counterAddress = getCounterAddressFromOperation(operation);
        return counterAddress.subAddress(0, counterAddress.size() - 2);
    }

    private String getCounterType(ModelNode operation) {
        PathAddress counterAddress = getCounterAddressFromOperation(operation);
        int size = counterAddress.size();
        PathAddress subAddress = counterAddress.subAddress(size - 1, size);
        return subAddress.getLastElement().getKey();
    }

    private PathAddress getCounterAddressFromOperation(ModelNode operation) {
        return PathAddress.pathAddress(operation.get(OP_ADDR));
    }
}
