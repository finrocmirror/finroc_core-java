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
package org.finroc.core.portdatabase;

import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.container.ConcurrentMap;
import org.finroc.jc.container.SafeConcurrentlyIterableList;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;

import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortData;
import org.finroc.core.port.stream.Transaction;
import org.finroc.core.port.stream.TransactionPacket;

/**
 * @author max
 *
 * This class implements a set of objects as TransactionalData.
 * It does not contain elements twice.
 *
 * It allows reading and modifying the set in different runtime environments concurrently.
 * The keys of the entries, however, need to be unique across the network.
 *
 * It handles pending commands (e.g. Remove command before the add command is received)
 */
public abstract class TransactionalSet<K, B extends TransactionalSet.Entry<K>> extends TransactionalData<B> {

    /** UID - should be unused */
    private static final long serialVersionUID = 4;

    /** Wrapped map */
    protected final ConcurrentMap<K, B> set = new ConcurrentMap<K, B>(null);

    /** For reading keys from buffer - reused */
    @PassByValue protected B tempReadKey;

    /** List with commands that could not yet be executed */
    @CppType("SafeConcurrentlyIterableList<B, 4, true>")
    @PassByValue protected final SafeConcurrentlyIterableList<B> pendingCommands = new SafeConcurrentlyIterableList<B>(2, 4);

    /** Time of Last check if pending commands can be executed */
    protected long lastPendingCommandCheck = 0;

    /** Frequency of checks if pending commands can be executed */
    public final int PENDING_COMMAND_CHECK_INTERVAL = 2000;

    /** After this period pending commands will be ignored */
    public final int PENDING_COMMAND_TIMEOUT = 10000;

    /** Log domain for this class */
    @InCpp("_CREATE_NAMED_LOGGING_DOMAIN(logDomain, \"stream_ports\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("stream_ports");

    @SuppressWarnings("unchecked")
    public TransactionalSet(String description, boolean input, boolean output, boolean local,
                            DataType bclass, int commitInterval, boolean inputShared, boolean outputShared) {

        super(description, input, output, local, bclass, commitInterval, inputShared, outputShared);
        tempReadKey = (B)bclass.createTransactionInstance();
    }

    /**
     * @param remove Entry to remove (only needs to contain key) (will be reused so do not store)
     * @return Defer operation? (not possible yet)
     */
    protected boolean incomingRemove(B remove) {
        B removed = set.remove(remove.getKey());
        return removed == null;
    }

    /**
     * @param add Entry to add
     */
    protected synchronized void incomingAdd(B add) {
        set.put(add.getKey(), add);
    }

    /**
     * @param change Entry containing changes
     * @return Defer operation? (not possible yet)
     */
    protected boolean incomingChange(B change) {
        B entry = set.get(change.getKey());
        if (entry != null) {
            entry.handleChange(change);
            return false;
        }
        return true;
    }

    protected void processPendingCommands(long time) {

        // check whether there is something to do
        if (time < lastPendingCommandCheck + PENDING_COMMAND_CHECK_INTERVAL) {
            return;
        }
        lastPendingCommandCheck = time;

        // process commands
        @Ptr ArrayWrapper<B> b = pendingCommands.getIterable();
        for (int i = 0, n = b.size(); i < n; i++) {
            B pc = b.get(i);
            boolean stillPending = false;
            if (pc.opCode == Transaction.REMOVE) {
                stillPending = incomingRemove(pc);
            } else if (pc.opCode == Transaction.CHANGE) {
                stillPending = incomingChange(pc);
            }
            if ((!stillPending) || time > pc.timestamp + PENDING_COMMAND_TIMEOUT) {
                pendingCommands.remove(pc);
                i--;
            }
        }
    }

    /**
     * Add element to set.
     *
     * @param elem Element to add (opCode does not need to be set)
     */
    public synchronized void add(B elem) {
        elem.opCode = Transaction.ADD;
        set.put(elem.getKey(), elem);
        commitTransaction(elem);
    }

    /**
     * Commit transaction and handle any expcetions
     *
     * @param transaction Transaction to commit
     */
    protected void commitTransaction(B transaction) {
        if (output != null) {
            try {
                commitData(transaction);
            } catch (Exception e) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
            }
        }
    }

    /**
     * Remove element from set.
     *
     * @param elem Key of element to remove
     */
    public void remove(K key) {
        remove(set.get(key));
    }
    /**
     * Remove element from set.
     *
     * @param elem Element to remove
     */
    public synchronized void remove(B elem) {
        set.remove(elem.getKey());
        elem.opCode = Transaction.REMOVE;
        commitData(elem);
    }

    @Override
    protected void update(long time) {
        processPendingCommands(time);
    }

    /**
     * Entry in set. Is transaction at the same time - makes handling pending commands simpler
     */
    public abstract static class Entry<K> extends Transaction {

        /**
         * @return Unique key in network
         */
        public abstract @Ref K getKey();

        /**
         * Change command has been received
         *
         * @param change Transaction/Entry containing change
         */
        public abstract void handleChange(Entry<K> change);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean processPacket(TransactionPacket buffer) {
        if (handlingNewConnection && (!buffer.initialPacket)) {
            return true;
        }

        CoreInput crv = ThreadLocalCache.getFast().inputPacketProcessor;
        crv.reset(buffer);
        while (crv.moreDataAvailable()) {
            tempReadKey.deserialize(crv);
            boolean defer = false;
            switch (tempReadKey.opCode) {
            case Transaction.ADD:
                incomingAdd(tempReadKey);
                break;
            case Transaction.REMOVE:
                defer = incomingRemove(tempReadKey);
                break;
            case Transaction.CHANGE:
                defer = incomingChange(tempReadKey);
                break;
            default:
                throw new RuntimeException("Corrupt Stream");
            }
            if (defer) {
                B b = (B)getType().createTransactionInstance();
                b.assign(tempReadKey);
                pendingCommands.add(b, false);
            }
        }
        return false;
    }

    @Override
    public PortData pullRequest(PortBase origin, byte addLocks) {
        ConcurrentMap<K, B>.MapIterator it = set.getIterator();
        TransactionPacket tp = output.getUnusedBuffer();
        tp.initialPacket = true;
        while (it.next()) {
            B v = it.getValue();
            tp.addTransaction(v);
        }
        tp.getManager().getCurrentRefCounter().setLocks(addLocks);
        return tp;
    }
}
