package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;

/**
 * CacheContainerCountersResource
 *
 * @author Vladimir Blagojevic
 * @since 9.2
 */
public class CacheContainerCountersResource extends SimpleResourceDefinition {
    static final PathElement PATH = PathElement.pathElement(ModelKeys.COUNTERS, ModelKeys.COUNTERS_NAME);

    private final boolean runtimeRegistration;
    private final ResolvePathHandler resolvePathHandler;

    CacheContainerCountersResource(ResolvePathHandler resolvePathHandler, boolean runtimeRegistration) {
        super(PATH, new InfinispanResourceDescriptionResolver(ModelKeys.CACHE_CONTAINER, ModelKeys.COUNTERS),
                CacheConfigOperationHandlers.CONTAINER_CONFIGURATIONS_ADD, ReloadRequiredRemoveStepHandler.INSTANCE);

        this.resolvePathHandler = resolvePathHandler;
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration rr) {
        PathElement strongCounterPath = PathElement.pathElement(ModelKeys.STRONG_COUNTER);
        PathElement weakCounterPath = PathElement.pathElement(ModelKeys.WEAK_COUNTER);
        rr.registerSubModel(new CounterResource(strongCounterPath, resolvePathHandler, runtimeRegistration));
        rr.registerSubModel(new CounterResource(weakCounterPath, resolvePathHandler, runtimeRegistration));
    }
}
