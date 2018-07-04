package ca.duldeb.sipcall;

public class SipCallRecord implements SipCallTask {

    private String fileName;

    public SipCallRecord(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean doIt(CallManager callManager, CallLegData leg) throws ApplicationErrorException {
        callManager.doRecord(leg, fileName);
        return false; // Don't wait
    }

}
