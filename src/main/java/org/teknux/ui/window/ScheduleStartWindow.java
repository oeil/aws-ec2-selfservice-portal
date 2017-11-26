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
import java.time.temporal.Temporal;
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
            final LocalDateTime startOfWeek = LocalDateTime.now();
            final LocalDateTime endOfWeek = startOfWeek.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).atStartOfDay();

            doScheduleRunBetween(getSelection(), getRegion(), new Schedule(startOfWeek), new Schedule(endOfWeek));
            close();
        });
        final DayOfWeek todayDay = LocalDateTime.now().getDayOfWeek();
        if (todayDay.equals(DayOfWeek.SATURDAY) || todayDay.equals(DayOfWeek.SUNDAY)) {
            workweekBtn.setEnabled(false);
        }


        Button nextWorkweekBtn = createButton("Next Work Week", event1 -> {
            final LocalDateTime nextStartOfWeek = LocalDateTime.now().toLocalDate().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay();
            final LocalDateTime nextEndOfWeek = nextStartOfWeek.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).atStartOfDay();

            doScheduleRunBetween(getSelection(), getRegion(), new Schedule(nextStartOfWeek), new Schedule(nextEndOfWeek));
            close();
        });

        Button forOneMonthBtn = createButton("1 Month", event1 -> {
            final LocalDateTime now = LocalDateTime.now();
            final LocalDateTime nextMonth = now.toLocalDate().plusMonths(1).atStartOfDay();

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
