package org.teknux.service.automation;

import java.time.LocalDateTime;

public class Schedule implements ISchedulerService.Schedule {

    private LocalDateTime dateTime;

    public Schedule(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public LocalDateTime when() {
        return dateTime;
    }
}
