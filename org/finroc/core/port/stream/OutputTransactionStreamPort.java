/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2010 Max Reichardt,
 *   Robotics Research Lab, University of Kaiserslautern
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.finroc.core.port.stream;

import org.finroc.jc.AtomicPtr;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.std.Port;
import org.finroc.core.port.std.PullRequestHandler;
import org.finroc.core.portdatabase.CoreSerializable;

/**
 * @author max
 *
 * This is a port that provides an output stream to it's user and to the outside.
 *
 * Typically, packets are pushed across the network.
 * Pulling doesn't make much sense for a stream.
 * Therefore, pulling typically provides some general/initial info about the stream - or simply nothing.
 *
 * (Implementation of this class is non-blocking... that's why it's slightly verbose)
 */
public class OutputTransactionStreamPort<T extends Transaction> extends Port<TransactionPacket> implements StreamCommitThread.Callback {

    /**
     * interval in which new data is commited
     * higher values may be useful for grouping transactions.
     */
    protected final int commitInterval;

    /**
     * last time stamp of commit
     */
    protected long lastCommit = 0;

    /**
     * Stream packet that is currently written.
     * When it is written to, this variable contains lock to this packet.
     * When packet has been committed, variable contains null
     */
    protected final AtomicPtr<TransactionPacket> currentPacket = new AtomicPtr<TransactionPacket>();

    /** ChunkedBuffer for signalling only: SIGNAL that writer currently writes to chunk */
    @PassByValue private static TransactionPacket LOCK = new TransactionPacket();

    /** ChunkedBuffer for signalling only: SIGNAL that writer should commit data after writing */
    @PassByValue private static TransactionPacket COMMIT = new TransactionPacket();

    /**
     * @param pci Port Creation Info
     * @param commitInterval Interval in which new data is commited - higher values may be useful for grouping transactions.
     * @param listener Listener for pull requests
     */
    public OutputTransactionStreamPort(PortCreationInfo pci, int commitInterval, PullRequestHandler listener) {
        super(pci);
        assert(commitInterval > 0);
        this.commitInterval = commitInterval;
        setPullRequestHandler(listener);
        StreamCommitThread.getInstance().register(this);
    }

    @Override
    protected void prepareDelete() {
        StreamCommitThread.getInstance().unregister(this);
        super.prepareDelete();
    }

    @Override
    // called by StreamThread periodically
    public void streamThreadCallback(long curTime) {
        if (curTime >= lastCommit + commitInterval) {

            // while loop to retry when atomic-compare-and-swap fails
            while (true) {
                TransactionPacket cb = currentPacket.get();
                if (cb == COMMIT) {
                    break; // none of our business
                }
                if (cb == LOCK) {
                    if (currentPacket.compareAndSet(LOCK, COMMIT)) {
                        break;
                    }
                } else if (cb == null) {
                    break; // none of our business
                } else {
                    // we commit
                    if (currentPacket.compareAndSet(cb, null)) {
                        publish(cb);
                    }
                }
            }

            lastCommit = curTime;
        }
    }

    /**
     * Writes data to stream
     * (may not be called concurrently)
     *
     * @param data Data to write (is copied and can instantly be reused)
     */
    public void commitData(CoreSerializable data) {

        // Lock packet
        TransactionPacket cb = null;
        TransactionPacket expect = null;
        while (true) {
            cb = currentPacket.get();
            assert(cb != LOCK); // concurrent write
            assert(cb != COMMIT); // concurrent write
            if (cb == null) {
                cb = getUnusedBuffer();
                expect = null;
                break;
            } else {
                if (currentPacket.compareAndSet(cb, LOCK)) {
                    expect = LOCK;
                    break;
                }
            }
        }

        // copy data to packet
        cb.add(data);
        //data.serialize(cb.serializer);

        // Unlock packet
        if (!currentPacket.compareAndSet(expect, cb)) {

            // okay... we need to commit
            assert(currentPacket.get() == COMMIT);
            publish(cb);
            currentPacket.set(null);
        }
    }

    /* (non-Javadoc)
     * @see core.port4.Port#getUnusedBuffer()
     */
    @Override // non-virtual, but override for user convenience
    public TransactionPacket getUnusedBuffer() {
        TransactionPacket result = super.getUnusedBuffer();
        result.reset();
        return result;
    }
}
