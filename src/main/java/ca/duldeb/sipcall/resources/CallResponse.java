package ca.duldeb.sipcall.resources;

public class CallResponse {

    public static final String INVITE_OK = "INVITE_OK";

    public static final String CALL_FINISHED = "CALL_FINISHED";

    public static final String PAUSE_DONE = "PAUSE_DONE";

    public static final String PLAY_DONE = "PLAY_DONE";

    public static final String TIMEOUT = "TIMEOUT";

    private String response;
    private String callId;
    private String reason;
    private int code;

    public CallResponse(String response) {
        this.response = response;
        this.callId = null;
        this.reason = null;
        this.code = 200;
    }

    public CallResponse(String response, String callId) {
        this.response = response;
        this.callId = callId;
        this.reason = null;
        this.code = 200;
    }

    public CallResponse(String response, String callId, String reason, int code) {
        this.response = response;
        this.callId = callId;
        this.reason = reason;
        this.code = code;
    }

    public String getResponse() {
        return response;
    }

    public String getCallId() {
        return callId;
    }

    public String getReason() {
        return reason;
    }

    public int getCode() {
        return code;
    }

}
