package ca.duldeb.sipcall.resources;

public class SendInviteForOutboundParams {
    private int legIndex;
    private String fromUser;
    private String to;
    private String sessionId;
    private String outboundTo;

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public int getLegIndex() {
        return legIndex;
    }

    public void setLegIndex(int legIndex) {
        this.legIndex = legIndex;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getOutboundTo() {
        return outboundTo;
    }

    public void setOutboundTo(String outboundTo) {
        this.outboundTo = outboundTo;
    }
}
