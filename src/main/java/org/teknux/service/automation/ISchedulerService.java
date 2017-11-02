package org.teknux.service.automation;

import org.teknux.service.IService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface ISchedulerService extends IService {

    void plan(Runnable task, Schedule schedule);

    Collection<Runnable> planned();

    Optional<Schedule> find(Runnable task);

    void cancel(Runnable task);

    interface Schedule {
        LocalDateTime when();
    }
}
