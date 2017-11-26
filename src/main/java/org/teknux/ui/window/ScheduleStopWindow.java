package org.teknux.ui.window;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import org.teknux.service.automation.IEc2AutomationService;
import org.teknux.service.automation.ISchedulerService;
import org.teknux.service.automation.Schedule;

import java.time.LocalDateTime;
import java.util.Set;

public class ScheduleStopWindow extends ScheduleStartWindow {

    public ScheduleStopWindow(Set<Instance> selection, Regions region, IEc2AutomationService ec2AutomationService) {
        super(selection, region, ec2AutomationService);
        setCaption("Stop Instance(s) In");
    }

    protected void createContent(VerticalLayout layout) {
        Button tenMinutesBtn = createButton("10 Minutes", event1 -> {
            final LocalDateTime now = LocalDateTime.now();
            doScheduleStop(getSelection(), getRegion(), new Schedule(now.plusMinutes(10)));
            close();
        });

        Button oneHourBtn = createButton("1 Hour", event1 -> {
            final LocalDateTime now = LocalDateTime.now();
            doScheduleStop(getSelection(), getRegion(), new Schedule(now.plusHours(1)));
            close();
        });

        Button tenHoursBtn = createButton("10 Hours", event1 -> {
            final LocalDateTime now = LocalDateTime.now();
            doScheduleStop(getSelection(), getRegion(), new Schedule(now.plusHours(10)));
            close();
        });

        Button workweekBtn = createButton("5 Days", event1 -> {
            final LocalDateTime now = LocalDateTime.now();
            doScheduleStop(getSelection(), getRegion(), new Schedule(now.toLocalDate().plusDays(5).atStartOfDay()));
            close();
        });

        Button nextMonthBtn = createButton("1 Month", event1 -> {
            final LocalDateTime inOneMonth = LocalDateTime.now().toLocalDate().plusMonths(1).atStartOfDay();
            doScheduleStop(getSelection(), getRegion(), new Schedule(inOneMonth));
            close();
        });

        layout.addComponents(tenMinutesBtn, oneHourBtn, tenHoursBtn, workweekBtn, nextMonthBtn);
    }

    protected void doScheduleStop(Set<Instance> instances, Regions region, ISchedulerService.Schedule endTime) {
        instances.stream().forEach(instance -> {
            getEc2AutomationService().stopOn(instance.getInstanceId(), region, endTime);
        });
    }
}
