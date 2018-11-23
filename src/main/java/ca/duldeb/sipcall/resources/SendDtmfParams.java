package ca.duldeb.sipcall.resources;

public class SendDtmfParams {
    private String callId;
    private String dtmf;
    
    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getDtmf() {
        return dtmf;
    }

    public void setDtmf(String dtmf) {
        this.dtmf = dtmf;
    }

}
