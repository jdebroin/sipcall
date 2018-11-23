package ca.duldeb.sipcall.resources;

public class SendDtmfResult {

    private String callId;

    public SendDtmfResult(String callId) {
        this.callId = callId;
    }

    public String getCallId() {
        return callId;
    }

}
