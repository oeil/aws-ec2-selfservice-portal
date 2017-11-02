package org.teknux.task.automation;

import com.amazonaws.regions.Regions;

public abstract class AbstractEc2InstanceAutomation {

    private String instanceId;
    private Regions region;

    public AbstractEc2InstanceAutomation(String instanceId, Regions region) {
        this.instanceId = instanceId;
        this.region = region;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractEc2InstanceAutomation that = (AbstractEc2InstanceAutomation) o;

        if (instanceId != null ? !instanceId.equals(that.instanceId) : that.instanceId != null) return false;
        return region == that.region;
    }

    @Override
    public int hashCode() {
        int result = instanceId != null ? instanceId.hashCode() : 0;
        result = 31 * result + (region != null ? region.hashCode() : 0);
        return result;
    }
}
