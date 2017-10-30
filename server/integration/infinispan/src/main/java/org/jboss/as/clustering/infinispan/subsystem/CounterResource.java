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

import org.infinispan.counter.api.Storage;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource
 * /subsystem=infinispan/cache-container=X/counter=*
 *
 * @author Pedro Ruivo
 * @author Vladimir Blagojevic
 * @since 9.2
 */
public class CounterResource extends SimpleResourceDefinition {

   //atributes
   static final AttributeDefinition COUNTER_NAME =
         new SimpleAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING, false)
            .setXmlName(Attribute.NAME.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

   static final AttributeDefinition STORAGE =
         new SimpleAttributeDefinitionBuilder(ModelKeys.STORAGE, ModelType.STRING, false)
         .setXmlName(Attribute.STORAGE.getLocalName())
         .setAllowExpression(false)
         .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
         .setAllowedValues(Storage.VOLATILE.toString(), Storage.PERSISTENT.toString())
         .build();

   static final AttributeDefinition INITIAL_VALUE =
         new SimpleAttributeDefinitionBuilder(ModelKeys.INITIAL_VALUE, ModelType.LONG, true)
         .setXmlName(Attribute.INITIAL_VALUE.getLocalName())
         .setAllowExpression(false)
         .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
         .build();

   static final AttributeDefinition[] ATTRIBUTES = { COUNTER_NAME, STORAGE, INITIAL_VALUE };

   // operations

   private final boolean runtimeRegistration;
   private final ResolvePathHandler resolvePathHandler;

   public CounterResource(PathElement pathElement, ResourceDescriptionResolver descriptionResolver,
         ResolvePathHandler resolvePathHandler, AbstractAddStepHandler addHandler, OperationStepHandler removeHandler,
         boolean runtimeRegistration) {
      super(pathElement, descriptionResolver, addHandler, removeHandler);
      this.resolvePathHandler = resolvePathHandler;
      this.runtimeRegistration = runtimeRegistration;
   }

   @Override
   public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
      super.registerAttributes(resourceRegistration);
      final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
      for (AttributeDefinition attr : ATTRIBUTES) {
          resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
      }

      if (runtimeRegistration) {
          // TODO runtime attributes go here
      }
   }

   @Override
   public void registerOperations(ManagementResourceRegistration resourceRegistration) {
      //TODO register reset op here
      super.registerOperations(resourceRegistration);
   }
}
