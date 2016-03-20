package ca.duldeb.sipcall.resources;

public class SendInviteResult {

    private String callId;

    public SendInviteResult(String callId) {
        this.callId = callId;
    }

    public String getCallId() {
        return callId;
    }

}
