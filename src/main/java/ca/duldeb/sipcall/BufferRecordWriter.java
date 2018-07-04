package ca.duldeb.sipcall;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BufferRecordWriter implements RecordWriter {

    private OutputStream output;

    public BufferRecordWriter(String fileName) throws FileNotFoundException {
        output = new BufferedOutputStream(new FileOutputStream(fileName));
    }

    @Override
    public void write(byte[] packetBuffer, int size) throws IOException {
        if (output != null) {
            int offset = 0;
            output.write(packetBuffer, offset, size);
        }
    }

    @Override
    public void close() throws IOException {
        if (output != null) {
            output.close();
            output = null;
        }
    }

}
