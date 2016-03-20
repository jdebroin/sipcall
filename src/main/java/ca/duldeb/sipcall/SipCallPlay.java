package ca.duldeb.sipcall;

public class SipCallPlay implements SipCallTask {

    private String fileName;

    public SipCallPlay(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean doIt(CallManager callManager, CallLegData leg) {
        return callManager.doPlay(leg, fileName);
    }

}
