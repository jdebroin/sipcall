package ca.duldeb.sipcall;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;

import javax.sip.Dialog;

import com.biasedbit.efflux.session.RtpSession;

public class CallLegData {
    private int legIndex;
    private int callIndex;
    private String callId;
    private CallHandler callHandler;
    private boolean outbound;
    private String outboundTo;
    private String fromUser;
    private String to;
    private String sessionId;
    private byte[] localSdp;
    private int localRtpPort;
    private String remoteRtpAddress;
    private int remoteRtpPort;
    private int payloadType;
    private int packetDuration;
    private int packetSize;
    private InputStream input;
    private Queue<SipCallTask> tasks = new LinkedList<SipCallTask>();

    private Dialog sipDialog;
    private RtpSession rtpSession;
    private long timestamp;
    private boolean playing;
    private ScheduledFuture<?> packetSenderHandle;
    private PlayReader playReader;
    private List<RecordWriter> recordWriters = new ArrayList<RecordWriter>();
    
    private int dtmfPayloadType;
    private Queue<DtmfPayload> dtmfs = new LinkedList<DtmfPayload>();
    public boolean hasRtpSession;

    public CallLegData(int legIndex, int callIndex, CallHandler callHandler, PlayReader playReader) {
        this.legIndex = legIndex;
        this.callIndex = callIndex;
        this.callId = legIndex + "." + callIndex;
        this.callHandler = callHandler;
        this.playReader = playReader;

        this.outbound = false;
        this.payloadType = 0;
        this.packetDuration = 30;
        this.timestamp = 0;
        this.playing = false;
        
        this.setDtmfPayloadType(101);
        
        this.hasRtpSession = false;
    }

    public int getLegIndex() {
        return legIndex;
    }

    public int getCallIndex() {
        return callIndex;
    }

    public String getCallId() {
        return callId;
    }

    public CallHandler getCallHandler() {
        return callHandler;
    }

    public String getName() {
        return callId;
    }

    public boolean isOutbound() {
        return outbound;
    }

    public void setOutbound(boolean outbound) {
        this.outbound = outbound;
    }

    public String getOutboundTo() {
        return outboundTo;
    }

    public void setOutboundTo(String outboundTo) {
        this.outboundTo = outboundTo;
    }

    public int getLocalRtpPort() {
        return localRtpPort;
    }

    public void setLocalRtpPort(int localRtpPort) {
        this.localRtpPort = localRtpPort;
    }

    public String getRemoteRtpAddress() {
        return remoteRtpAddress;
    }

    public void setRemoteRtpAddress(String remoteRtpAddress) {
        this.remoteRtpAddress = remoteRtpAddress;
    }

    public int getRemoteRtpPort() {
        return remoteRtpPort;
    }

    public void setRemoteRtpPort(int remoteRtpPort) {
        this.remoteRtpPort = remoteRtpPort;
    }

    public int getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(int payloadType) {
        this.payloadType = payloadType;
    }

    public int getPacketDuration() {
        return packetDuration;
    }

    public void setPacketDuration(int packetDuration) {
        this.packetDuration = packetDuration;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public InputStream getInput() {
        return input;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    public Queue<SipCallTask> getTasks() {
        return tasks;
    }

    public void setTasks(Queue<SipCallTask> tasks) {
        this.tasks = tasks;
    }

    public Dialog getSipDialog() {
        return sipDialog;
    }

    public void setSipDialog(Dialog sipDialog) {
        this.sipDialog = sipDialog;
    }

    public RtpSession getRtpSession() {
        return rtpSession;
    }

    public void setRtpSession(RtpSession rtpSession) {
        this.rtpSession = rtpSession;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public ScheduledFuture<?> getPacketSenderHandle() {
        return packetSenderHandle;
    }

    public void setPacketSenderHandle(ScheduledFuture<?> packetSenderHandle) {
        this.packetSenderHandle = packetSenderHandle;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public byte[] getLocalSdp() {
        return localSdp;
    }

    public void setLocalSdp(byte[] localSdp) {
        this.localSdp = localSdp;
    }

    public PlayReader getPlayReader() {
        return playReader;
    }

    public void writeDataPacket(byte[] packetBuffer, int size) throws IOException {
        for (RecordWriter recordWriter : recordWriters) {
            recordWriter.write(packetBuffer, size);
        }
    }

    public void addRecordWriter(RecordWriter recordWriter) {
        recordWriters.add(recordWriter);
    }

    public void removeRecordWriters() throws IOException {
        for (RecordWriter recordWriter : recordWriters) {
            recordWriter.close();
        }
    }

    public void pushDtmf(String dtmf) {
        for (int i = 0; i < dtmf.length(); i++) {
            this.dtmfs.add(new DtmfPayload(true, dtmf.charAt(i), false, 160));
            this.dtmfs.add(new DtmfPayload(false, dtmf.charAt(i), false, 320));
            this.dtmfs.add(new DtmfPayload(false, dtmf.charAt(i), false, 480));
            this.dtmfs.add(new DtmfPayload(false, dtmf.charAt(i), true, 640));
            this.dtmfs.add(new DtmfPayload(false, dtmf.charAt(i), true, 800));
        }
    }

    public boolean hasDtmf() {
        return (!dtmfs.isEmpty());
    }
    
    public DtmfPayload popDtmf() {
        return dtmfs.poll();
    }

    public int getDtmfPayloadType() {
        return dtmfPayloadType;
    }

    public void setDtmfPayloadType(int dtmfPayloadType) {
        this.dtmfPayloadType = dtmfPayloadType;
    }

    public boolean beginRtpSession() {
        if (hasRtpSession) {
            return false;
        }
        hasRtpSession = true;
        return true;
    }

    public void endRtpSession() {
        hasRtpSession = false;
    }
}
