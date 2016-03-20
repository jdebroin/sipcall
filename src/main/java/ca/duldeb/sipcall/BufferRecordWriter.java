package ca.duldeb.sipcall;

import java.io.IOException;

public class BufferRecordWriter implements RecordWriter {

    @Override
    public void write(CallLegData leg, byte[] packetBuffer, int size) throws IOException {
        if (leg.getOutput() != null) {
            int offset = 0;
            leg.getOutput().write(packetBuffer, offset, size);
        }
    }

}
