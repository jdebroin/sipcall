package ca.duldeb.sipcall;

public class SipCallHangup implements SipCallTask {

    @Override
    public boolean doIt(CallManager callManager, CallLegData leg) {
        return callManager.doSendBye(leg);
    }

}
