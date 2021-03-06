package ca.duldeb.sipcall;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.session.RtpSession;

public class RtpLayer {
    private final Logger LOGGER = LoggerFactory.getLogger(RtpLayer.class);

    private String localAddress;
    private int defaultLocalPort;

    protected final HashedWheelTimer timer;
    private final ScheduledExecutorService scheduler;

    final int maximumPacketSize = 30 * 8;
    private byte[] packetBuffer = new byte[maximumPacketSize];
    private byte[] silenceBuffer = new byte[maximumPacketSize];

    public RtpLayer(String localAddress, int defaultLocalPort, HashedWheelTimer timer, ScheduledExecutorService scheduler) {
        this.localAddress = localAddress;
        this.defaultLocalPort = defaultLocalPort;
        this.timer = timer;
        this.scheduler = scheduler;
    }

    public void createSession(CallLegData leg) throws ApplicationErrorException {
//        String remoteAddress = leg.getRemoteRtpAddress();
//        int remotePort = leg.getRemoteRtpPort();
        Collection<Integer> payloadTypes = new HashSet<Integer>();
        payloadTypes.add(leg.getPayloadType());
        payloadTypes.add(leg.getDtmfPayloadType());

        try {
            boolean ok = false;
            int maxAttempts = 1;
            int localPort = defaultLocalPort;
            if (localPort == 0) {
                maxAttempts = 100;
                localPort = 49152 + (int)(Math.random() * (65535 - 49152));
            } else {
                maxAttempts = 1;
            }
            int attempt = 0;
            while (!ok) {
                RtpParticipant local = RtpParticipant.createReceiver(new RtpParticipantInfo(1), localAddress, localPort,
                        localPort + 1);
                //RtpParticipant remote = RtpParticipant.createReceiver(new RtpParticipantInfo(2), remoteAddress, remotePort, remotePort + 1);
                //final SingleParticipantSession session = new SingleParticipantSession("Session1", payloadType, local, remote, timer);
                final SipParticipantSession session = new SipParticipantSession("Session1", payloadTypes, local, timer, null);
                
                leg.setRtpSession(session);
                attempt++;
                ok = session.init();
                if (ok) {
                    leg.setLocalRtpPort(localPort);
                    LOGGER.info("RTP session initialized on local port " + localPort);
                }
                else {
                    if (attempt < maxAttempts) {
                        LOGGER.info("Error initializing RTP session on port " + localPort);
                        localPort += 2;
                        LOGGER.info("Trying another port: " + localPort);
                    } else {
                        throw new ApplicationErrorException("Error initializing RTP session on port " + localPort);            
                    }
                }
                session.addDataListener(new RtpDataListener(leg));

                ok = true;
            }
        } catch (IllegalArgumentException e) {
            throw new ApplicationErrorException("Error setting up RTP session", e);            
        }
        

        leg.setTimestamp(0);

        for (int i = 0; i < silenceBuffer.length; i++) {
            silenceBuffer[i] = (byte) 0xff;
        }

        leg.setPacketSize(leg.getPacketDuration() * 8);
    }

    public void connectSession(CallLegData leg) {
        String remoteAddress = leg.getRemoteRtpAddress();
        int remotePort = leg.getRemoteRtpPort();
        RtpSession session = leg.getRtpSession();
        RtpParticipant remoteParticipant = RtpParticipant.createReceiver(new RtpParticipantInfo(2), remoteAddress, remotePort, remotePort + 1);
        session.addReceiver(remoteParticipant);
    }
    
    public void play(final CallLegData leg) {

        final Runnable packetSender = new Runnable() {

            public void run() {
                try {
                    long timestamp = leg.getTimestamp();
                    int packetSize = leg.getPacketSize();
    
                    DtmfPayload dtmf = leg.popDtmf();
                    if (dtmf != null) {
                        sendDtmfPacket(leg.getRtpSession(), leg.getDtmfPayloadType(), dtmf, timestamp);
                        // timestamp is the same for all copies of the dtmf
    
                    } else {
                        int totalBytesRead = 0;
                        while (totalBytesRead < packetSize) {
                            int bytesRemaining = packetSize - totalBytesRead;
                            // input.read() returns -1, 0, or more :
                            int bytesRead = leg.getPlayReader().read(leg, packetBuffer, totalBytesRead, bytesRemaining);
                            if (bytesRead > 0) {
                                totalBytesRead = totalBytesRead + bytesRead;
                            } else {
                                break;
                            }
                        }
        
                        byte[] data;
                        boolean marker;
                        if (totalBytesRead > 0) {
                            if (totalBytesRead < packetSize) {
                                System.arraycopy(silenceBuffer, totalBytesRead, packetBuffer, totalBytesRead, packetSize
                                        - totalBytesRead);
                            }
                            data = packetBuffer;
                            if (!leg.isPlaying()) {
                                marker = true;
                                leg.setPlaying(true);
                                LOGGER.debug("play started");
                            } else {
                                marker = false;
                            }
                            // LOGGER.debug("Num bytes read: " + totalBytesRead);
                        } else {
                            data = silenceBuffer;
                            if (leg.isPlaying()) {
                                leg.setPlaying(false);
                                LOGGER.debug("play done");
                                leg.getCallHandler().handlePlayDone(leg);
                            }
                            marker = false;
                        }
        
                        sendPacket(leg.getRtpSession(), data, timestamp, marker);
                        timestamp += packetSize;
                        leg.setTimestamp(timestamp);
                    }
                    
                } catch (Exception e) {
                    LOGGER.error("play packet sender scheduler run exception", e);
                }
            }
        };

        final ScheduledFuture<?> packetSenderHandle = scheduler.scheduleAtFixedRate(packetSender, 0,
                leg.getPacketDuration(), TimeUnit.MILLISECONDS);
        leg.setPacketSenderHandle(packetSenderHandle);

        LOGGER.debug("play packet sender scheduler started");
        
    }

    public void stopPlay(CallLegData leg) {
        if (leg.getPacketSenderHandle() != null)
            leg.getPacketSenderHandle().cancel(true);
    }

    private boolean sendPacket(RtpSession session, byte[] data, long timestamp, boolean marker) {
        DataPacket packet = new DataPacket();
        packet.setTimestamp(timestamp);
        packet.setData(data);
        packet.setMarker(marker);

        return session.sendDataPacket(packet);
    }

    private boolean sendDtmfPacket(RtpSession session, int payloadType, DtmfPayload payload, long timestamp) {
        DataPacket packet = new DataPacket();
        packet.setTimestamp(timestamp);
        packet.setPayloadType(payloadType);
        byte[] data = payload.toBytes();
        packet.setData(data);
        packet.setMarker(payload.getMarker());

        LOGGER.debug("send dtmf packet " + packet);
        return session.sendDataPacket(packet);
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public void terminateSession(CallLegData leg) {
        RtpSession session = leg.getRtpSession();
        if (session != null) {
            session.terminate();
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
        timer.stop();
    }
}
