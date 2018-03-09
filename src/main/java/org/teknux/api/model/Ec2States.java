package org.teknux.api.model;

import java.util.Arrays;
import java.util.Optional;

public enum Ec2States {

    UNKNOWN(-1),
    PENDING(0),
    RUNNING(16),
    //RUNNING_2(272),
    SHUTTING_DOWN(32),
    TERMINATED(48),
    STOPPING(64),
    STOPPED(80);

    private int code;

    Ec2States(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Optional<Ec2States> fromCode(int code) {
        return Arrays.asList(Ec2States.values()).stream().filter(ec2States -> ec2States.code == code).findFirst();
    }
}
