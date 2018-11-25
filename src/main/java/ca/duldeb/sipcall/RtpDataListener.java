package ca.duldeb.sipcall;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.session.RtpSession;
import com.biasedbit.efflux.session.RtpSessionDataListener;

public class RtpDataListener implements RtpSessionDataListener {
    private final Logger LOGGER = LoggerFactory.getLogger(RtpDataListener.class);

    private CallLegData leg;

    public RtpDataListener(CallLegData leg) {
        this.leg = leg;
    }

    @Override
    public void dataPacketReceived(RtpSession session, RtpParticipantInfo participant, DataPacket packet) {
        // LOGGER.debug("Session 1 received packet: " + packet + "(session: " + session.getId() + ")");
        if (packet.getPayloadType() == leg.getPayloadType()) {
            int size = packet.getDataSize();
            byte[] data = packet.getDataAsArray();
            try {
                leg.writeDataPacket(data, size);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
