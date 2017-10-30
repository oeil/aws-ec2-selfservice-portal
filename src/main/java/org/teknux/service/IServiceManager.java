package org.teknux.service;

import java.util.Map;


public interface IServiceManager {

    <T extends IService> T getService(final Class<T> serviceClass);

    Map<Class<? extends IService>, IService> getServices();

    void start() throws ServiceException;

    void stop() throws ServiceException;

}
