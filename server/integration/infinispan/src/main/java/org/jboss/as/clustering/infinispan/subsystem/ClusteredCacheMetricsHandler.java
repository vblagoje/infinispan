package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.stats.ClusterCacheStats;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 * Handler which manages read-only access to clustered cache runtime information (metrics)
 *
 * @see ClusterCacheStats
 * @author Vladimir Blagojevic
 */
public class ClusteredCacheMetricsHandler extends AbstractRuntimeOnlyHandler {
   public static final ClusteredCacheMetricsHandler INSTANCE = new ClusteredCacheMetricsHandler();

   public enum ClusteredCacheMetrics {

      NUMBER_OF_LOCKS_AVAILABLE(MetricKeys.NUMBER_OF_LOCKS_AVAILABLE, ModelType.INT, true),
      NUMBER_OF_LOCKS_HELD(MetricKeys.NUMBER_OF_LOCKS_HELD, ModelType.INT, true),

      AVERAGE_READ_TIME(MetricKeys.AVERAGE_READ_TIME, ModelType.LONG, true),
      AVERAGE_WRITE_TIME(MetricKeys.AVERAGE_WRITE_TIME, ModelType.LONG, true),
      AVERAGE_REMOVE_TIME(MetricKeys.AVERAGE_REMOVE_TIME, ModelType.LONG, true),
      ELAPSED_TIME(MetricKeys.ELAPSED_TIME, ModelType.LONG, true),
      EVICTIONS(MetricKeys.EVICTIONS, ModelType.LONG, true),
      HIT_RATIO(MetricKeys.HIT_RATIO, ModelType.DOUBLE, true),
      HITS(MetricKeys.HITS, ModelType.LONG, true),
      MISSES(MetricKeys.MISSES, ModelType.LONG, true),
      NUMBER_OF_ENTRIES(MetricKeys.NUMBER_OF_ENTRIES, ModelType.INT, true),
      READ_WRITE_RATIO(MetricKeys.READ_WRITE_RATIO,ModelType.DOUBLE, true),
      REMOVE_HITS(MetricKeys.REMOVE_HITS, ModelType.LONG, true),
      REMOVE_MISSES(MetricKeys.REMOVE_MISSES, ModelType.LONG, true),
      STORES(MetricKeys.STORES, ModelType.LONG, true), TIME_SINCE_RESET(MetricKeys.TIME_SINCE_RESET, ModelType.LONG, true),

      INVALIDATIONS(MetricKeys.INVALIDATIONS, ModelType.LONG, true),
      PASSIVATIONS(MetricKeys.PASSIVATIONS, ModelType.STRING, true),

      ACTIVATIONS(MetricKeys.ACTIVATIONS, ModelType.STRING, true),
      CACHE_LOADER_LOADS(MetricKeys.CACHE_LOADER_LOADS, ModelType.LONG, true),
      CACHE_LOADER_MISSES(MetricKeys.CACHE_LOADER_MISSES, ModelType.LONG, true),
      CACHE_LOADER_STORES(MetricKeys.CACHE_LOADER_STORES, ModelType.LONG, true);

      private static final Map<String, ClusteredCacheMetrics> MAP = new HashMap<String, ClusteredCacheMetrics>();

      static {
         for (ClusteredCacheMetrics metric : ClusteredCacheMetrics.values()) {
            MAP.put(metric.toString(), metric);
         }
      }

      final AttributeDefinition definition;
      final boolean clustered;

      private ClusteredCacheMetrics(final AttributeDefinition definition, final boolean clustered) {
         this.definition = definition;
         this.clustered = clustered;
      }

      private ClusteredCacheMetrics(String attributeName, ModelType type, boolean allowNull) {
         this(new SimpleAttributeDefinitionBuilder(attributeName, type, allowNull).setStorageRuntime().build(), true);
      }

      @Override
      public final String toString() {
         return definition.getName();
      }

      public static ClusteredCacheMetrics getStat(final String stringForm) {
         return MAP.get(stringForm);
      }
   }

