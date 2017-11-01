package org.teknux.task;

import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIDetachedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

public class LongRunningTask<Result> implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(LongRunningTask.class);

    private Callable<Result> actualWork;
    private WeakReference<LongRunningTaskCallback<Result>> weakCallback;
    private WeakReference<Component> weakUI;
    private CancelListener cancelListener;

    public LongRunningTask(Callable<Result> longOperation, LongRunningTaskCallback<Result> uiCallback, Component ui) {
        this.actualWork = longOperation;
        this.weakCallback = new WeakReference<>(uiCallback);
        this.weakUI = new WeakReference<>(ui);
    }

    @Override
    public void run() {
        try {
            LOG.trace("### About to run Background Task ###");
            Result longRunningOperationResult = actualWork.call();
            LOG.trace("### Background Task Done ###");

            final LongRunningTaskCallback<Result> callback = weakCallback.get();
            final Component uiComponent = weakUI.get();

            if (callback != null && uiComponent != null) {
                final UI ui = uiComponent.getUI();
                ui.access(() -> {
                    callback.done(longRunningOperationResult);
                    LOG.trace("### UI Task Done ###");

                    final PushMode pushMode = ui.getPushConfiguration().getPushMode();
                    if (PushMode.MANUAL.equals(pushMode)) {
                        ui.push();
                        LOG.trace("### Pushed ###");
                    } else if (PushMode.DISABLED.equals(pushMode)) {
                        LOG.error("!!! Push is Disabled !!!");
                    }
                });
            } else {
                LOG.trace(String.format("!!! Callback / UI Component is null !!! => UI=[%s] | Callback=[%s]", uiComponent, callback));
            }
        } catch (UIDetachedException e) {
            LOG.trace("!!! UI Component is detached, task cancelled !!!");
            doCancel();
        } catch (Exception e) {
            System.err.println(e);
        }

    }

    protected void doCancel() {
        if (cancelListener != null) {
            cancelListener.cancel(this);
        }
    }

    public void setCancelListener(CancelListener cancelListener) {
        this.cancelListener = cancelListener;
    }

    public interface LongRunningTaskCallback<Result> {
        void done(Result result);
    }

    public interface CancelListener<Task extends LongRunningTask<?>> {
        void cancel(Task t);
    }
}
