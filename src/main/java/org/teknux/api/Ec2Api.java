package org.teknux.api;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Ec2Api {

    private AmazonEC2 ec2;

    public Ec2Api(AmazonEC2 ec2) {
        this.ec2 = ec2;
    }

    public static Ec2Api instance() {
        return instance(Regions.DEFAULT_REGION);
    }

    public static Ec2Api instance(Regions region) {
        return new Ec2Api(AmazonEC2ClientBuilder.standard().withCredentials(new PropertiesCredentialProvider()).withRegion(region).build());
    }

    public Set<Instance> instances() {
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

    public List<Address> elasticIPs() {
        DescribeAddressesResult response = ec2.describeAddresses();
        return response.getAddresses();
    }

    public String allocateElasticIP(String instanceId) {
        final AllocateAddressRequest allocateAddressRequest = new AllocateAddressRequest().withDomain(DomainType.Vpc);
        AllocateAddressResult allocateAddressResult = ec2.allocateAddress(allocateAddressRequest);

        final String allocationId = allocateAddressResult.getAllocationId();
        AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest().withInstanceId(instanceId).withAllocationId(allocationId);
        AssociateAddressResult associateAddressResult = ec2.associateAddress(associateAddressRequest);

        return allocateAddressResult.getPublicIp();
    }

    public List<KeyPairInfo> keyPairs() {
        return ec2.describeKeyPairs().getKeyPairs();
    }

    public List<SecurityGroup> securityGroups() {
        DescribeSecurityGroupsRequest securityGroupsRequest = new DescribeSecurityGroupsRequest();
        return ec2.describeSecurityGroups(securityGroupsRequest).getSecurityGroups();
    }

    public List<Image> images() {
        DescribeImagesRequest r = new DescribeImagesRequest();
        return ec2.describeImages().getImages();
    }

    public List<Instance> createInstance(String name, Image image, InstanceType instanceType, SecurityGroup securityGroup, KeyPairInfo keyPairInfo, boolean fixedIpAddress) {
        final RunInstancesRequest runRequest = new RunInstancesRequest()
                .withImageId(image.getImageId())
                .withInstanceType(instanceType)
                .withMaxCount(1)
                .withMinCount(1)
                .withSecurityGroupIds(securityGroup.getGroupId());

        final List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("Name", name));

        Reservation reservation = ec2.runInstances(runRequest).getReservation();
        if (fixedIpAddress) {
            reservation.getInstances().stream().forEach(instance -> {
                allocateElasticIP(instance.getInstanceId());
                assignTags(instance.getInstanceId(), tags);
            });
        }

        final List<Instance> instances = new ArrayList<>();
        ec2.describeInstances().withReservations(reservation).getReservations().forEach(reservation1 -> instances.addAll(reservation1.getInstances()));
        return instances;
    }

    public void assignTags(String resourceId, List<Tag> tags) {
        ec2.createTags(new CreateTagsRequest().withResources(resourceId).withTags(tags));
    }

    private static class PropertiesCredentialProvider implements AWSCredentialsProvider {

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
