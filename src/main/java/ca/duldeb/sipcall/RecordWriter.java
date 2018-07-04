package ca.duldeb.sipcall;

import java.io.IOException;

public interface RecordWriter {
    void write(byte[] packetBuffer, int size) throws IOException;

    void close() throws IOException;

}
