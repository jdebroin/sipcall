package ca.duldeb.sipcall;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferPlayReader implements PlayReader {
    private final Logger LOGGER = LoggerFactory.getLogger(BufferPlayReader.class);

    @Override
    public int read(CallLegData leg, byte[] packetBuffer, int totalBytesRead, int bytesRemaining) {
        int bytesRead;
        if (leg.getInput() != null) {
            try {
                bytesRead = leg.getInput().read(packetBuffer, totalBytesRead, bytesRemaining);
                if (bytesRead < 0) {
                    // end of file
                    leg.setInput(null);
                }
            } catch (IOException e) {
                LOGGER.error("error reading input file for playback: " + e.getMessage(), e);
                leg.setInput(null);
                bytesRead = 0;
            }
        } else {
            bytesRead = 0;
        }
        return bytesRead;
    }

}
