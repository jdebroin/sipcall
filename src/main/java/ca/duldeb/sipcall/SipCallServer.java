package ca.duldeb.sipcall;

import java.net.URISyntaxException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import ca.duldeb.sipcall.resources.SipCallWebSocketServlet;

public class SipCallServer {

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

        ServletContainer servletContainer = new ServletContainer();
        ServletHolder servletHolder = new ServletHolder(servletContainer);
        //servletHolder.setInitParameter(org.glassfish.jersey.server.ServerProperties.PROVIDER_PACKAGES,
        //        "ca.duldeb.sipcall.resources");
        servletHolder.setInitParameter(org.glassfish.jersey.server.ServerProperties.PROVIDER_CLASSNAMES,
                  "ca.duldeb.sipcall.resources.SipCallResource, ca.duldeb.sipcall.resources.ApplicationErrorExceptionMapper");
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
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
