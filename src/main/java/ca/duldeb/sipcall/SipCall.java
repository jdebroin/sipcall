package ca.duldeb.sipcall;

public class SipCall {
    public SipCall() throws ApplicationErrorException {
        CallManager callManager = new CallManager(null);
        CallHandler callHandler = new StandaloneCallHandler(callManager);
        callManager.init(callHandler);

        for (int i = 0; i < callManager.getConfig().getNbLegs(); i++) {
            callManager.startCalls(i);
        }
    }

    public static void main(String[] args) {
        System.out.println("main entered");

        try {
            new SipCall();
        } catch (ApplicationErrorException e) {
            System.err.println(e);
        }
    }

}
