package ca.duldeb.sipcall;

import java.net.URISyntaxException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.duldeb.sipcall.resources.SipCallWebSocketServlet;

public class SipCallServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SipCallServer.class);
            
    public static void main(String[] args) throws URISyntaxException {

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        int port = 8084;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        ResourceConfig resourceConfig = new SipCallResourceConfig();
        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletHolder servletHolder = new ServletHolder(servletContainer);
        context.addServlet(servletHolder, "/ws/*");

        ServletHolder staticServletHolder = new ServletHolder("default", DefaultServlet.class);
        staticServletHolder.setInitParameter("dirAllowed", "true");
        String staticResourceBase = SipCallServer.class.getResource("/web").toURI().toString();
        staticServletHolder.setInitParameter("resourceBase", staticResourceBase);
        context.addServlet(staticServletHolder, "/*");

        SipCallWebSocketServlet webSocketServlet = new SipCallWebSocketServlet();
        ServletHolder webSocketServletHolder = new ServletHolder(webSocketServlet);
        context.addServlet(webSocketServletHolder, "/socket");

        try {
            server.start();
            System.out.println("SipCallServer HTTP server listening on port " + port);
            LOGGER.info("SipCallServer HTTP server listening on port " + port);
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Error running SipCallServer", e);
        }
    }

}
