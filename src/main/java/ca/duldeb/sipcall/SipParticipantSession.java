/* 
 * ---------------------------------------------------------------------------
 *
 * COPYRIGHT (c) 2014 Nuance Communications Inc. All Rights Reserved.
 *
 * The copyright to the computer program(s) herein is the property of
 * Nuance Communications Inc. The program(s) may be used and/or copied
 * only with the written permission from Nuance Communications Inc.
 * or in accordance with the terms and conditions stipulated in the
 * agreement/contract under which the program(s) have been supplied.
 *
 * Author: jdebroin
 * Date  : Jun 23, 2014
 *
 * ---------------------------------------------------------------------------
 */
package ca.duldeb.sipcall;

import java.util.Collection;

import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.util.HashedWheelTimer;

import com.biasedbit.efflux.participant.ParticipantDatabase;
import com.biasedbit.efflux.participant.ParticipantEventListener;
import com.biasedbit.efflux.participant.RtpParticipant;
import com.biasedbit.efflux.session.AbstractRtpSession;
import com.biasedbit.efflux.session.RtpSessionEventListener;

public class SipParticipantSession extends AbstractRtpSession implements ParticipantEventListener {

    // constructors ---------------------------------------------------------------------------------------------------

    public SipParticipantSession(String id, int payloadType, RtpParticipant localParticipant) {
        super(id, payloadType, localParticipant, null, null);
    }

    public SipParticipantSession(String id, int payloadType, RtpParticipant localParticipant,
                                   HashedWheelTimer timer) {
        super(id, payloadType, localParticipant, timer, null);
    }

    public SipParticipantSession(String id, int payloadType, RtpParticipant localParticipant,
                                   OrderedMemoryAwareThreadPoolExecutor executor) {
        super(id, payloadType, localParticipant, null, executor);
    }

    public SipParticipantSession(String id, int payloadType, RtpParticipant localParticipant,
                                   HashedWheelTimer timer, OrderedMemoryAwareThreadPoolExecutor executor) {
        super(id, payloadType, localParticipant, timer, executor);
    }
    
    public SipParticipantSession(String id, Collection<Integer> payloadTypes, RtpParticipant localParticipant,
                HashedWheelTimer timer, OrderedMemoryAwareThreadPoolExecutor executor) {
        super(id, payloadTypes, localParticipant, timer, executor);
    }

    // AbstractRtpSession ---------------------------------------------------------------------------------------------

    @Override
    protected ParticipantDatabase createDatabase() {
        return new SipParticipantDatabase(this.id);
    }

    // ParticipantEventListener ---------------------------------------------------------------------------------------

    @Override
    public void participantCreatedFromSdesChunk(RtpParticipant participant) {
        for (RtpSessionEventListener listener : this.eventListeners) {
            listener.participantJoinedFromControl(this, participant);
        }
    }

    @Override
    public void participantCreatedFromDataPacket(RtpParticipant participant) {
        for (RtpSessionEventListener listener : this.eventListeners) {
            listener.participantJoinedFromData(this, participant);
        }
    }

    @Override
    public void participantDeleted(RtpParticipant participant) {
        for (RtpSessionEventListener listener : this.eventListeners) {
            listener.participantDeleted(this, participant);
        }
    }
}

