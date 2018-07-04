package ca.duldeb.sipcall.resources;

import static org.apache.commons.logging.LogFactory.getLog;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;

import ca.duldeb.sipcall.ApplicationErrorException;
import ca.duldeb.sipcall.CallLegData;
import ca.duldeb.sipcall.CallManager;
import ca.duldeb.sipcall.SipCallConfig;

@Path("/sipcall")
public class SipCallResource {
    private static final Log LOGGER = getLog(SipCallResource.class);

    private static CallManager callManager;
    private static PolledCallHandler callHandler;

    @Path("/init")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void init(InitParams params) throws ApplicationErrorException {
        LOGGER.info("init");
        doInit(params.getConfig());
    }

    @Path("/shutdown")
    @POST
    public void shutdown() throws ApplicationErrorException {
        LOGGER.info("shutdown");
        if (callManager != null) {
            callManager.doShutdown();
            callManager = null;
        }
        if (callHandler != null) {
            callHandler.handlePollTimeout();
            callHandler = null;
        }
    }

    @Path("/sendInvite")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SendInviteResult sendInvite(SendInviteParams params) throws ApplicationErrorException {
        LOGGER.info("sendInvite");
        if (callManager == null) {
            throw new ApplicationErrorException("Not initialized");
        }
        CallLegData leg = callManager.createCallLegData(params.getLegIndex());
        leg.setSessionId(params.getSessionId());
        callManager.doSendInvite(leg, params.getFromUser(), params.getTo());
        return new SendInviteResult(leg.getCallId());
    }

    @Path("/sendInviteForOutbound")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SendInviteResult sendInviteForOutbound(SendInviteForOutboundParams params) throws ApplicationErrorException {
        LOGGER.info("sendInvite");
        if (callManager == null) {
            throw new ApplicationErrorException("Not initialized");
        }
        CallLegData leg = callManager.createCallLegData(params.getLegIndex());
        leg.setOutbound(true);
        leg.setOutboundTo(params.getOutboundTo());
        leg.setSessionId(params.getSessionId());
        callManager.doSendInvite(leg, params.getFromUser(), params.getTo());
        return new SendInviteResult(leg.getCallId());
    }

    @Path("/waitForInvite")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WaitForInviteResult waitForInvite(WaitForInviteParams params) throws ApplicationErrorException {
        LOGGER.info("sendInvite");
        if (callManager == null) {
            throw new ApplicationErrorException("Not initialized");
        }
        CallLegData leg = callManager.createCallLegData(params.getLegIndex());
        callManager.doWaitForInvite(leg);
        return new WaitForInviteResult(leg.getCallId());
    }

    @Path("/sendBye")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public SendByeResult sendBye(SendByeParams params) throws ApplicationErrorException {
        LOGGER.info("sendBye");
        if (callManager == null) {
            throw new ApplicationErrorException("Not initialized");
        }
        CallLegData leg = callManager.getCall(params.getCallId());
        if (leg == null)
            throw new IllegalArgumentException("no leg matches callId " + params.getCallId());
        callManager.doSendBye(leg);
        return new SendByeResult(leg.getCallId());
    }

    @Path("/play")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PlayResult play(PlayParams params) throws ApplicationErrorException {
        LOGGER.info("play");
        if (callManager == null) {
            throw new ApplicationErrorException("Not initialized");
        }
        CallLegData leg = callManager.getCall(params.getCallId());
        if (leg == null)
            throw new IllegalArgumentException("no leg matches callId " + params.getCallId());
        callManager.doPlay(leg, params.getFileName());
        return new PlayResult(leg.getCallId());
    }

    @Path("/record")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RecordResult record(RecordParams params) throws ApplicationErrorException {
        LOGGER.info("record");
        if (callManager == null) {
            throw new ApplicationErrorException("Not initialized");
        }
        CallLegData leg = callManager.getCall(params.getCallId());
        if (leg == null)
            throw new IllegalArgumentException("no leg matches callId " + params.getCallId());
        callManager.doRecord(leg, params.getFileName());
        return new RecordResult(leg.getCallId());
    }

    @Path("/poll")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public void poll(@QueryParam("timeoutMs") int timeoutMs, @Suspended final AsyncResponse asyncResponse)
            throws ApplicationErrorException {
        LOGGER.info("poll");

        asyncResponse.setTimeoutHandler(new TimeoutHandler() {

            @Override
            public void handleTimeout(AsyncResponse asyncResponse) {
                LOGGER.info("poll: timeout");
                if (callHandler != null) {
                    callHandler.handlePollTimeout();
                } else {
                    asyncResponse.resume(new CallResponse(CallResponse.TIMEOUT));
                }
            }
        });
        asyncResponse.setTimeout(timeoutMs, TimeUnit.MILLISECONDS);

        if (callHandler != null) {
            callHandler.poll(new CallResponseListener() {

                @Override
                public void provide(CallResponse response) {
                    LOGGER.info("poll: response");
                    asyncResponse.resume(response);
                }
            });
        }
    }

    private void doInit(SipCallConfig config) throws ApplicationErrorException {
        if (callManager == null) {
            try {
                callManager = new CallManager(config);
                callHandler = new PolledCallHandler(callManager);
                callManager.init(callHandler);
            } catch (Exception e) {
                callManager = null;
                throw new ApplicationErrorException("init exception", e);
            }

        }
    }
}
