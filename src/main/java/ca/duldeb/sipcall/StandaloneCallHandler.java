package ca.duldeb.sipcall;

import static org.apache.commons.logging.LogFactory.getLog;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;

public class StandaloneCallHandler implements CallHandler {
    private static final Log LOGGER = getLog(StandaloneCallHandler.class);

    protected final CallManager callManager;

    private boolean shutdown;
    private AtomicInteger activeLegs;

    public StandaloneCallHandler(CallManager callManager) {
        this.callManager = callManager;
        shutdown = false;
        activeLegs = new AtomicInteger(callManager.getConfig().getNbLegs());
    }

    public void handleInviteOk(CallLegData leg) {
        LOGGER.info(leg.getName() + " INVITE OK, remote media=" + leg.getRemoteRtpAddress() + ":"
                + leg.getRemoteRtpPort());
        try {
            callManager.connectRtpSession(leg);
        } catch (ApplicationErrorException e) {
            LOGGER.error(e.getMessage(), e);
        }

        for (SipCallTask task : callManager.getConfig().getTasks()) {
            leg.getTasks().add(task);
        }

        doNextTask(leg);
    }

    public void handleInviteFailure(CallLegData leg, int code) {
        LOGGER.info(leg.getName() + " INVITE failed with " + code);
        handleFinished(leg);
    }

    public void handleBye(CallLegData leg) {
        LOGGER.info(leg.getName() + " received BYE");
        callManager.cleanupRtp(leg);
        handleFinished(leg);
    }

    public void handleByeOk(CallLegData leg) {
        LOGGER.info(leg.getName() + " BYE OK");
        handleFinished(leg);
    }

    public void handleByeFailure(CallLegData leg, int code) {
        LOGGER.info(leg.getName() + " BYE failed with " + code);
        handleFinished(leg);
    }

    private void handleFinished(CallLegData leg) {
        int legIndex = leg.getLegIndex();
        int callIndex = leg.getCallIndex();
        LOGGER.debug(leg.getName() + " finished");
        callIndex++;
        if (!shutdown) {
            if (callIndex < callManager.getConfig().getNbIterations()) {
                try {

                    CallLegData newCallLeg = callManager.createCallLegData(legIndex, callIndex);
                    callManager.doSendInvite(newCallLeg);
                } catch (ApplicationErrorException e) {
                    LOGGER.error(e.getMessage(), e);
                    shutdown = true;
                    callManager.doShutdown();
                }
            } else {
                int remaining = activeLegs.decrementAndGet();
                LOGGER.debug(remaining + " legs remaining");
                if (remaining == 0) {
                    shutdown = true;
                    callManager.doShutdown();
                }
            }
        }
    }

    public void handlePauseDone(CallLegData leg) {
        doNextTask(leg);
    }

    public void handlePlayDone(CallLegData leg) {
        doNextTask(leg);
    }

    private void doNextTask(CallLegData leg) {
        while (!leg.getTasks().isEmpty()) {
            SipCallTask next = leg.getTasks().remove();
            try {
                if (next.doIt(callManager, leg)) {
                    return;
                }
            } catch (ApplicationErrorException e) {
                LOGGER.error("Task failed", e);
            }
        }
        LOGGER.debug("no more tasks");
        callManager.doSendBye(leg);
    }
}
