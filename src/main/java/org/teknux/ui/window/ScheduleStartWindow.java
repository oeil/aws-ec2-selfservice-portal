package org.teknux.ui.window;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import org.teknux.service.automation.IEc2AutomationService;
import org.teknux.service.automation.ISchedulerService;
import org.teknux.service.automation.Schedule;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

public class ScheduleStartWindow extends AbstractInstancesActionWindow {

    public ScheduleStartWindow(Set<Instance> selection, Regions region, IEc2AutomationService ec2AutomationService) {
        super("Start Instance(s) For", selection, region, ec2AutomationService);
    }

    protected void createContent(VerticalLayout layout) {
        Button tenMinutesBtn = createButton("10 Minutes", event1 -> {
            final LocalDateTime now = LocalDateTime.now();
            doScheduleRunBetween(getSelection(), getRegion(), new Schedule(now), new Schedule(now.plusMinutes(10)));
            close();
        });

        Button oneHourBtn = createButton("1 Hour", event1 -> {
            final LocalDateTime now = LocalDateTime.now();
            doScheduleRunBetween(getSelection(), getRegion(), new Schedule(now), new Schedule(now.plusHours(1)));
            close();
        });

        Button tenHoursBtn = createButton("10 Hours", event1 -> {
            final LocalDateTime now = LocalDateTime.now();
            doScheduleRunBetween(getSelection(), getRegion(), new Schedule(now), new Schedule(now.plusHours(10)));
            close();
        });

        Button workweekBtn = createButton("Current Work Week", event1 -> {
            final LocalDateTime startOfWeek = LocalDateTime.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            final LocalDateTime endOfWeek = startOfWeek.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

            doScheduleRunBetween(getSelection(), getRegion(), new Schedule(startOfWeek), new Schedule(endOfWeek));
            close();
        });

        Button nextWorkweekBtn = createButton("Next Work Week", event1 -> {
            final LocalDateTime nextStartOfWeek = LocalDateTime.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            final LocalDateTime nextEndOfWeek = nextStartOfWeek.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

            doScheduleRunBetween(getSelection(), getRegion(), new Schedule(nextStartOfWeek), new Schedule(nextEndOfWeek));
            close();
        });

        Button forOneMonthBtn = createButton("1 Month", event1 -> {
            final LocalDateTime now = LocalDateTime.now();
            final LocalDateTime nextMonth = now.plusMonths(1);

            doScheduleRunBetween(getSelection(), getRegion(), new Schedule(now), new Schedule(nextMonth));
            close();
        });

        layout.addComponents(tenMinutesBtn, oneHourBtn, tenHoursBtn, workweekBtn,  nextWorkweekBtn, forOneMonthBtn);
    }

    protected void doScheduleRunBetween(Set<Instance> instances, Regions region, ISchedulerService.Schedule startTime, ISchedulerService.Schedule endTime) {
        instances.stream().forEach(instance -> {
            getEc2AutomationService().runBetween(instance.getInstanceId(), region, startTime, endTime);
        });
    }
}
