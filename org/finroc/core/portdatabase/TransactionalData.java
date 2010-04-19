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

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.Virtual;

import org.finroc.core.FrameworkElement;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.stream.InputPacketProcessor;
import org.finroc.core.port.stream.InputTransactionStreamPort;
import org.finroc.core.port.stream.OutputTransactionStreamPort;
import org.finroc.core.port.stream.Transaction;
import org.finroc.core.port.stream.TransactionPacket;
import org.finroc.core.port.std.PortDataImpl;
import org.finroc.core.port.std.PullRequestHandler;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.stream.SingletonPort;
import org.finroc.core.port.stream.StreamCommitThread;

/**
 * @author max
 *
 * This is the base class for large complex data that is shared in the whole
 * runtime. It's characteristics are that data is large, singleton, but usually only minor
 * changes are made, so that copying - especially over network - would result
 * in a waste of resources. Therefore instances of transactional data
 * are synchronized using transactions.
 * The object may be modified - non-blocking - in several runtime environments at the same time.
 * The programmer needs to make sure that the incoming transactions result in the same
 * data in each runtime environment. If multiple threads access the same Java object
 * the programmer also needs to take care of locking.
 *
 * This class already creates the necessary ports in a PortSet. This can be retrieved using getPortSet()
 */
public abstract class TransactionalData<B extends Transaction> extends PortDataImpl implements InputPacketProcessor<TransactionPacket>, StreamCommitThread.Callback, PullRequestHandler {

    /** Port Set for Transactional Data */
    private final TDPortSet portSet;

    /** Port for outgoing transactions */
    protected final OutputTransactionStreamPort<B> output;

    /** Port for incoming transactions */
    protected final InputTransactions input;

    /** Port sharing data locally */
    protected final SingletonPort<TransactionalData<B>> localData;

    /** Are we currently handling new connection? */
    protected volatile boolean handlingNewConnection = false;

    public TransactionalData(String description, boolean input, boolean output, boolean local, DataType bclass,
                             int commitInterval, boolean inputShared, boolean outputShared) {

        portSet = new TDPortSet(description);
        this.input = input ? new InputTransactions(new PortCreationInfo(bclass,
                     inputShared ? PortFlags.SHARED_INPUT_PORT : PortFlags.INPUT_PORT)) : null;
        this.output = output ? new OutputTransactionStreamPort<B>(new PortCreationInfo("output transactions", portSet, bclass,
                      outputShared ? PortFlags.SHARED_OUTPUT_PORT : PortFlags.OUTPUT_PORT), commitInterval, this) : null;
        localData = local ? new SingletonPort<TransactionalData<B>>(new PortCreationInfo("data", this.getType(), PortFlags.OUTPUT_PORT), this) : null;
        StreamCommitThread.getInstance().register(this);
    }

    /**
     * Port Set for transactional data class
     */
    private class TDPortSet extends FrameworkElement {

        public TDPortSet(String description) {
            super(description);
        }

        @Override
        protected void prepareDelete() {
            StreamCommitThread.getInstance().unregister(TransactionalData.this);
            super.prepareDelete();
        }

        @Override
        protected void postChildInit() {
            TransactionalData.this.postChildInit();
            super.postChildInit();
        }
    }

    /**
     * Special port for input transactions
     */
    protected class InputTransactions extends InputTransactionStreamPort<B> {

        public InputTransactions(PortCreationInfo pci) {
            super("input transactions", pci, TransactionalData.this);
        }

        // we have a new connection
        @Override
        protected void newConnection(AbstractPort partner) {
            handleNewConnection(partner);
        }
    }

    /**
     * Writes data to output stream (is existent)
     *
     * @param data Transaction to write (is copied and can instantly be reused)
     */
    public void commitData(B data) {
        if (output != null) {
            output.commitData(data);
        }
    }

    @Virtual protected void handleNewConnection(AbstractPort partner) {
        handlingNewConnection = true;
        @Const TransactionPacket b = (TransactionPacket)input.getPullLockedUnsafe(false);
        this.processPacket(b);
        b.getManager().getCurrentRefCounter().releaseLock();
        handlingNewConnection = false;
    }

    /**
     * @return Port Set for Transactional Data
     */
    public FrameworkElement getPortSet() {
        return portSet;
    }

    /** may be overridden */
    protected void postChildInit() {}

    @Override
    public void streamThreadCallback(long time) {
        update(time);
    }

    /**
     * Can be overridden... is called regularly
     */
    protected void update(long time) {}

    @Override
    public void deserialize(CoreInput is) {
        throw new RuntimeException("Deserialization not meant to be done");
    }

    @Override
    public void serialize(CoreOutput os) {
        throw new RuntimeException("Serialization not meant to be done");
    }
}
