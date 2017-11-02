package org.teknux.task.automation;

import com.amazonaws.regions.Regions;
import org.teknux.api.Ec2Api;
import org.teknux.service.automation.ISchedulerService;

import java.util.HashSet;
import java.util.Set;

public class Ec2InstanceStartAutomationTask extends AbstractEc2InstanceAutomation implements Runnable {

    public Ec2InstanceStartAutomationTask(String instanceId, Regions region) {
        super(instanceId, region);
    }

    @Override
    public void run() {
        final Set<String> ids = new HashSet<>(1);
        ids.add(getInstanceId());
        Ec2Api.instance(getRegion()).startInstances(ids);
    }
}
