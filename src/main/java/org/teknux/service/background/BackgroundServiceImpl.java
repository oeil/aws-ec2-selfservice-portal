package org.teknux.service.background;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teknux.service.IServiceManager;
import org.teknux.service.ServiceException;

import java.util.Map;
import java.util.concurrent.*;

public class BackgroundServiceImpl implements IBackgroundService {

    private static final Logger LOG = LoggerFactory.getLogger(BackgroundServiceImpl.class);

    private volatile Map<Runnable, Future<?>> tasks;
    private ExecutorService executorService;

    @Override
    public void execute(Runnable task) {
        //make sure the same task (instance) does not run concurrently
        synchronized (task) {
            if (tasks.containsKey(task)) {
                Future previousExecution = tasks.get(task);
                if (!previousExecution.isDone() && !previousExecution.isCancelled()) {
                    LOG.trace(String.format("Non-completed previous execution found [%s], cancelling it...", task));
                    previousExecution.cancel(false);
                }
            }
            Future newExecution = executorService.submit(() -> {
                task.run();
                tasks.remove(task);
            });
            tasks.put(task, newExecution);
            LOG.trace(String.format("New background execution submitted [%s]", task));
        }
    }

    @Override
    public void init(IServiceManager serviceManager) {

    }

    @Override
    public void start() throws ServiceException {
        tasks = new ConcurrentHashMap<>();
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void stop() throws ServiceException {
        try {
            executorService.shutdownNow();
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("Background task(s) killed, too long to complete!", e);
        }
    }
}
