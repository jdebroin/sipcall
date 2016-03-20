package ca.duldeb.sipcall.resources;

public class RecordResult {

    private String callId;

    public RecordResult(String callId) {
        this.callId = callId;
    }

    public String getCallId() {
        return callId;
    }

}
