package org.teknux.service.automation;

import com.amazonaws.regions.Regions;
import org.teknux.service.IService;

public interface IEc2AutomationService extends IService {

    void startTill(String instanceId, Regions region, ISchedulerService.Schedule scheduleToStop);

    void runBetween(String instanceId, Regions region, ISchedulerService.Schedule scheduleToSart, ISchedulerService.Schedule scheduleToStop);

    void stopOn(String instanceId, Regions region, ISchedulerService.Schedule scheduleToStop);

    void cancelPlans(String instanceId);

    boolean hasPlan(String instanceId);

    ISchedulerService.Schedule getStartSchedule(String instanceId);

    ISchedulerService.Schedule getStopSchedule(String instanceId);
}
