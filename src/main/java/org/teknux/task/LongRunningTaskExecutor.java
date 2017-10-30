package org.teknux.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class LongRunningTaskExecutor extends ScheduledThreadPoolExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(LongRunningTaskExecutor.class);

    private Map<LongRunningTask<?>, Future<?>> tasks;

    public LongRunningTaskExecutor(int corePoolSize) {
        super(corePoolSize);
        this.tasks = new ConcurrentHashMap<>();
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        synchronized (runnable) {
            if (runnable instanceof LongRunningTask<?>) {
                LOG.trace(String.format("Registering Task=[%s]", task));
                final LongRunningTask<?> longRunningTask = (LongRunningTask<?>) runnable;
                if (tasks.containsKey(longRunningTask)) {
                    LOG.trace(String.format("Task=[%s] Already Scheduled, cancel it before re-schedule", task));
                    longRunningTask.doCancel();
                }
                longRunningTask.setCancelListener(t -> cancel(longRunningTask));
                tasks.put(longRunningTask, task);
            }
        }

        return super.decorateTask(runnable, task);
    }

    public void cancel(LongRunningTask<?> task) {
        LOG.trace(String.format("About to Cancel Task=[%s]", task));
        if (tasks.containsKey(task)) {
            tasks.get(task).cancel(false);
            tasks.remove(task);
            LOG.trace(String.format("Cancelled | Task=[%s]", task));
        } else {
            LOG.trace(String.format("Not Found | Task=[%s]", task));
        }
    }
}
