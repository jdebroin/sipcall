package ca.duldeb.sipcall;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.IOException;

import org.apache.commons.logging.Log;

public class BufferPlayReader implements PlayReader {
    private static final Log LOGGER = getLog(BufferPlayReader.class);

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
