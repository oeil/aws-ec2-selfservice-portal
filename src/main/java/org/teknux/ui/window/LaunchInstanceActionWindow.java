package org.teknux.ui.window;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.teknux.service.automation.IEc2AutomationService;

import java.util.List;
import java.util.Set;

public class LaunchInstanceActionWindow extends Window {

    private Regions region;
    private IEc2AutomationService ec2AutomationService;

    public LaunchInstanceActionWindow(Regions region, IEc2AutomationService ec2AutomationService) {
        super("Launch Image");
        this.region = region;
        this.ec2AutomationService = ec2AutomationService;

        setResizable(false);
        setModal(true);
        setWidth(400, Unit.PIXELS);

        VerticalLayout layout = new VerticalLayout();
        createContent(layout);
        setContent(layout);
    }

    protected void createContent(VerticalLayout layout) {
        FormLayout form = new FormLayout();

        TextField nameFiled = new TextField();
        nameFiled.setRequiredIndicatorVisible(true);
        form.addComponent(nameFiled);



        layout.addComponent(form);
    }

    protected Regions getRegion() {
        return region;
    }

    protected IEc2AutomationService getEc2AutomationService() {
        return ec2AutomationService;
    }

    protected static Button createButton(String caption, Button.ClickListener clickListener) {
        Button button = new Button(caption);
        button.addClickListener(clickListener);
        button.addStyleName(ValoTheme.BUTTON_PRIMARY);
        button.setWidth(100, Unit.PERCENTAGE);
        return button;
    }
}
