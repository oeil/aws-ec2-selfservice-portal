package org.teknux;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teknux.api.fetcher.Ec2Fetcher;
import org.teknux.api.model.Ec2States;
import org.teknux.task.LongRunningTask;
import org.teknux.task.LongRunningTaskExecutor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 */
@Theme("mytheme")
@Push(value = PushMode.MANUAL)
public class AppUI extends UI {

    private static final Logger LOG = LoggerFactory.getLogger(AppUI.class);

    private static ScheduledExecutorService executor = new LongRunningTaskExecutor(255);

    private LongRunningTask.LongRunningTaskCallback<Set<Instance>> uiPostProcess;
    private LongRunningTask<Set<Instance>> refreshTask;

    private LongRunningTask.LongRunningTaskCallback<List<InstanceStateChange>> startUiPostProcess;
    private LongRunningTask<List<InstanceStateChange>> startInstanceTask;

    private LongRunningTask.LongRunningTaskCallback<List<InstanceStateChange>> stopUiPostProcess;
    private LongRunningTask<List<InstanceStateChange>> stopInstanceTask;

    private ComboBox<Regions> regionsComboBox;
    private Button startButton;
    private Button stopButton;
    private Grid<Instance> instancesGrid;
    private Label lastupdateLabel;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        final VerticalLayout rootLayout = new VerticalLayout();
        rootLayout.setMargin(true);
        rootLayout.setSpacing(true);
        rootLayout.setSizeFull();

        //top layout
        final HorizontalLayout topLayout = new HorizontalLayout();
        topLayout.setMargin(false);
        topLayout.setSpacing(true);
        topLayout.setWidth(100, Unit.PERCENTAGE);
        rootLayout.addComponents(topLayout);

        regionsComboBox = new ComboBox<>();
        regionsComboBox.setItemCaptionGenerator(new RegionCaptionGenerator());
        regionsComboBox.setItems(regions(Regions.GovCloud));
        regionsComboBox.setSelectedItem(Regions.US_EAST_1);
        regionsComboBox.setWidth(200, Unit.PIXELS);
        regionsComboBox.addSelectionListener(event -> doRefresh());
        regionsComboBox.setEnabled(false);
        regionsComboBox.setPageLength(25);
        topLayout.addComponent(regionsComboBox);

        //buttons bar
        final HorizontalLayout topButtonsLayout = new HorizontalLayout();
        topButtonsLayout.setMargin(false);
        topButtonsLayout.setSpacing(true);
        topButtonsLayout.setWidthUndefined();
        topLayout.addComponents(topButtonsLayout);
        topLayout.setComponentAlignment(topButtonsLayout, Alignment.MIDDLE_RIGHT);

        startButton = new Button("Start");
        startButton.addStyleName("primary");
        startButton.setEnabled(false);
        startButton.addClickListener(event -> doStart(instancesGrid.getSelectedItems()));

        stopButton = new Button("Stop");
        stopButton.addStyleName("danger");
        stopButton.setEnabled(false);
        stopButton.addClickListener(event -> doStop(instancesGrid.getSelectedItems()));

        topButtonsLayout.addComponents(startButton, stopButton);

        // ec2 instances grid
        instancesGrid = new Grid<>();
        instancesGrid.addColumn(Instance::getTags, tags -> tags.stream().filter(tag -> tag.getKey().toLowerCase().equals("name")).findFirst().get().getValue()).setCaption("Name");
        instancesGrid.addColumn(Instance::getInstanceId).setCaption("Id");
        instancesGrid.addColumn(Instance::getState, instanceState -> instanceState.getName()).setCaption("State");
        instancesGrid.addColumn(Instance::getPublicIpAddress).setCaption("IPv4 Public IP");
        instancesGrid.setHeight(100, Unit.PERCENTAGE);
        instancesGrid.setWidth(100, Unit.PERCENTAGE);
        instancesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        instancesGrid.addSelectionListener(event -> gridSelectionChanged(event));
        instancesGrid.addItemClickListener(event -> {
            final Instance clickedInstance = event.getItem();
            if (clickedInstance != null) {
                instancesGrid.deselectAll();
                instancesGrid.select(clickedInstance);
            }
        });
        instancesGrid.setVisible(false);

        lastupdateLabel = new Label();

        rootLayout.addComponents(instancesGrid, lastupdateLabel);
        rootLayout.setExpandRatio(instancesGrid, 1.0f);
        rootLayout.setComponentAlignment(lastupdateLabel, Alignment.BOTTOM_RIGHT);

        setContent(rootLayout);

