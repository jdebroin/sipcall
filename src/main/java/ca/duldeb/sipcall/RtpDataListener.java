package ca.duldeb.sipcall;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.IOException;

import org.apache.commons.logging.Log;

import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.participant.RtpParticipantInfo;
import com.biasedbit.efflux.session.RtpSession;
import com.biasedbit.efflux.session.RtpSessionDataListener;

public class RtpDataListener implements RtpSessionDataListener {
    private static final Log LOGGER = getLog(RtpDataListener.class);

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
                leg.getRecordWriter().write(leg, data, size);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
