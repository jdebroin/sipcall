package ca.duldeb.sipcall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import ca.duldeb.sipcall.resources.InitParams;
import ca.duldeb.sipcall.resources.SendInviteParams;

public class SipCallResourceConfigTest extends JerseyTest {
    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        ResourceConfig resourceConfig = new SipCallResourceConfig();
        return resourceConfig;
    }
    
    @Test
    public void testInit() {
        String params = "{\"config\":{\"localSipAddress\":\"127.0.0.1\",\"localSipPort\":0,\"localRtpPort\":0}}";
        Response output = target("/sipcall/init")
                .request()
                .post(Entity.json(params));
        assertEquals(204, output.getStatus());
    }
    
    @Test
    public void testInitParams() {
        InitParams params = new InitParams();
        SipCallConfig config = new SipCallConfig();
        config.setLocalSipAddress("127.0.0.1");
        config.setLocalSipPort(0);
        config.setLocalRtpPort(0);
        params.setConfig(config);
        Response output = target("/sipcall/init")
                .request()
                .post(Entity.json(params));
        assertEquals(204, output.getStatus());
    }
    
    @Test
    public void testPoll() {
        Response output = target("/sipcall/poll")
                .queryParam("timeoutMs", 40)
                .request()
                .get();
        assertEquals(200, output.getStatus());
        String response = output.readEntity(String.class);
        assertEquals("{\"response\":\"TIMEOUT\",\"callId\":null,\"reason\":null,\"code\":200}", response);
    }
    
    @Test
    public void testSendInvite() {
        SendInviteParams params = new SendInviteParams();
        Response output = target("/sipcall/sendInvite")
                .request()
                .post(Entity.json(params));
        assertEquals(500, output.getStatus());
        String response = output.readEntity(String.class);
        assertTrue(response.contains("message"));
        assertTrue(response.contains("Error sending INVITE"));
    }
}