   /*
    * Two constraints need to be dealt with here: 1. There may be no started cache instance
    * available to interrogate. Because of lazy deployment, a cache instance is only started upon
    * deployment of an application which uses that cache instance. 2. The attribute name passed in
    * may not correspond to a defined metric
    *
    * Read-only attributes have no easy way to throw an exception without negatively impacting other
    * parts of the system. Therefore in such cases, as message will be logged and a ModelNode of
    * undefined will be returned.
    */
   @Override
   protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
      final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
      final String cacheContainerName = address.getElement(address.size() - 2).getValue();
      final String cacheName = address.getLastElement().getValue();
      final String attrName = operation.require(NAME).asString();
      final ServiceController<?> controller = context.getServiceRegistry(false).getService(
            CacheService.getServiceName(cacheContainerName, cacheName));
      Cache<?, ?> cache = (Cache<?, ?>) controller.getValue();
      ClusteredCacheMetrics metric = ClusteredCacheMetrics.getStat(attrName);
      ModelNode result = new ModelNode();

      if (metric == null) {
         context.getFailureDescription().set(String.format("Unknown metric %s", attrName));
      } else if (cache == null) {
         context.getFailureDescription().set(String.format("Unavailable cache %s", attrName));
      } else {
         AdvancedCache<?, ?> aCache = cache.getAdvancedCache();
         ClusterCacheStats clusterCacheStats = aCache.getComponentRegistry().getComponent(ClusterCacheStats.class);
         switch (metric) {
         case NUMBER_OF_LOCKS_AVAILABLE:
            result.set(clusterCacheStats.getNumberOfLocksAvailable());
            break;
         case NUMBER_OF_LOCKS_HELD:
            result.set(clusterCacheStats.getNumberOfLocksHeld());
            break;
         case AVERAGE_READ_TIME: {
            result.set(clusterCacheStats.getAverageReadTime());
            break;
         }
         case AVERAGE_WRITE_TIME: {
            result.set(clusterCacheStats.getAverageWriteTime());
            break;
         }
         case AVERAGE_REMOVE_TIME: {
            result.set(clusterCacheStats.getAverageRemoveTime());
            break;
         }
         case EVICTIONS: {
            result.set(clusterCacheStats.getEvictions());
            break;
         }
         case HIT_RATIO: {
            long hits = clusterCacheStats.getHits();
            long total = clusterCacheStats.getMisses() + hits;
            if (total > 0) {
               result.set(hits / total);
            } else {
               result.set(0);
            }
            break;
         }
         case HITS: {
            result.set(clusterCacheStats.getHits());
            break;
         }
         case MISSES: {
            result.set(clusterCacheStats.getMisses());
            break;
         }
         case NUMBER_OF_ENTRIES: {
            result.set(clusterCacheStats.getCurrentNumberOfEntries());
            break;
         }
         case READ_WRITE_RATIO: {
            long stores = clusterCacheStats.getStores();
            if (stores > 0){
               result.set(clusterCacheStats.getHits() + clusterCacheStats.getMisses() / stores);
            } else {
               result.set(0);
            }
            break;
         }
         case REMOVE_HITS: {
            result.set(clusterCacheStats.getRemoveHits());
            break;
         }
         case REMOVE_MISSES: {
            result.set(clusterCacheStats.getRemoveMisses());
            break;
         }
         case STORES: {
            result.set(clusterCacheStats.getStores());
            break;
         }
         case INVALIDATIONS: {
            result.set(clusterCacheStats.getInvalidations());
            break;
         }
         case PASSIVATIONS: {
            result.set(clusterCacheStats.getPassivations());
            break;
         }
         case ACTIVATIONS: {
            result.set(clusterCacheStats.getActivations());
            break;
         }
         case CACHE_LOADER_LOADS: {
            result.set(clusterCacheStats.getCacheLoaderLoads());
            break;
         }
         case CACHE_LOADER_MISSES: {
            result.set(clusterCacheStats.getCacheLoaderMisses());
            break;
         }
         case CACHE_LOADER_STORES: {
            result.set(clusterCacheStats.getStoreWrites());
            break;
         }
         default: {
            context.getFailureDescription().set(String.format("Unknown metric %s", metric));
            break;
         }
         }
         context.getResult().set(result);
      }
      context.stepCompleted();
   }

   public void registerClusteredMetrics(ManagementResourceRegistration container) {
      for (ClusteredCacheMetrics metric : ClusteredCacheMetrics.values()) {
         if (metric.clustered) {
            container.registerMetric(metric.definition, this);
         }
      }
   }
}
