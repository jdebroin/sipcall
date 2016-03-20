package ca.duldeb.sipcall;

public class SipCallPause implements SipCallTask {

    private int delay;

    public SipCallPause(int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public boolean doIt(CallManager callManager, CallLegData leg) {
        return callManager.doPause(leg, delay);
    }

}
