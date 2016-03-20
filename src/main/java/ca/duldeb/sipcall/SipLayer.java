package ca.duldeb.sipcall;

import static org.apache.commons.logging.LogFactory.getLog;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.nist.javax.sip.Utils;
import gov.nist.javax.sip.UtilsExt;

import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;

public class SipLayer implements SipListener {
    private static final Log LOGGER = getLog(SipLayer.class);

    private String ip;
    private int port;

    private SipStack sipStack;
    private SipFactory sipFactory;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    private SipProvider sipProvider;

    private SdpFactory sdpFactory;
    private UtilsExt utilsExt;

    private ConcurrentLinkedQueue<CallLegData> waitingForInviteLegs = new ConcurrentLinkedQueue<CallLegData>();

    public SipLayer(String ip, int port) throws PeerUnavailableException, TransportNotSupportedException,
            InvalidArgumentException, ObjectInUseException, TooManyListenersException {
        this.ip = ip;
        this.port = port;

        sipFactory = SipFactory.getInstance();

        sipFactory.setPathName("gov.nist");

        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "SipCall");
        properties.setProperty("javax.sip.IP_ADDRESS", this.ip);
        sipStack = sipFactory.createSipStack(properties);

        headerFactory = sipFactory.createHeaderFactory();

        addressFactory = sipFactory.createAddressFactory();

        messageFactory = sipFactory.createMessageFactory();
        
        // ListeningPoint tcp = sipStack.createListeningPoint(port, "tcp");

        ListeningPoint udp = null;
        int maxAttempts = 1;
        if (this.port == 0) {
            maxAttempts = 100;
            this.port = 49152 + (int) (Math.random() * ((65535 - 49152) + 1));
        } else {
            maxAttempts = 1;
        }
        int i = 0;
        while (udp == null) {
            try {
                i++;
                udp = sipStack.createListeningPoint(this.port, "udp");
            } catch (InvalidArgumentException e) {
                if (i < maxAttempts) {
                    LOGGER.info("Error creating listening point on port " + this.port + ": " + e.getMessage());
                    this.port++;
                    udp = null;
                    LOGGER.info("Trying another port: " + this.port);
                } else {
                    throw e;
                }
            }
        }

        // sipProvider = sipStack.createSipProvider(tcp);
        // sipProvider.addSipListener(this);

        sipProvider = sipStack.createSipProvider(udp);
        sipProvider.addSipListener(this);

        sdpFactory = SdpFactory.getInstance();

        utilsExt = new Utils();

