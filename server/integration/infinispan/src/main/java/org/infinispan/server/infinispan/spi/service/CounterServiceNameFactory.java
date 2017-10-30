package org.infinispan.server.infinispan.spi.service;

import org.jboss.msc.service.ServiceName;

/**
 * Factory for generating service names for services associated with a counter.
 * @author Vladimir Blagojevic
 */
public interface CounterServiceNameFactory {

    /**
     * Returns an appropriate service name for the specified container and counter
     * @param container a container name
     * @param counter a counter name
     * @return a service name
     */
    ServiceName getServiceName(String container, String counter);
}
