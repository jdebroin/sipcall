package ca.duldeb.sipcall.resources;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.duldeb.sipcall.CallManager;
import ca.duldeb.sipcall.RecordWriter;

public class SipCallWebSocketListener implements WebSocketListener, RecordWriter {
    private final Logger LOGGER = LoggerFactory.getLogger(SipCallWebSocketListener.class);

    private Session outbound;

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Received binary message on WebSocket.");
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        LOGGER.info("WebSocket closed: statusCode=" + statusCode + ", reason=" + reason + ".");
        CallManager.removeAudioDestination(this);
    }

    @Override
    public void onWebSocketConnect(Session session) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("WebSocket to agent connected.");
        this.outbound = session;
        CallManager.addAudioDestination(this);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof java.net.SocketTimeoutException) {
            LOGGER.warn("Timeout on WebSocket.");
        } else if (cause instanceof java.io.EOFException) {
            LOGGER.info("WebSocket disconnected.");
        } else {
            LOGGER.warn("Error on WebSocket.", cause);
        }
    }

    @Override
    public void onWebSocketText(String message) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("onWebSocketText, message='" + message + "'.");
    }

    @Override
    public void write(byte[] packetBuffer, int size) throws IOException {
        outbound.getRemote().sendBytes(ByteBuffer.wrap(packetBuffer, 0, size));
    }

    @Override
    public void close() throws IOException {
    }

}
