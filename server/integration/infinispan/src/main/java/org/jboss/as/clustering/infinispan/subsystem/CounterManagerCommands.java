package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Optional;
import java.util.Properties;

import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.PropertyFormatter;
import org.infinispan.counter.api.Storage;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.infinispan.SecurityActions;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 8.0
 */
public final class CounterManagerCommands {

   private static final AttributeDefinition COUNTER_NAME = SimpleAttributeDefinitionBuilder
         .create("name", ModelType.STRING, false)
         .build();
   private static final AttributeDefinition TYPE = SimpleAttributeDefinitionBuilder
         .create("type", ModelType.STRING, false)
         .setAllowedValues(
               CounterType.WEAK.toString(),
               CounterType.BOUNDED_STRONG.toString(),
               CounterType.UNBOUNDED_STRONG.toString())
         .build();
   private static final AttributeDefinition STORAGE = SimpleAttributeDefinitionBuilder
         .create("storage", ModelType.STRING, true)
         .setAllowedValues(
               Storage.VOLATILE.toString(),
               Storage.PERSISTENT.toString())
         .build();
   private static final AttributeDefinition INITIAL_VALUE = SimpleAttributeDefinitionBuilder
         .create("initial-value", ModelType.LONG, true)
         .build();
   private static final AttributeDefinition LOWER_BOUND = SimpleAttributeDefinitionBuilder
         .create("lower-bound", ModelType.LONG, true)
         .build();
   private static final AttributeDefinition UPPER_BOUND = SimpleAttributeDefinitionBuilder
         .create("upper-bound", ModelType.LONG, true)
         .build();
   private static final AttributeDefinition CONCURRENCY = SimpleAttributeDefinitionBuilder
         .create("concurrency", ModelType.INT, true)
         .build();
   private static final OperationDefinition COUNTER_LIST = buildOperation("counter-list")
         .setReadOnly()
         .build();
   private static final OperationDefinition COUNTER_ADD = buildOperation("counter-add")
         .setParameters(COUNTER_NAME, TYPE, STORAGE, INITIAL_VALUE, LOWER_BOUND, UPPER_BOUND, CONCURRENCY)
         .build();
   private static final OperationDefinition COUNTER_REMOVE = buildOperation("counter-remove")
         .setParameters(COUNTER_NAME)
         .build();

   private static final OperationDefinition COUNTER_CONFIGURATION = buildOperation("counter-configuration")
         .setParameters(COUNTER_NAME)
         .build();

   private CounterManagerCommands() {
   }

   private static SimpleOperationDefinitionBuilder buildOperation(String name) {
      return new SimpleOperationDefinitionBuilder(name,
            new InfinispanResourceDescriptionResolver(ModelKeys.CACHE_CONTAINER))
            .setRuntimeOnly();
   }

   public static void register(ManagementResourceRegistration resourceRegistration) {
      resourceRegistration.registerOperationHandler(COUNTER_LIST, CounterListCommand.INSTANCE);
      resourceRegistration.registerOperationHandler(COUNTER_ADD, CounterAddCommand.INSTANCE);
      resourceRegistration.registerOperationHandler(COUNTER_REMOVE, CounterRemoveCommand.INSTANCE);
      resourceRegistration.registerOperationHandler(COUNTER_CONFIGURATION, CounterConfigurationCommand.INSTANCE);
   }

   private static String counterName(OperationContext context, ModelNode operation) throws OperationFailedException {
      return COUNTER_NAME.resolveModelAttribute(context, operation).asString();
   }

   private static CounterType counterType(OperationContext context, ModelNode operation)
         throws OperationFailedException {
      String typeAsString = TYPE.resolveModelAttribute(context, operation).asString();
      return CounterType.valueOf(typeAsString);
   }

   private static Optional<Storage> findStorage(OperationContext context, ModelNode operation)
         throws OperationFailedException {
      ModelNode storage = STORAGE.resolveModelAttribute(context, operation);
      return storage.isDefined() ?
             Optional.of(Storage.valueOf(storage.asString())) :
             Optional.empty();
   }

   private static Optional<Long> findInitialValue(OperationContext context, ModelNode operation)
         throws OperationFailedException {
      return findLong(INITIAL_VALUE, context, operation);
   }

