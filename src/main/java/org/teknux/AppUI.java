package org.teknux;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontIcon;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;
import de.akquinet.engineering.vaadin.timerextension.TimerExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teknux.api.Ec2Api;
import org.teknux.api.model.Ec2States;
import org.teknux.service.IServiceManager;
import org.teknux.service.background.IBackgroundService;
import org.teknux.task.LongRunningTask;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 *
 */
@Theme("mytheme")
@Push(value = PushMode.MANUAL)
public class AppUI extends UI {

    private static final Logger LOG = LoggerFactory.getLogger(AppUI.class);

    private LongRunningTask.LongRunningTaskCallback<ViewModel> uiPostProcess;
    private LongRunningTask<ViewModel> refreshTask;

    private LongRunningTask.LongRunningTaskCallback<List<InstanceStateChange>> startUiPostProcess;
    private LongRunningTask<List<InstanceStateChange>> startInstanceTask;

    private LongRunningTask.LongRunningTaskCallback<List<InstanceStateChange>> stopUiPostProcess;
    private LongRunningTask<List<InstanceStateChange>> stopInstanceTask;

    private TimerExtension timerExtension;

    private ComboBox<Regions> regionsComboBox;
    private Button startButton;
    private Button stopButton;
    private Grid<Instance> instancesGrid;
    private Label lastupdateLabel;

    private ViewModel viewModel;

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
        regionsComboBox.setWidth(250, Unit.PIXELS);
        regionsComboBox.addSelectionListener(event -> {
            instancesGrid.deselectAll();
            doRefresh();
        });
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

        startButton = createButton("Start", event -> doStart(instancesGrid.getSelectedItems()));
        startButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        startButton.setEnabled(false);

        stopButton = createButton("Stop", event -> doStop(instancesGrid.getSelectedItems()));
        stopButton.addStyleName(ValoTheme.BUTTON_DANGER);
        stopButton.setEnabled(false);

        topButtonsLayout.addComponents(startButton, stopButton);

        // ec2 instances grid
        instancesGrid = new Grid<>();
        instancesGrid.addColumn(Instance::getTags, tags -> tags.stream().filter(tag -> tag.getKey().toLowerCase().equals("name")).findFirst().get().getValue()).setCaption("Name");
        instancesGrid.addColumn(Instance::getState, instanceState -> {
            final Ec2States ec2States= Ec2States.fromCode(instanceState.getCode()).get();
            FontIcon icon = VaadinIcons.QUESTION;
            if (ec2States != null) {
                switch (ec2States) {
                    case PENDING:
                        icon = VaadinIcons.HOURGLASS;
                        break;
                    case RUNNING:
                        icon = VaadinIcons.CHECK_CIRCLE;
                        break;
                    case STOPPED:
                        icon = VaadinIcons.CIRCLE_THIN;
                        break;
                    case STOPPING:
                        icon = VaadinIcons.CIRCLE;
                        break;
                    case TERMINATED:
                        icon = VaadinIcons.CLOSE_CIRCLE_O;
                        break;
                }
            }
            return icon.getHtml() + String.format("<span>%s</span>", instanceState.getName());
        }, new HtmlRenderer()).setCaption("State");
        instancesGrid.addColumn(Instance::getInstanceId).setCaption("Id");
        instancesGrid.addColumn(Instance::getPublicIpAddress, instanceAddress -> {
            if (instanceAddress == null || instanceAddress.isEmpty()) {
                return "";
            }
            boolean isElastic = viewModel.elasticIPs.stream().filter(address -> instanceAddress.equals(address.getPublicIp())).findFirst().isPresent();
            return isElastic ? String.format("[ %s ]", instanceAddress) : instanceAddress;

        }).setCaption("IPv4 Public IP");
        instancesGrid.addColumn(Instance::getLaunchTime).setCaption("Launch Time");

        instancesGrid.setStyleGenerator(instance -> {
            final Ec2States state = Ec2States.fromCode(instance.getState().getCode()).get();
            if (state != null) {
                switch (state) {
                    case PENDING:
                        return "style-line-ec2-instance-pending";
                    case RUNNING:
                        return "style-line-ec2-instance-running";
                    case STOPPED:
                        return "style-line-ec2-instance-stopped";
                    case STOPPING:
                        return "style-line-ec2-instance-stopping";
                    case TERMINATED:
                        return "style-line-ec2-instance-terminated";
                }
            }
            return null;
        });
        instancesGrid.setHeight(100, Unit.PERCENTAGE);
        instancesGrid.setWidth(100, Unit.PERCENTAGE);
        instancesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        instancesGrid.addSelectionListener(event -> gridSelectionChanged(event));
        instancesGrid.addItemClickListener(event -> {
            final Instance clickedInstance = event.getItem();
            if (clickedInstance != null) {
                final Set<Instance> selection = instancesGrid.getSelectedItems();
                instancesGrid.deselectAll();
                if (!selection.contains(clickedInstance) || selection.size() > 1) {
                    instancesGrid.select(clickedInstance);
                } else {
                    this.focus();
                }
            }
        });
        instancesGrid.setColumnReorderingAllowed(true);
        instancesGrid.getColumns().stream().forEach(instanceColumn -> instanceColumn.setHidable(true));
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

