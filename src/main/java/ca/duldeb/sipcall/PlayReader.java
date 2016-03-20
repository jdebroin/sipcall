package ca.duldeb.sipcall;

public interface PlayReader {
    int read(CallLegData leg, byte[] packetBuffer, int totalBytesRead, int bytesRemaining);

}
