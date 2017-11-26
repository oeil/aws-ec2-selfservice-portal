package org.teknux.service.automation;

import com.amazonaws.regions.Regions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teknux.service.IServiceManager;
import org.teknux.service.ServiceException;
import org.teknux.task.automation.AbstractEc2InstanceAutomation;
import org.teknux.task.automation.Ec2InstanceStartAutomationTask;
import org.teknux.task.automation.Ec2InstanceStopAutomationTask;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Ec2AutomationServiceImpl implements IEc2AutomationService {

    private static final Logger LOG = LoggerFactory.getLogger(Ec2AutomationServiceImpl.class);

    private ISchedulerService schedulerService;

    @Override
    public void startTill(String instanceId, Regions region, ISchedulerService.Schedule scheduleToStop) {
        LOG.debug(String.format("Schedule Start Instance [%s] till [%s]", instanceId, scheduleToStop.when()));
        cancelPlans(instanceId);
        schedulerService.plan(new Ec2InstanceStartAutomationTask(instanceId, region), () -> LocalDateTime.now());
        schedulerService.plan(new Ec2InstanceStopAutomationTask(instanceId, region), scheduleToStop);
    }

    @Override
    public void runBetween(String instanceId, Regions region, ISchedulerService.Schedule scheduleToSart, ISchedulerService.Schedule scheduleToStop) {
        LOG.debug(String.format("Schedule Run Instance [%s] between [%s]", instanceId, scheduleToSart.when(), scheduleToStop.when()));
        cancelPlans(instanceId);
        schedulerService.plan(new Ec2InstanceStartAutomationTask(instanceId, region), scheduleToSart);
        schedulerService.plan(new Ec2InstanceStopAutomationTask(instanceId, region), scheduleToStop);
    }

    @Override
    public void stopOn(String instanceId, Regions region, ISchedulerService.Schedule scheduleToStop) {
        LOG.debug(String.format("Schedule Stop Instance [%s] on [%s]", instanceId, scheduleToStop.when()));
        cancelPlans(instanceId);
        schedulerService.plan(new Ec2InstanceStopAutomationTask(instanceId, region), scheduleToStop);
    }

    @Override
    public void cancelPlans(String instanceId) {
        LOG.debug(String.format("Cancelling all schedules Instance [%s]", instanceId));
        find(instanceId).stream().forEach(runnable -> {
            schedulerService.cancel(runnable);
            LOG.trace(String.format("++Cancelled InstanceId=[%s] Task=[%s]", instanceId, runnable));
        });
    }

    @Override
    public boolean hasPlan(String instanceId) {
        if (getStartSchedule(instanceId) != null) {
            return true;
        }

        if (getStopSchedule(instanceId) != null) {
            return true;
        }

        return false;
    }

    @Override
    public ISchedulerService.Schedule getStartSchedule(String instanceId) {
        Optional<Runnable> startTask = schedulerService.planned().stream().filter(runnable -> {
            if (runnable instanceof Ec2InstanceStartAutomationTask) {
                Ec2InstanceStartAutomationTask task = (Ec2InstanceStartAutomationTask) runnable;
                if (instanceId.equals(task.getInstanceId())) {
                    return true;
                }
            }
            return false;
        }).findFirst();

        if (!startTask.isPresent()) {
            return null;
        }

        return schedulerService.find(startTask.get());
    }

    @Override
    public ISchedulerService.Schedule getStopSchedule(String instanceId) {
        Optional<Runnable> stopTask = schedulerService.planned().stream().filter(runnable -> {
            if (runnable instanceof Ec2InstanceStopAutomationTask) {
                Ec2InstanceStopAutomationTask task = (Ec2InstanceStopAutomationTask) runnable;
                if (instanceId.equals(task.getInstanceId())) {
                    return true;
                }
            }
            return false;
        }).findFirst();

        if (!stopTask.isPresent()) {
            return null;
        }

        return schedulerService.find(stopTask.get());
    }

    private List<Runnable> find(String instanceId) {
        return schedulerService.planned().stream().filter(runnable -> {
            if (runnable instanceof AbstractEc2InstanceAutomation) {
                AbstractEc2InstanceAutomation task = (AbstractEc2InstanceAutomation) runnable;
                if (instanceId.equals(task.getInstanceId())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
    }

    @Override
    public void init(IServiceManager serviceManager) {
        schedulerService = serviceManager.getService(ISchedulerService.class);
    }

    @Override
    public void start() throws ServiceException {

    }

    @Override
    public void stop() throws ServiceException {

    }
}
