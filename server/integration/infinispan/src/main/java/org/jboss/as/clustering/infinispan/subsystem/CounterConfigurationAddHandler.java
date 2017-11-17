package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.ArrayList;
import java.util.Collection;

import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterConfiguration.Builder;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.Storage;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.infinispan.spi.service.CacheContainerServiceName;
import org.infinispan.server.infinispan.spi.service.CounterServiceName;
import org.jboss.as.clustering.infinispan.DefaultCacheContainer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * Add operation handler for /subsystem=infinispan/cache-container=clustered/counter=*
 *
 * @author Vladimir Blagojevic
 *
 */
public class CounterConfigurationAddHandler extends AbstractAddStepHandler implements  RestartableServiceHandler{

    CounterConfigurationAddHandler() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        super.performRuntime(context, operation, model);

        this.installRuntimeServices(context, operation, model, null);
    }

    @Override
    public Collection<ServiceController<?>> installRuntimeServices(OperationContext context, ModelNode operation,
            ModelNode containerModel, ModelNode cacheModel) throws OperationFailedException {

        String counterName = getCounterName(operation);
        String containerName = getContainerName(operation);
        String counterType = getCounterType(operation);

        Builder b = getBuilder(context, containerModel, counterType);
        processModelNode(context, containerName, containerModel, b);

        // define configuration
        ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        ServiceName serviceName = CacheContainerServiceName.CACHE_CONTAINER.getServiceName(containerName);
        ServiceController<?> controller = serviceRegistry.getService(serviceName);
        DefaultCacheContainer cacheManager = (DefaultCacheContainer) controller.getValue();

        Collection<ServiceController<?>> controllers = new ArrayList<>(2);
        if (cacheManager != null) {
            CounterManager counterManager = EmbeddedCounterManagerFactory.asCounterManager(cacheManager);
            CounterConfiguration configuration = b.build();
            counterManager.defineCounter(counterName, configuration);
        } else {
            ServiceController<?> service = this.installCounterConfigurationService(context.getServiceTarget(), containerName, counterName, b.build());
            controllers.add(service);
        }
        return controllers;
    }

    @Override
    public void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode containerModel,
            ModelNode cacheModel) throws OperationFailedException {
        // TODO Auto-generated method stub

    }

    private ServiceController<?> installCounterConfigurationService(ServiceTarget target, String containerName,
            String configurationName, CounterConfiguration configuration) {
        final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
        final CounterConfigurationDependencies dependencies = new CounterConfigurationDependencies(container);
        final Service<?> service = new CounterConfigurationService(configuration, configurationName, dependencies);
        final ServiceBuilder<?> builder = target.addService(CounterServiceName.CONFIGURATION.getServiceName(containerName, configurationName), service)
                .addDependency(CacheContainerServiceName.CACHE_CONTAINER.getServiceName(containerName), EmbeddedCacheManager.class, container);
        return builder.install();
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        this.populate(operation, model);
    }

    void populate(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
        for (AttributeDefinition attr : CounterConfigurationResource.ATTRIBUTES) {
            attr.validateAndSet(fromModel, toModel);
        }
    }

    private String getContainerName(ModelNode operation) {
        PathAddress counterAddress = getCounterAddressFromOperation(operation);
        PathAddress pathAddress = counterAddress.subAddress(0, counterAddress.size() - 3);
        return pathAddress.getLastElement().getValue();
    }

    private String getCounterType(ModelNode operation) {
        PathAddress counterAddress = getCounterAddressFromOperation(operation);
        int size = counterAddress.size();
        PathAddress subAddress = counterAddress.subAddress(size - 1, size);
        return subAddress.getLastElement().getKey();
    }

    private String getCounterName(ModelNode operation) {
        PathAddress counterAddress = getCounterAddressFromOperation(operation);
        int size = counterAddress.size();
        PathAddress subAddress = counterAddress.subAddress(size - 1, size);
        return subAddress.getLastElement().getValue();
    }

    private PathAddress getCounterAddressFromOperation(ModelNode operation) {
        return PathAddress.pathAddress(operation.get(OP_ADDR));
    }

    private Builder getBuilder(OperationContext context, ModelNode counter, String counterType)
            throws OperationFailedException {
        boolean isWeakCounter = ModelKeys.WEAK_COUNTER.equals(counterType);
        if (isWeakCounter) {
            return CounterConfiguration.builder(CounterType.WEAK);
        } else {
            ModelNode lowerBoundModel = counter.get(ModelKeys.LOWER_BOUND);
            ModelNode upperBoundModel = counter.get(ModelKeys.UPPER_BOUND);
            boolean isBounded = lowerBoundModel.isDefined() || upperBoundModel.isDefined();
            return isBounded ? CounterConfiguration.builder(CounterType.BOUNDED_STRONG)
                    : CounterConfiguration.builder(CounterType.UNBOUNDED_STRONG);
        }
    }

    void processModelNode(OperationContext context, String containerName, ModelNode counter,
            CounterConfiguration.Builder builder) throws OperationFailedException {

        long initialValue = CounterConfigurationResource.INITIAL_VALUE.resolveModelAttribute(context, counter).asLong();
        String storageType = CounterConfigurationResource.STORAGE.resolveModelAttribute(context, counter).asString();

        builder.initialValue(initialValue);
        builder.storage(Storage.valueOf(storageType));
    }

    private static class CounterConfigurationDependencies implements CounterConfigurationService.Dependencies {

        private InjectedValue<EmbeddedCacheManager> container;
        public CounterConfigurationDependencies(InjectedValue<EmbeddedCacheManager> container) {
           this.container = container;
        }

        @Override
        public EmbeddedCacheManager getCacheContainer() {
            return this.container.getValue();
        }

    }
}