        // run a refresh to start fetching AWS Ec2 data in background and avoid blocking UI
        setupStartTasks();
        setupStopTasks();
        setupRefreshTasks();
        doRefresh();
    }

    private void setupRefreshTasks() {
        Callable<Set<Instance>> backgroundTask = () -> Ec2Fetcher.instance(regionsComboBox.getSelectedItem().orElse(Regions.DEFAULT_REGION)).getInstances();

        //configure ui required handling after background task is done
        uiPostProcess = instances -> {
            if (!instancesGrid.isVisible()) {
                instancesGrid.setVisible(true);
            }

            Set<Instance> selectedItems = instancesGrid.asMultiSelect().getSelectedItems();
            instancesGrid.setItems(instances);
            if (selectedItems != null) {
                selectedItems.forEach(instance -> instancesGrid.select(instance));
            }
            lastupdateLabel.setValue(String.format("Lastupdate at %s", LocalDateTime.now().format(DateTimeFormatter.ISO_TIME).toString()));
            regionsComboBox.setEnabled(true);
        };

        //configure overall recurrent task (background data fetch + ui update)
        refreshTask = new LongRunningTask<>(backgroundTask, uiPostProcess, instancesGrid);
    }

    private void setupStartTasks() {
        Callable<List<InstanceStateChange>> backgroundStartTask = () -> {
            Set<String> ids = new HashSet<>(instancesGrid.getSelectedItems().stream().map(instance -> instance.getInstanceId()).collect(Collectors.toList()));
            return Ec2Fetcher.instance(regionsComboBox.getSelectedItem().orElse(Regions.DEFAULT_REGION)).startInstances(ids);
        };

        //configure ui required handling after background task is done
        startUiPostProcess = instances -> {
            instancesGrid.deselectAll();
            doRefresh();
        };

        //configure overall recurrent task (background data fetch + ui update)
        startInstanceTask = new LongRunningTask<>(backgroundStartTask, startUiPostProcess, instancesGrid);
    }

    private void setupStopTasks() {
        Callable<List<InstanceStateChange>> backgroundStopTask = () -> {
            Set<String> ids = new HashSet<>(instancesGrid.getSelectedItems().stream().map(instance -> instance.getInstanceId()).collect(Collectors.toList()));
            return Ec2Fetcher.instance(regionsComboBox.getSelectedItem().orElse(Regions.DEFAULT_REGION)).stopInstances(ids);
        };

        //configure ui required handling after background task is done
        stopUiPostProcess = instances -> {
            instancesGrid.deselectAll();
            doRefresh();
        };

        //configure overall recurrent task (background data fetch + ui update)
        stopInstanceTask = new LongRunningTask<>(backgroundStopTask, stopUiPostProcess, instancesGrid);
    }

    private List<Regions> regions(Regions... exclude) {
        return Arrays.asList(Regions.values()).stream().filter(regions -> !Arrays.asList(exclude).contains(regions)).collect(Collectors.toList());
    }

    private void doRefresh() {
        lastupdateLabel.setValue("loading ...");
        regionsComboBox.setEnabled(false);
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        executor.scheduleAtFixedRate(refreshTask, 0, 30, TimeUnit.SECONDS);
    }

    private void gridSelectionChanged(final SelectionEvent<Instance> event) {
        final Set<Instance> selection = event.getAllSelectedItems();
        updateButtonsState(selection);
    }

    private void updateButtonsState(final Set<Instance> selection) {
        if (selection == null || selection.isEmpty()) {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            return;
        }

        boolean isStopped = selection.stream().allMatch(instance -> instance.getState().getCode() == Ec2States.STOPPED.getCode());
        startButton.setEnabled(isStopped);

        boolean isStarted = selection.stream().allMatch(instance -> instance.getState().getCode() == Ec2States.RUNNING.getCode());
        stopButton.setEnabled(isStarted);
    }

    private void doStart(Set<Instance> instances) {
        new Notification("Start", String.format("Starting [%s] Instance(s)", instances.size()), Notification.Type.HUMANIZED_MESSAGE, true).show(this.getPage());
        executor.execute(startInstanceTask);
    }

    private void doStop(Set<Instance> instances) {
        new Notification("Stop", String.format("Stopping [%s] Instance(s) to stop", instances.size()), Notification.Type.HUMANIZED_MESSAGE, true).show(this.getPage());
        executor.execute(stopInstanceTask);
    }

    @Override
    public void close() {
        LOG.trace("Closing UI");
        super.close();
    }

    @Override
    public void detach() {
        LOG.trace("Detaching UI");
        super.detach();
    }

    private class RegionCaptionGenerator implements ItemCaptionGenerator<Regions> {

        @Override
        public String apply(Regions item) {
            switch (item) {
                case US_WEST_2:
                    return "US West (Oregon)";
                case US_WEST_1:
                    return "US West (N. California)";
                case US_EAST_2:
                    return "US East (Ohio)";
                case US_EAST_1:
                    return "US East (N. Virginia)";
                case AP_SOUTH_1:
                    return "Asia Pacific (Mumbai)";
                case AP_NORTHEAST_2:
                    return "Asia Pacific (Seoul)";
                case AP_SOUTHEAST_1:
                    return "Asia Pacific (Singapore)";
                case AP_SOUTHEAST_2:
                    return "Asia Pacific (Sydney)";
                case AP_NORTHEAST_1:
                    return "Asia Pacific (Tokyo)";
//                case CA_CENTRAL_1:
//                    return "Canada (Central)";
                case CN_NORTH_1:
                    return "China (Beijing)";
                case EU_CENTRAL_1:
                    return "EU (Frankfurt)";
                case EU_WEST_1:
                    return "EU (Ireland)";
//                case EU_WEST_2:
//                    return "EU (London)";
                case  SA_EAST_1:
                    return "South America (São Paulo)";
                case GovCloud:
                    return "AWS GovCloud (US)";
            }
            return "Unknown";
        }
    }
}
