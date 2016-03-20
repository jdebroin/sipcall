package ca.duldeb.sipcall.resources;

import static org.apache.commons.logging.LogFactory.getLog;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.logging.Log;

import ca.duldeb.sipcall.ApplicationErrorException;
import ca.duldeb.sipcall.CallHandler;
import ca.duldeb.sipcall.CallLegData;
import ca.duldeb.sipcall.CallManager;

public class PolledCallHandler implements CallHandler {
    private static final Log LOGGER = getLog(PolledCallHandler.class);

    protected final CallManager callManager;
    
    private Queue<CallResponse> responses = new LinkedList<CallResponse>();
    private Queue<CallResponseListener> listeners = new LinkedList<CallResponseListener>();
    private Object lock = new Object();

    public PolledCallHandler(CallManager callManager) {
        this.callManager = callManager;
    }

    public void handleInviteOk(CallLegData leg) {
        LOGGER.info(leg.getName() + " INVITE OK, remote media=" + leg.getRemoteRtpAddress() + ":"
                + leg.getRemoteRtpPort());
        try {
            callManager.connectRtpSession(leg);
        } catch (ApplicationErrorException e) {
            LOGGER.error(e.getMessage(), e);
            callManager.doSendBye(leg);
            return;
        }
        handleResponse(new CallResponse(CallResponse.INVITE_OK, leg.getCallId()));
    }

    public void handleInviteFailure(CallLegData leg, int code) {
        LOGGER.info(leg.getName() + " INVITE failed with " + code);
        handleFinished(leg, "INVITE_FAILED", code);
    }

    public void handleBye(CallLegData leg) {
        LOGGER.info(leg.getName() + " received BYE");
        callManager.cleanupRtp(leg);
        handleFinished(leg, "BYE_RECEIVED", 200);
    }

    public void handleByeOk(CallLegData leg) {
        LOGGER.info(leg.getName() + " BYE OK");
        handleFinished(leg, "BYE_SENT", 200);
    }

    public void handleByeFailure(CallLegData leg, int code) {
        LOGGER.info(leg.getName() + " BYE failed with " + code);
        handleFinished(leg, "BYE_SENT", code);
    }

    private void handleFinished(CallLegData leg, String reason, int code) {
        LOGGER.debug(leg.getName() + " finished");
        handleResponse(new CallResponse(CallResponse.CALL_FINISHED, leg.getCallId(), reason, code));
    }

    public void handlePauseDone(CallLegData leg) {
        LOGGER.info(leg.getName() + " pause done");
        handleResponse(new CallResponse(CallResponse.PAUSE_DONE, leg.getCallId()));
    }

    public void handlePlayDone(CallLegData leg) {
        LOGGER.info(leg.getName() + " pause done");
        handleResponse(new CallResponse(CallResponse.PLAY_DONE, leg.getCallId()));
    }

    public synchronized void poll(CallResponseListener listener) {
        LOGGER.debug("poll : " + responses.size() + " responses");
        synchronized(lock) {
            listeners.add(listener);
            if (!responses.isEmpty()) {
                CallResponse response = responses.remove();
                sendToListeners(response);
            }
        }
    }

    private synchronized void handleResponse(CallResponse callResponse) {
        LOGGER.debug("handleResponse : " + listeners.size() + " listeners");
        synchronized(lock) {
            responses.add(callResponse);
            if (!listeners.isEmpty()) {
                CallResponse response = responses.remove();
                sendToListeners(response);
            }
        }
    }

    public void handlePollTimeout() {
        handleResponse(new CallResponse(CallResponse.TIMEOUT));
    }

    private void sendToListeners(CallResponse response) {
        LOGGER.debug("sendToListeners : " + listeners.size() + " listeners");
        while (!listeners.isEmpty()) {
            CallResponseListener listener = listeners.remove();
            listener.provide(response);
        }
    }

}
