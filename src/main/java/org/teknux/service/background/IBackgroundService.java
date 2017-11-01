package org.teknux.service.background;

import org.teknux.service.IService;

public interface IBackgroundService extends IService {

    void execute(Runnable task);
}
