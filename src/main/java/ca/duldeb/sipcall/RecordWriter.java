package ca.duldeb.sipcall;

import java.io.IOException;

public interface RecordWriter {
    void write(CallLegData leg, byte[] packetBuffer, int size) throws IOException;

}
