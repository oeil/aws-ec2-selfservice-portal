package org.teknux.service.automation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teknux.service.IServiceManager;
import org.teknux.service.ServiceException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SchedulerServiceImpl implements ISchedulerService {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    private ScheduledExecutorService scheduledExecutorService;
    private volatile Map<Schedule, Runnable> tasks;

    @Override
    public void plan(Runnable task, Schedule schedule) {
        tasks.put(schedule, task);
    }

    @Override
    public Collection<Runnable> planned() {
        return tasks.values();
    }

    @Override
    public Optional<Schedule> find(Runnable task) {
        Optional<Map.Entry<Schedule, Runnable>> entry = tasks.entrySet().stream().filter(scheduleRunnableEntry -> scheduleRunnableEntry.getValue().equals(task)).findFirst();
        if (entry.isPresent()) {
            return Optional.of(entry.get().getKey());
        }
        return Optional.of(null);
    }

    @Override
    public void cancel(Runnable task) {
        tasks.remove(task);
    }

    @Override
    public void init(IServiceManager serviceManager) {
        tasks = new ConcurrentHashMap<>();
    }

    @Override
    public void start() throws ServiceException {
        LOG.trace("Starting scheduler...");
        scheduledExecutorService = Executors.newScheduledThreadPool(128);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            LOG.trace("Lookup for registered tasks to trigger");
            final List<Schedule> schedules = tasks.keySet().stream().filter(schedule -> schedule.when().isBefore(LocalDateTime.now())).collect(Collectors.toList());
            LOG.trace(String.format("-- Found [%s]", schedules.size()));
            schedules.parallelStream().forEach(schedule -> {
                scheduledExecutorService.execute(() -> {
                    final Runnable task = tasks.remove(schedule);
                    LOG.trace(String.format("++ Running [%s] planned for [%s]", task, schedule.when()));
                    task.run();
                    LOG.trace(String.format("++ Completed [%s]", task));
                });
            });
        }, 0, 30, TimeUnit.SECONDS);
        LOG.trace("Scheduler started");
    }

    @Override
    public void stop() throws ServiceException {
        LOG.trace("Stopping scheduler...");
        scheduledExecutorService.shutdownNow();
        try {
            scheduledExecutorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("Scheduler took too long to shutodown, it has been killed!", e);
        }
        tasks.clear();
        LOG.trace("Scheduler stopped");
    }
}
