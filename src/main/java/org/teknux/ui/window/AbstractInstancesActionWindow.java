package org.teknux.ui.window;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import org.teknux.service.automation.IEc2AutomationService;

import java.util.Set;

public abstract class AbstractInstancesActionWindow extends Window {

    private Set<Instance> selection;
    private Regions region;
    private IEc2AutomationService ec2AutomationService;

    public AbstractInstancesActionWindow(String title, Set<Instance> selection, Regions region, IEc2AutomationService ec2AutomationService) {
        super(title);
        this.selection = selection;
        this.region = region;
        this.ec2AutomationService = ec2AutomationService;

        setResizable(false);
        setModal(true);
        setWidth(400, Sizeable.Unit.PIXELS);

        VerticalLayout layout = new VerticalLayout();
        createContent(layout);
        setContent(layout);
    }

    protected abstract void createContent(VerticalLayout layout);

    public Set<Instance> getSelection() {
        return selection;
    }

    public Regions getRegion() {
        return region;
    }

    public IEc2AutomationService getEc2AutomationService() {
        return ec2AutomationService;
    }

    protected static Button createButton(String caption, Button.ClickListener clickListener) {
        Button button = new Button(caption);
        button.addClickListener(clickListener);
        button.addStyleName(ValoTheme.BUTTON_PRIMARY);
        button.setWidth(100, Sizeable.Unit.PERCENTAGE);
        return button;
    }
}
