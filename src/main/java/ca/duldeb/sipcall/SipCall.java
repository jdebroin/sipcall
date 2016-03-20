package ca.duldeb.sipcall;

import static org.apache.commons.logging.LogFactory.getLog;

import org.apache.commons.logging.Log;

public class SipCall {
    private static final Log LOGGER = getLog(SipCall.class);

    public SipCall() throws ApplicationErrorException {
        CallManager callManager = new CallManager(null);
        CallHandler callHandler = new StandaloneCallHandler(callManager);
        callManager.init(callHandler);

        for (int i = 0; i < callManager.getConfig().getNbLegs(); i++) {
            callManager.startCalls(i);
        }
    }

    public static void main(String[] args) {
        LOGGER.info("main entered");

        try {
            new SipCall();
        } catch (ApplicationErrorException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