   private static Optional<Long> findLowerBound(OperationContext context, ModelNode operation)
         throws OperationFailedException {
      return findLong(LOWER_BOUND, context, operation);
   }

   private static Optional<Long> findUpperBound(OperationContext context, ModelNode operation)
         throws OperationFailedException {
      return findLong(LOWER_BOUND, context, operation);
   }

   private static Optional<Integer> findConcurrency(OperationContext context, ModelNode operation)
         throws OperationFailedException {
      ModelNode anInteger = CONCURRENCY.resolveModelAttribute(context, operation);
      return anInteger.isDefined() ?
             Optional.of(anInteger.asInt()) :
             Optional.empty();
   }

   private static Optional<Long> findLong(AttributeDefinition definition, OperationContext context, ModelNode operation)
         throws OperationFailedException {
      ModelNode aLong = definition.resolveModelAttribute(context, operation);
      return aLong.isDefined() ?
             Optional.of(aLong.asLong()) :
             Optional.empty();
   }

   private static OperationFailedException counterManagerNotFound() {
      return new OperationFailedException("CounterManager not found in server.");
   }

   private static OperationFailedException counterNotFound(String name) {
      return new OperationFailedException("Counter '" + name + "' not defined.");
   }

   private static class CounterListCommand extends BaseCounterManagerCommand {
      private static final CounterListCommand INSTANCE = new CounterListCommand();

      private CounterListCommand() {
         super();
      }

      @Override
      protected ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
            throws Exception {
         final ModelNode result = new ModelNode().setEmptyList();
         counterManager.getCountersName().forEach(result::add);
         return result;
      }
   }

   private static class CounterAddCommand extends BaseCounterManagerCommand {
      private static final CounterAddCommand INSTANCE = new CounterAddCommand();

      private CounterAddCommand() {
         super();
      }

      @Override
      protected ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
            throws Exception {
         final String counterName = counterName(context, operation);
         final ModelNode result = new ModelNode();
         final CounterConfiguration.Builder builder = CounterConfiguration.builder(counterType(context, operation));
         findStorage(context, operation).ifPresent(builder::storage);
         findInitialValue(context, operation).ifPresent(builder::initialValue);
         findLowerBound(context, operation).ifPresent(builder::lowerBound);
         findUpperBound(context, operation).ifPresent(builder::upperBound);
         findConcurrency(context, operation).ifPresent(builder::concurrencyLevel);
         result.set(counterManager.defineCounter(counterName, builder.build()));
         return result;
      }
   }

   private static class CounterRemoveCommand extends BaseCounterManagerCommand {
      private static final CounterRemoveCommand INSTANCE = new CounterRemoveCommand();

      private CounterRemoveCommand() {
         super();
      }

      @Override
      protected ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
            throws Exception {
         final String counterName = counterName(context, operation);
         counterManager.remove(counterName);
         return new ModelNode();
      }
   }

   private static class CounterConfigurationCommand extends BaseCounterManagerCommand {
      private static final CounterConfigurationCommand INSTANCE = new CounterConfigurationCommand();

      private CounterConfigurationCommand() {
         super();
      }

      @Override
      protected ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
            throws Exception {
         final String counterName = counterName(context, operation);
         final ModelNode result = new ModelNode().setEmptyList();
         CounterConfiguration configuration = counterManager.getConfiguration(counterName);
         if (configuration != null) {
            Properties properties = PropertyFormatter.getInstance().format(configuration);
            properties.forEach((key, value) -> {
               ModelNode node = new ModelNode();
               node.set(String.valueOf(key), String.valueOf(value));
               result.add(node);
            });
         } else {
            throw counterNotFound(counterName);
         }
         return result;
      }
   }

   private static abstract class BaseCounterManagerCommand extends CacheContainerCommands {

      BaseCounterManagerCommand() {
         super(0);
      }

      abstract ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
            throws Exception;

      @Override
      protected final ModelNode invokeCommand(EmbeddedCacheManager cacheManager, OperationContext context,
            ModelNode operation)
            throws Exception {
         Optional<CounterManager> optCounterManager = SecurityActions.findCounterManager(cacheManager);
         CounterManager counterManager = optCounterManager.orElseThrow(CounterManagerCommands::counterManagerNotFound);
         return invoke(counterManager, context, operation);
      }
   }
}
