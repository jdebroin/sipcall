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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.biasedbit.efflux.logging.Logger;
import com.biasedbit.efflux.packet.DataPacket;
import com.biasedbit.efflux.packet.SdesChunk;
import com.biasedbit.efflux.packet.SdesChunkItem;
import com.biasedbit.efflux.participant.ParticipantDatabase;
import com.biasedbit.efflux.participant.ParticipantOperation;
import com.biasedbit.efflux.participant.RtpParticipant;

public class SipParticipantDatabase implements ParticipantDatabase {

    private static final Logger LOG = Logger.getLogger(SipParticipantDatabase.class);

    private String id;

    /**
     * List of unicast receivers. This is a list of explicitly added participants, by the applications using this lib.
     * They might get linked to
     */
    private final Collection<RtpParticipant> receivers;
    /**
     * List of existing members.
     */
    private final Map<Long, RtpParticipant> members;
    private final ReentrantReadWriteLock lock;

    public SipParticipantDatabase(String id) {
        this.id = id;

        this.receivers = new ArrayList<RtpParticipant>();
        this.members = new HashMap<Long, RtpParticipant>();

        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Collection<RtpParticipant> getReceivers() {
        return Collections.unmodifiableCollection(this.receivers);
    }

    @Override
    public Map<Long, RtpParticipant> getMembers() {
        return Collections.unmodifiableMap(this.members);
    }

    @Override
    public void doWithReceivers(ParticipantOperation operation) {
        this.lock.readLock().lock();
        try {
            for (RtpParticipant receiver : this.receivers) {
                try {
                    operation.doWithParticipant(receiver);
                } catch (Exception e) {
                    LOG.error("Failed to perform operation {} on receiver {}.", e, operation, receiver);
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void doWithParticipants(ParticipantOperation operation) {
        this.lock.readLock().lock();
        try {
            for (RtpParticipant member : this.members.values()) {
                try {
                    operation.doWithParticipant(member);
                } catch (Exception e) {
                    LOG.error("Failed to perform operation {} on member {}.", e, operation, member);
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean addReceiver(RtpParticipant remoteParticipant) {
        if (!remoteParticipant.isReceiver()) {
            return false;
        }

        this.lock.writeLock().lock();
        try {
            // Iterate through the members, trying to find a match for this participant through the RTP ports or CNAME.
            boolean isMember = false;
            for (RtpParticipant member : this.members.values()) {
                boolean sameDestinationAddresses = member.getDataDestination().equals(
                        remoteParticipant.getDataDestination())
                        && member.getControlDestination().equals(remoteParticipant.getControlDestination());
                boolean sameCname = member.getInfo().getCname().equals(remoteParticipant.getInfo().getCname());
                if (sameDestinationAddresses || sameCname) {
                    // Instead of adding the newly provided participant, reuse the member
                    this.receivers.add(member);
                    isMember = true;
                    break;
                }
            }

            return isMember || this.receivers.add(remoteParticipant);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeReceiver(RtpParticipant remoteParticipant) {
        this.lock.writeLock().lock();
        try {
            return this.receivers.remove(remoteParticipant);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public RtpParticipant getParticipant(long ssrc) {
        this.lock.readLock().lock();
        try {
            return this.members.get(ssrc);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public RtpParticipant getOrCreateParticipantFromDataPacket(SocketAddress origin, DataPacket packet) {
        this.lock.writeLock().lock();
        try {
            RtpParticipant participant = this.members.get(packet.getSsrc());
            if (participant == null) {
                // Iterate through the receivers, trying to find a match for this participant through the RTP ports.
                for (RtpParticipant receiver : this.receivers) {
                    if (receiver.getDataDestination().equals(origin)) {
                        // Will be added to the members list.
                        receiver.getInfo().setSsrc(packet.getSsrc());
                        participant = receiver;
                        participant.setLastDataOrigin(origin);
                        break;
                    }
                }

                // boolean created = false;
                // if (!isReceiver) {
                // // Will be added to the members list but will NOT be a receiver.
                // participant = RtpParticipant.createFromUnexpectedDataPacket(origin, packet);
                // created = true;
                // }

                if (participant != null) {
                    this.members.put(packet.getSsrc(), participant);
                }

                // if (created) {
                // this.listener.participantCreatedFromDataPacket(participant);
                // }
            }

            return participant;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public RtpParticipant getOrCreateParticipantFromSdesChunk(SocketAddress origin, SdesChunk chunk) {
        this.lock.writeLock().lock();
        try {
            RtpParticipant participant = this.members.get(chunk.getSsrc());
            if (participant == null) {
                // Iterate through the receivers, trying to find a match for this participant through the RTCP ports or
                // CNAME.
                for (RtpParticipant receiver : this.receivers) {
                    // Verify if CNAME is the same
                    boolean equalCname = false;
                    String chunkCname = chunk.getItemValue(SdesChunkItem.Type.CNAME);
                    if ((chunkCname != null) && chunkCname.equals(receiver.getInfo().getCname())) {
                        equalCname = true;
                    }

                    // If either CNAME matches or control destination matches source, then there's a match in the
                    // receivers list.
                    if (receiver.getControlDestination().equals(origin) || equalCname) {
                        // Will be added to the members list.
                        receiver.getInfo().setSsrc(chunk.getSsrc());
                        participant = receiver;
                        participant.setLastControlOrigin(origin);
                        participant.receivedSdes();
                        participant.getInfo().updateFromSdesChunk(chunk);
                        break;
                    }
                }

                // boolean created = false;
                // if (!isReceiver) {
                // // Will be added to the members list but will NOT be a receiver.
                // participant = RtpParticipant.createFromSdesChunk(origin, chunk);
                // created = true;
                // }

                if (participant != null) {
                    this.members.put(chunk.getSsrc(), participant);
                }
                // if (created) {
                // this.listener.participantCreatedFromSdesChunk(participant);
                // }
            }

            return participant;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public int getReceiverCount() {
        return this.receivers.size();
    }

    @Override
    public int getParticipantCount() {
        return this.members.size();
    }

    @Override
    public void cleanup() {
    }

}
