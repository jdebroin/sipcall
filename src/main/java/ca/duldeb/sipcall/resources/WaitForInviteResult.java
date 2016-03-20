package ca.duldeb.sipcall.resources;

public class WaitForInviteResult {

    private String callId;

    public WaitForInviteResult(String callId) {
        this.callId = callId;
    }

    public String getCallId() {
        return callId;
    }

}
