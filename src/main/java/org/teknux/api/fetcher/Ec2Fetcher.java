package org.teknux.api.fetcher;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Ec2Fetcher {

    private AmazonEC2 ec2;

    public Ec2Fetcher(AmazonEC2 ec2) {
        this.ec2 = ec2;
    }

    public static Ec2Fetcher instance() {
        return instance(Regions.DEFAULT_REGION);
    }

    public static Ec2Fetcher instance(Regions region) {
        return new Ec2Fetcher(AmazonEC2ClientBuilder.standard().withCredentials(new PropertiesCredentialProvider()).withRegion(region).build());
    }

    public Set<Instance> getInstances() {
        DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();

        List<Reservation> reservations = describeInstancesRequest.getReservations();
        Set<Instance> instances = new HashSet<>();
        reservations.forEach(reservation -> instances.addAll(reservation.getInstances()));

        return instances;
    }

    public List<InstanceStateChange> startInstances(Set<String> ids) {
        StartInstancesRequest request = new StartInstancesRequest();
        request.setInstanceIds(ids);
        StartInstancesResult result = ec2.startInstances(request);
        return result.getStartingInstances();
    }

    public List<InstanceStateChange> stopInstances(Set<String> ids) {
        StopInstancesRequest request = new StopInstancesRequest();
        request.setInstanceIds(ids);
        StopInstancesResult result = ec2.stopInstances(request);
        return result.getStoppingInstances();
    }

    public static class PropertiesCredentialProvider implements AWSCredentialsProvider {

        public static final String AWS_ACCESS_KEY_ID_PROPERTY = "AWSAccessKeyId";
        public static final String AWS_SECRET_KEY_PROPERTY = "AWSSecretKey";

        @Override
        public AWSCredentials getCredentials() {
            final String accessKey = System.getProperty(AWS_ACCESS_KEY_ID_PROPERTY);
            final String secretKey = System.getProperty(AWS_SECRET_KEY_PROPERTY);

            return new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return accessKey;
                }

                @Override
                public String getAWSSecretKey() {
                    return secretKey;
                }
            };
        }

        @Override
        public void refresh() {
            //nop
        }
    }
}
