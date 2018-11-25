package ca.duldeb.sipcall;

import org.apache.cxf.common.logging.Slf4jLogger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class SipCallResourceConfig extends ResourceConfig {
    public SipCallResourceConfig() {
        // map JUL logs to SLF4J
        java.util.logging.Logger jerseyLogger = new Slf4jLogger(this.getClass().getName(), null);
        register(new LoggingFeature(jerseyLogger, java.util.logging.Level.INFO, LoggingFeature.Verbosity.HEADERS_ONLY, 200));
        register(JacksonFeature.class);
        packages("ca.duldeb.sipcall.resources");
    }

}
