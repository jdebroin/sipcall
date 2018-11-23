package ca.duldeb.sipcall;

public class DtmfPayload {
    private boolean marker;
    private char dtmf;
    private boolean end;
    private int duration;
    
    public DtmfPayload(boolean marker, char dtmf, boolean end, int duration) {
        this.marker = marker;
        this.dtmf = dtmf;
        this.end = end;
        this.duration = duration;
    }

    public boolean getMarker() {
        return marker;
    }

    public byte[] toBytes() {
        byte[] data = new byte[4];
        if (dtmf >= '0' && dtmf <='9') {
            data[0] = (byte) (dtmf - '0');
        } else if (dtmf == '*') {
            data[0] = 10;
        } else if (dtmf == '#') {
            data[0] = 10;
        } else if (dtmf >= 'A' && dtmf <= 'D') {
            data[0] = (byte) (dtmf - 'A' + 12);
        } else if (dtmf == '&') {
            data[0] = 16;
        } else {
            data[0] = 0;
        }
        int e = (end ? 1 : 0);
        // R = 0
        int volume = 10;
        data[1] = (byte) ((e << 7) | volume);
        data[2] = (byte) (duration / 256);
        data[3] = (byte) (duration % 256);
        return data;
    }
}