    private static Button createButton(String caption, Button.ClickListener clickListener) {
        Button button = new Button(caption);
        button.addClickListener(clickListener);
        return button;
    }

    private void setupRefreshTasks() {
        timerExtension = TimerExtension.create(this);
        timerExtension.setIntervalInMs(30*1000);
        timerExtension.addTimerListener(timerEvent -> doRefresh());

        Callable<ViewModel> backgroundTask = () -> {
            final Ec2Api fetcher = Ec2Api.instance(regionsComboBox.getSelectedItem().orElse(Regions.DEFAULT_REGION));
            final Set<Instance> instances = fetcher.instances();
            final List<Address> elasticAddresses = fetcher.elasticIPs();

            return new ViewModel(instances, elasticAddresses);
        };

        //configure ui required handling after background task is done
        uiPostProcess = viewModel -> {
            this.viewModel = viewModel;

            if (!instancesGrid.isVisible()) {
                instancesGrid.setVisible(true);
            }

            Set<Instance> selectedItems = instancesGrid.asMultiSelect().getSelectedItems();
            instancesGrid.setItems(viewModel.getInstances());
            if (selectedItems != null) {
                selectedItems.forEach(instance -> instancesGrid.select(instance));
            }

            lastupdateLabel.setValue(String.format("Lastupdate at %s", LocalDateTime.now().format(DateTimeFormatter.ISO_TIME).toString()));
            regionsComboBox.setEnabled(true);

            timerExtension.start();
        };

        //configure overall recurrent task (background data fetch + ui update)
        refreshTask = new LongRunningTask<>(backgroundTask, uiPostProcess, instancesGrid);
    }

    private void setupStartTasks() {
        Callable<List<InstanceStateChange>> backgroundStartTask = () -> {
            Set<String> ids = new HashSet<>(instancesGrid.getSelectedItems().stream().map(instance -> instance.getInstanceId()).collect(Collectors.toList()));
            return Ec2Api.instance(regionsComboBox.getSelectedItem().orElse(Regions.DEFAULT_REGION)).startInstances(ids);
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
            return Ec2Api.instance(regionsComboBox.getSelectedItem().orElse(Regions.DEFAULT_REGION)).stopInstances(ids);
        };

        //configure ui required handling after background task is done
        stopUiPostProcess = instances -> {
            instancesGrid.deselectAll();
            doRefresh();
        };

        //configure overall recurrent task (background data fetch + ui update)
        stopInstanceTask = new LongRunningTask<>(backgroundStopTask, stopUiPostProcess, instancesGrid)  ;
    }

    private List<Regions> regions(Regions... exclude) {
        return Arrays.asList(Regions.values()).stream().filter(regions -> !Arrays.asList(exclude).contains(regions)).collect(Collectors.toList());
    }

    private void doRefresh() {
        if (timerExtension.isStarted()) {
            timerExtension.stop();
        }

        lastupdateLabel.setValue("loading ...");
        regionsComboBox.setEnabled(false);
        startButton.setEnabled(false);
        stopButton.setEnabled(false);

        // run task now in background
        getServiceManager().getService(IBackgroundService.class).execute(refreshTask);
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
        getServiceManager().getService(IBackgroundService.class).execute(startInstanceTask);
    }

    private void doStop(Set<Instance> instances) {
        new Notification("Stop", String.format("Stopping [%s] Instance(s) to stop", instances.size()), Notification.Type.HUMANIZED_MESSAGE, true).show(this.getPage());
        getServiceManager().getService(IBackgroundService.class).execute(stopInstanceTask);
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

    public IServiceManager getServiceManager() {
        return AppServlet.getServiceManager(VaadinServlet.getCurrent().getServletContext());
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

    private static class ViewModel {

        private Set<Instance> instances;
        private List<Address> elasticIPs;

        public ViewModel(Set<Instance> instances, List<Address> elasticIPs) {
            this.instances = instances;
            this.elasticIPs = elasticIPs;
        }

        public Set<Instance> getInstances() {
            return instances;
        }

        public void setInstances(Set<Instance> instances) {
            this.instances = instances;
        }

        public List<Address> getElasticIPs() {
            return elasticIPs;
        }

        public void setElasticIPs(List<Address> elasticIPs) {
            this.elasticIPs = elasticIPs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ViewModel viewModel = (ViewModel) o;

            if (instances != null ? !instances.equals(viewModel.instances) : viewModel.instances != null) return false;
            return elasticIPs != null ? elasticIPs.equals(viewModel.elasticIPs) : viewModel.elasticIPs == null;
        }

        @Override
        public int hashCode() {
            int result = instances != null ? instances.hashCode() : 0;
            result = 31 * result + (elasticIPs != null ? elasticIPs.hashCode() : 0);
            return result;
        }
    }
}
