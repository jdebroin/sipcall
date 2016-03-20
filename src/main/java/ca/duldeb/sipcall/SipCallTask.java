package ca.duldeb.sipcall;

interface SipCallTask {
    boolean doIt(CallManager callManager, CallLegData leg);
}
