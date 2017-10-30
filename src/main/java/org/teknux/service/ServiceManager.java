package org.teknux.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Central access point for services
 */
public class ServiceManager implements IServiceManager {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceManager.class);

    /**
     * Running Service List
     */
    protected final Map<Class<? extends IService>, IService> services = new LinkedHashMap<>();

    public <T extends IService> void addService(Class<T> clazz, T service) {
        service.init(this);
        services.put(clazz, service);
    }

    @Override
    public void start() throws ServiceException {
        LOG.trace("[Service] Starting Service Manager...");

        synchronized (services) {
            LOG.trace("[Service] Starting {} Services...", services.size());
            for (final IService service : services.values()) {
                LOG.trace("[Service] Starting Service [{}]...", service.getClass().getSimpleName());
                service.start();
                LOG.trace("[Service] Service [{}] started", service.getClass().getSimpleName());
            }
            LOG.trace("[Service] {} Services started", services.size());
        }

        LOG.trace("[Service] Service Manager started");
    }

    @Override
    public void stop() throws ServiceException {
        LOG.trace("[Service] Stopping Service Manager...");

        synchronized (services) {
            LOG.trace("[Service] Stopping {} Services...", services.size());

            List<IService> reverseServices = new ArrayList<>(services.values());
            Collections.reverse(reverseServices);

            for (final IService service : reverseServices) {
                LOG.trace("[Service] Stopping Service [{}]...", service.getClass().getSimpleName());
                service.stop();
                LOG.trace("[Service] Service [{}] stopped", service.getClass().getSimpleName());
            }
            LOG.trace("[Service] {} Services stopped", services.size());
        }

        LOG.trace("[Service] Service Manager stopped.");
    }

    @Override
    public <T extends IService> T getService(final Class<T> serviceClass) {
        synchronized (services) {
            final IService wantedService = services.get(serviceClass);
            if (wantedService == null || !serviceClass.isAssignableFrom(wantedService.getClass())) {
                throw new IllegalArgumentException("Service Unavailable");
            }
            return serviceClass.cast(wantedService);
        }
    }

    @Override
    public Map<Class<? extends IService>, IService> getServices() {
        return services;
    }
}
