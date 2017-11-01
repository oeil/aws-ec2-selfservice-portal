package org.teknux;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teknux.service.ServiceManager;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import java.util.Objects;

@WebServlet(urlPatterns = "/*", name = "AppServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = AppUI.class, productionMode = true)
public class AppServlet extends VaadinServlet implements SessionInitListener, SessionDestroyListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AppServlet.class);
    private static final String CONTEXT_ATTRIBUTE_SERVICE_MANAGER = "AppServiceManager";

    private ServiceManager serviceManager;

    @Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();

        serviceManager = initServiceManager();

        getService().addSessionInitListener(this);
        getService().addSessionDestroyListener(this);
    }

    private ServiceManager initServiceManager() throws UnavailableException {
        ServiceManager serviceManager;
        try {
            serviceManager = new ServiceManager();
            getServletContext().setAttribute(CONTEXT_ATTRIBUTE_SERVICE_MANAGER, serviceManager);

            //serviceManager.addService(IService.class, new Service());

            serviceManager.start();

        } catch (Exception e) {
            LOG.error("Error while starting Services", e);
            throw new UnavailableException("Error while initializing Services");
        }

        return serviceManager;
    }

    @Override
    public void destroy() {
        Objects.requireNonNull(serviceManager);
        try {
            serviceManager.stop(); // stop the service
        } catch (org.teknux.service.ServiceException e) {
            LOG.error("Error while stopping Services", e);
        }
        getServletContext().removeAttribute(CONTEXT_ATTRIBUTE_SERVICE_MANAGER);

        super.destroy();
    }

    @Override
    public void sessionInit(final SessionInitEvent event) throws com.vaadin.server.ServiceException {
        event.getSession().getSession().setMaxInactiveInterval(60);
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent event) {
    }
}
