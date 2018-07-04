package ca.duldeb.sipcall;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.TransportNotSupportedException;

import org.apache.commons.logging.Log;
import org.jboss.netty.util.HashedWheelTimer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CallManager {
    private static final Log LOGGER = getLog(SipCall.class);

    private static RecordWriter audioDestination;

    private SipCallConfig config;
    private SipLayer sipLayer;
    private HashedWheelTimer timer;
    private RtpLayer rtpLayer;
    private ScheduledExecutorService scheduler;

    private CallHandler callHandler;
    private PlayReader playReader;

    private AtomicInteger callIndex = new AtomicInteger(0);
    private ConcurrentHashMap<String, CallLegData> calls = new ConcurrentHashMap<String, CallLegData>();

    public CallManager(SipCallConfig extraConfig) throws ApplicationErrorException {
        // try {
        // this.myAddress = Inet4Address.getLocalHost().getHostAddress();
        // } catch (UnknownHostException e) {
        // throw new ApplicationErrorException("Error creating SipLayer", e);
        // }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        String configFileName = "config.json";
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(configFileName);

        try {
            config = mapper.readValue(is, SipCallConfig.class);
        } catch (JsonParseException e) {
            throw new ApplicationErrorException("Error reading " + configFileName, e);
        } catch (JsonMappingException e) {
            throw new ApplicationErrorException("Error reading " + configFileName, e);
        } catch (IOException e) {
            throw new ApplicationErrorException("Error reading " + configFileName, e);
        }

        if (extraConfig != null) {
            config.merge(extraConfig);
        }
    }

    public void init(CallHandler callHandler) throws ApplicationErrorException {
        this.callHandler = callHandler;
        this.playReader = new BufferPlayReader();

        try {
            if (config.getLocalSipAddress() == null || config.getLocalSipAddress() == "") {
                InetAddress addr = Inet4Address.getLocalHost();
                config.setLocalSipAddress(addr.getHostAddress());
            }
        } catch (UnknownHostException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ApplicationErrorException("Error creating SipLayer", e);
        }

        LOGGER.info("localSipAddress=" + config.getLocalSipAddress());
        try {
            sipLayer = new SipLayer(config.getLocalSipAddress(), config.getLocalSipPort());
            LOGGER.info("SipLayer listening on " + config.getLocalSipAddress() + ":" + config.getLocalSipPort());
        } catch (PeerUnavailableException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ApplicationErrorException("Error creating SipLayer", e);
        } catch (TransportNotSupportedException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ApplicationErrorException("Error creating SipLayer", e);
        } catch (ObjectInUseException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ApplicationErrorException("Error creating SipLayer", e);
        } catch (InvalidArgumentException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ApplicationErrorException("Error creating SipLayer", e);
        } catch (TooManyListenersException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ApplicationErrorException("Error creating SipLayer", e);
        }

        timer = new HashedWheelTimer(1, TimeUnit.MILLISECONDS);
        scheduler = Executors.newScheduledThreadPool(1);
        rtpLayer = new RtpLayer(config.getLocalSipAddress(), config.getLocalRtpPort(), timer, scheduler);
    }

    public CallHandler getCallHandler() {
        return callHandler;
    }

    public void startCalls(int legIndex) throws ApplicationErrorException {
        CallLegData leg = createCallLegData(legIndex);
        doSendInvite(leg);
    }

    public SipCallConfig getConfig() {
        return config;
    }

    public CallLegData createCallLegData(int legIndex) {
        return createCallLegData(legIndex, callIndex.incrementAndGet());
    }

    public CallLegData createCallLegData(int legIndex, int callIndex) {
        CallLegData leg = new CallLegData(legIndex, callIndex, callHandler, playReader);
        calls.put(leg.getCallId(), leg);
        LOGGER.debug(leg.getName() + " created");
        return leg;
    }

    public boolean doSendInvite(CallLegData leg) throws ApplicationErrorException {
        return doSendInvite(leg, null, null);
    }

    public boolean doSendInvite(CallLegData leg, String fromUser, String to) throws ApplicationErrorException {

        if (fromUser != null) {
            leg.setFromUser(fromUser);
        } else {
            leg.setFromUser(config.getLocalUserName());
        }
        if (to != null) {
            leg.setTo(to);
        } else {
            leg.setTo(config.getTo());
        }

        createRtpSession(leg);

        String sdpData;
        sdpData = "v=0\r\n" + "o=4855 13760799956958020 13760799956958020 IN IP4 " + config.getLocalSipAddress()
                + "\r\n" + "s=mysession session\r\n" + "p=+46 8 52018010\r\n" + "c=IN IP4 "
                + config.getLocalRtpAddress() + "\r\n" + "t=0 0\r\n" + "m=audio " + leg.getLocalRtpPort()
                + " RTP/AVP 0\r\n" + "a=rtpmap:0 PCMU/8000\r\n";
        byte[] sdp = sdpData.getBytes();
        leg.setLocalSdp(sdp);

        try {
            LOGGER.info(leg.getName() + " sending INVITE to " + leg.getTo());
            sipLayer.sendInvite(leg);
        } catch (ParseException e) {
            throw new ApplicationErrorException("Error sending INVITE", e);
        } catch (InvalidArgumentException e) {
            throw new ApplicationErrorException("Error sending INVITE", e);
        } catch (SipException e) {
            throw new ApplicationErrorException("Error sending INVITE", e);
        }
        return true;
    }

    public void doWaitForInvite(CallLegData leg) throws ApplicationErrorException {
        LOGGER.info(leg.getName() + " sending INVITE to " + leg.getTo());

        createRtpSession(leg);

        String sdpData;
        sdpData = "v=0\r\n" + "o=4855 13760799956958020 13760799956958020 IN IP4 " + config.getLocalSipAddress()
                + "\r\n" + "s=mysession session\r\n" + "p=+46 8 52018010\r\n" + "c=IN IP4 "
                + config.getLocalRtpAddress() + "\r\n" + "t=0 0\r\n" + "m=audio " + leg.getLocalRtpPort()
                + " RTP/AVP 0\r\n" + "a=rtpmap:0 PCMU/8000\r\n";
        byte[] sdp = sdpData.getBytes();
        leg.setLocalSdp(sdp);

        sipLayer.waitForInvite(leg);
    }

    public boolean doSendBye(CallLegData leg) {
        cleanupRtp(leg);
        LOGGER.info(leg.getName() + " sending BYE");
        sipLayer.sendBye(leg);
        return true;
    }

    public void doShutdown() {
        LOGGER.info("shutting down");
        if (rtpLayer != null)
            rtpLayer.shutdown();
        if (sipLayer != null)
            sipLayer.shutdown();
    }

    public boolean doPause(final CallLegData leg, int delay) {
        LOGGER.debug(leg.getName() + " pausing for " + delay + " seconds");
        final Runnable pause = new Runnable() {
            public void run() {
                callHandler.handlePauseDone(leg);
            }
        };
        TimeUnit unit = TimeUnit.SECONDS;
        scheduler.schedule(pause, delay, unit);
        return true;
    }

    public int read(CallLegData leg, byte[] packetBuffer, int totalBytesRead, int bytesRemaining) {
        int bytesRead;
        if (leg.getInput() != null) {
            try {
                bytesRead = leg.getInput().read(packetBuffer, totalBytesRead, bytesRemaining);
                if (bytesRead < 0) {
                    // end of file
                    endPlay(leg);
                }
            } catch (IOException e) {
                LOGGER.error("error reading input file for playback: " + e.getMessage(), e);
                endPlay(leg);
                bytesRead = 0;
            }
        } else {
            bytesRead = 0;
        }
        return bytesRead;
    }

    public boolean doPlay(CallLegData leg, String fileName) throws ApplicationErrorException {
        endPlay(leg);
        LOGGER.debug(leg.getName() + " playing from " + fileName);
        //leg.setInput(new BufferedInputStream(this.getClass().getClassLoader().getResourceAsStream(fileName)));
        try {
            leg.setInput(new BufferedInputStream(new FileInputStream(fileName)));
        } catch (FileNotFoundException e) {
            throw new ApplicationErrorException(leg.getName() + " error playing " + fileName, e);
        }
        return true;
    }

    public void doRecord(CallLegData leg, String fileName) throws ApplicationErrorException {
        endRecord(leg);
        if (audioDestination != null) {
            leg.addRecordWriter(audioDestination);
        }
        LOGGER.debug(leg.getName() + " recording to " + fileName);
        try {
            leg.addRecordWriter(new BufferRecordWriter(fileName));
        } catch (FileNotFoundException e) {
            throw new ApplicationErrorException(leg.getName() + " error recording to " + fileName, e);
        }
    }

    public void createRtpSession(CallLegData leg) throws ApplicationErrorException {
        rtpLayer.createSession(leg);
    }

    public void connectRtpSession(CallLegData leg) throws ApplicationErrorException {
        rtpLayer.connectSession(leg);
        rtpLayer.play(leg);
    }

    public void cleanupRtp(CallLegData leg) {
        endPlay(leg);
        rtpLayer.stopPlay(leg);
        endRecord(leg);
        rtpLayer.terminateSession(leg);
    }

    private void endPlay(CallLegData leg) {
        InputStream in = leg.getInput();
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
            leg.setInput(null);
        }
    }

    private void endRecord(CallLegData leg) {
        try {
            leg.removeRecordWriters();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public CallLegData getCall(String callId) {
        return calls.get(callId);
    }

    public static void addAudioDestination(RecordWriter audioDestination) {
        CallManager.audioDestination = audioDestination;
    }

    public static void removeAudioDestination(RecordWriter audioDestination) {
        CallManager.audioDestination = null;
    }

}
