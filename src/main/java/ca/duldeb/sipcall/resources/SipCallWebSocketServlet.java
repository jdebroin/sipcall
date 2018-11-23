package ca.duldeb.sipcall.resources;

import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

//@WebServlet(name = "SipCall WebSocket Servlet", urlPatterns = { "/socket" })
@WebServlet(name = "SipCall WebSocket Servlet")
public class SipCallWebSocketServlet extends WebSocketServlet {

    private static final long serialVersionUID = 1288418855762486269L;
    private long idleTimeoutMs = 10000;

    public SipCallWebSocketServlet() {
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(idleTimeoutMs);
        factory.setCreator(new SipCallWebSocketCreator());
    }
}