        LOGGER.info("SIP stack will listen on " + this.ip + ":" + this.port);
    }

    public void shutdown() {
        for (Iterator<?> itr = sipStack.getSipProviders(); itr.hasNext();) {
            SipProvider sipProvider = (SipProvider) itr.next();
            try {
                sipProvider.removeSipListener(this);
                sipStack.deleteSipProvider(sipProvider);
            } catch (ObjectInUseException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        for (Iterator<?> itr = sipStack.getListeningPoints(); itr.hasNext();) {
            ListeningPoint listeningPoint = (ListeningPoint) itr.next();
            try {
                sipStack.deleteListeningPoint(listeningPoint);
            } catch (ObjectInUseException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        sipStack.stop();
        sipStack = null;
    }

    public void sendInvite(CallLegData leg) throws ParseException, InvalidArgumentException, SipException {
        String fromUser = leg.getFromUser();
        String to = leg.getTo();
        byte[] sdp = leg.getLocalSdp();

        SipURI from = addressFactory.createSipURI(fromUser, getHost() + ":" + getPort());
        Address fromNameAddress = addressFactory.createAddress(from);
        fromNameAddress.setDisplayName(fromUser);
        String fromTag = utilsExt.generateTag();
        FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, fromTag);

        String username = to.substring(to.indexOf(":") + 1, to.indexOf("@"));
        String address = to.substring(to.indexOf("@") + 1);

        SipURI toAddress = addressFactory.createSipURI(username, address);
        Address toNameAddress = addressFactory.createAddress(toAddress);
        toNameAddress.setDisplayName(username);
        ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

        SipURI requestURI = addressFactory.createSipURI(username, address);
        requestURI.setTransportParam("udp");

        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader viaHeader = headerFactory.createViaHeader(getHost(), getPort(), "udp", null);
        viaHeaders.add(viaHeader);

        CallIdHeader callIdHeader = sipProvider.getNewCallId();

        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1, Request.INVITE);

        MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

        Request request = messageFactory.createRequest(requestURI, Request.INVITE, callIdHeader, cSeqHeader,
                fromHeader, toHeader, viaHeaders, maxForwards);

        SipURI contactURI = addressFactory.createSipURI(fromUser, getHost());
        contactURI.setPort(getPort());
        Address contactAddress = addressFactory.createAddress(contactURI);
        contactAddress.setDisplayName(fromUser);
        ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
        request.addHeader(contactHeader);
        
        if (leg.isOutbound()) {
            Header callType = headerFactory.createHeader("P-Nuance-Call-Type", "outbound");
            request.addHeader(callType);
            Header outboundTo = headerFactory.createHeader("P-Nuance-Outbound-To", leg.getOutboundTo());
            request.addHeader(outboundTo);
        }

        ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
        request.setContent(sdp, contentTypeHeader);

        // try {
        // Thread.currentThread().sleep(500);
        // } catch (InterruptedException e) {
        // LOGGER.error(e.getMessage(), e);
        // }

        ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(request);
        Dialog dialog = clientTransaction.getDialog();
        dialog.setApplicationData(leg);
        leg.setSipDialog(dialog);

        LOGGER.debug("Sending INVITE\n" + request.toString());
        clientTransaction.sendRequest();
    }

    public void waitForInvite(CallLegData leg) {
        waitingForInviteLegs.add(leg);

    }

    public String getHost() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    private void handleRemoteSdp(CallLegData leg, byte[] rawContent) {
        String remoteSdpContent = new String(rawContent);

        try {
            SessionDescription remoteSdp = sdpFactory.createSessionDescription(remoteSdpContent);

            if (remoteSdp.getConnection() != null) {
                leg.setRemoteRtpAddress(remoteSdp.getConnection().getAddress());
            }
            Vector<MediaDescription> remoteMedias = remoteSdp.getMediaDescriptions(false); // parse, don't create
            for (Enumeration<MediaDescription> e = remoteMedias.elements(); e.hasMoreElements();) {
                MediaDescription remoteMediaDesc = e.nextElement();
                Media remoteMedia = remoteMediaDesc.getMedia();
                if (remoteMediaDesc.getConnection() != null) {
                    leg.setRemoteRtpAddress(remoteMediaDesc.getConnection().getAddress());
                }
                leg.setRemoteRtpPort(remoteMedia.getMediaPort());
            }
            LOGGER.debug(leg.getName() + " saveRemoteSdp: remoteRtp=" + leg.getRemoteRtpAddress() + ":"
                    + leg.getRemoteRtpPort());

            leg.setPayloadType(0);
        } catch (SdpParseException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        } catch (SdpException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void handleBye(CallLegData leg) {
        leg.getCallHandler().handleBye(leg);
        leg.setSipDialog(null);
    }

    public void sendBye(CallLegData leg) {
        Dialog dialog = leg.getSipDialog();
        if (dialog != null) {
            try {
                Request byeRequest = dialog.createRequest(Request.BYE);
                ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(byeRequest);
                LOGGER.debug("Sending BYE\n" + byeRequest.toString());
                dialog.sendRequest(clientTransaction);
            } catch (SipException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public void processDialogTerminated(DialogTerminatedEvent event) {
        LOGGER.debug("Got " + event.toString());
    }

    public void processIOException(IOExceptionEvent event) {
        LOGGER.debug("Got " + event.toString());
    }

    public void processRequest(RequestEvent event) {
        Request request = event.getRequest();
        LOGGER.debug("Got " + request.toString());

        Dialog dialog = event.getDialog();
        CallLegData leg = (dialog != null) ? (CallLegData) dialog.getApplicationData() : null;

        String method = request.getMethod();
        if (method.equals(Request.INVITE)) {
            Response tryingResponse = null;
            try {
                tryingResponse = messageFactory.createResponse(Response.TRYING, request);
            } catch (ParseException e) {
                LOGGER.error(e.getMessage(), e);
                return;
            }
            ServerTransaction serverTransaction = event.getServerTransaction();
            if (serverTransaction == null) {
                try {
                    serverTransaction = sipProvider.getNewServerTransaction(request);
                } catch (TransactionAlreadyExistsException e) {
                    LOGGER.error(e.getMessage(), e);
                    return;
                } catch (TransactionUnavailableException e) {
                    LOGGER.error(e.getMessage(), e);
                    return;
                }
            }

            try {
                serverTransaction.sendResponse(tryingResponse);
            } catch (SipException e) {
                LOGGER.error(e.getMessage(), e);
                return;
            } catch (InvalidArgumentException e) {
                LOGGER.error(e.getMessage(), e);
                return;
            }
            if (leg == null) {
                // new call
                leg = waitingForInviteLegs.poll();
                if (leg != null) {
                    dialog = serverTransaction.getDialog();
                    dialog.setApplicationData(leg);
                    leg.setSipDialog(dialog);

                    handleRemoteSdp(leg, request.getRawContent());
                    Response okResponse = null;
                    try {
                        okResponse = messageFactory.createResponse(Response.OK, request);
                    } catch (ParseException e) {
                        LOGGER.error(e.getMessage(), e);
                        waitingForInviteLegs.add(leg);
                        return;
                    }

                    Address address = null;
                    try {
                        address = addressFactory.createAddress("<sip:" + ip + ":" + port + ">");
                    } catch (ParseException e1) {
                        LOGGER.error(e1.getMessage(), e1);
                        waitingForInviteLegs.add(leg);
                        return;
                    }
                    ContactHeader contactHeader = headerFactory.createContactHeader(address);
                    okResponse.addHeader(contactHeader);

                    byte[] sdp = leg.getLocalSdp();

                    ContentTypeHeader contentTypeHeader = null;
                    try {
                        contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
                    } catch (ParseException e2) {
                        LOGGER.error(e2.getMessage(), e2);
                        waitingForInviteLegs.add(leg);
                        return;
                    }
                    try {
                        okResponse.setContent(sdp, contentTypeHeader);
                    } catch (ParseException e1) {
                        LOGGER.error(e1.getMessage(), e1);
                        waitingForInviteLegs.add(leg);
                        return;
                    }

                    try {
                        serverTransaction.sendResponse(okResponse);
                    } catch (SipException e) {
                        LOGGER.error(e.getMessage(), e);
                        waitingForInviteLegs.add(leg);
                        return;
                    } catch (InvalidArgumentException e) {
                        LOGGER.error(e.getMessage(), e);
                        waitingForInviteLegs.add(leg);
                        return;
                    }
                } else {
                    // No waiting leg
                    Response failResponse = null;
                    try {
                        failResponse = messageFactory.createResponse(Response.BUSY_HERE, request);
                    } catch (ParseException e) {
                        LOGGER.error(e.getMessage(), e);
                        waitingForInviteLegs.add(leg);
                        return;
                    }
                    try {
                        serverTransaction.sendResponse(failResponse);
                    } catch (SipException e) {
                        LOGGER.error(e.getMessage(), e);
                        waitingForInviteLegs.add(leg);
                        return;
                    } catch (InvalidArgumentException e) {
                        LOGGER.error(e.getMessage(), e);
                        waitingForInviteLegs.add(leg);
                        return;
                    }
                }
            } else {
                // re-INVITE: TODO
            }
        } else if (method.equals(Request.ACK)) {
            leg.getCallHandler().handleInviteOk(leg);

        } else if (method.equals(Request.BYE)) {
            handleBye(leg);
            detachLeg(leg);
        }
    }

    @Override
    public void processResponse(ResponseEvent event) {
        Response response = event.getResponse();
        LOGGER.debug("Got " + response.toString());

        ClientTransaction clientTransaction = event.getClientTransaction();
        Dialog dialog = event.getDialog();
        CallLegData leg = (dialog != null) ? (CallLegData) dialog.getApplicationData() : null;

        int code = response.getStatusCode();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        String method = cseq.getMethod();
        long cseqno = cseq.getSeqNumber();

        if (code == Response.OK) {
            if (method.equals(Request.INVITE)) {
                handleRemoteSdp(leg, response.getRawContent());

                // Request ackRequest = dialog.createRequest(Request.ACK);
                Request ackRequest = null;
                try {
                    ackRequest = dialog.createAck(cseqno);
                } catch (SipException | InvalidArgumentException e) {
                    LOGGER.error("Error creating ACK: " + e.getMessage(), e);
                    leg.getCallHandler().handleInviteFailure(leg, code);
                    detachLeg(leg);
                    return;
                }

                try {
                    dialog.sendAck(ackRequest);
                } catch (SipException e) {
                    LOGGER.error("Error sending ACK: " + e.getMessage(), e);
                    leg.getCallHandler().handleInviteFailure(leg, code);
                    detachLeg(leg);
                    return;
                }

                leg.getCallHandler().handleInviteOk(leg);
            } else if (method.equals(Request.BYE)) {
                leg.getCallHandler().handleByeOk(leg);
                detachLeg(leg);
            }
        } else if (code >= 300) {
            if (method.equals(Request.INVITE)) {
                leg.getCallHandler().handleInviteFailure(leg, code);
            } else if (method.equals(Request.BYE)) {
                leg.getCallHandler().handleByeFailure(leg, code);
                detachLeg(leg);
            }
        }
    }

    public void processTimeout(TimeoutEvent event) {
        LOGGER.debug("Got " + event.toString());
    }

    public void processTransactionTerminated(TransactionTerminatedEvent event) {
        LOGGER.debug("Got " + event.toString());
    }

    private void detachLeg(CallLegData leg) {
        Dialog dialog = leg.getSipDialog();
        if (dialog != null) {
            dialog.setApplicationData(null);
        }
        leg.setSipDialog(null);
    }

}
