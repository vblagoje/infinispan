package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 8.0
 */
class CounterResource extends SimpleResourceDefinition {

   public static final PathElement PATH = PathElement.pathElement(ModelKeys.COUNTER);
   private final boolean runtimeRegistration;
   private final ResolvePathHandler resolvePathHandler;

   CounterResource(ResolvePathHandler resolvePathHandler, boolean runtimeRegistration) {
      super(PATH, new InfinispanResourceDescriptionResolver(ModelKeys.COUNTER));
      this.runtimeRegistration = runtimeRegistration;
      this.resolvePathHandler = resolvePathHandler;
   }


   @Override
   public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
      super.registerAttributes(resourceRegistration);
   }

   @Override
   public void registerOperations(ManagementResourceRegistration resourceRegistration) {
      super.registerOperations(resourceRegistration);
   }

}
