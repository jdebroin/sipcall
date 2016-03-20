package ca.duldeb.sipcall.resources;

public class SendByeResult {

    private String callId;

    public SendByeResult(String callId) {
        this.callId = callId;
    }

    public String getCallId() {
        return callId;
    }

}
