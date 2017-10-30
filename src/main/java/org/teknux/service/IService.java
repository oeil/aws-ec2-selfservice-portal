package org.teknux.service;

public interface IService {

    void init(final IServiceManager serviceManager);

    void start() throws ServiceException;

    void stop() throws ServiceException;
}
