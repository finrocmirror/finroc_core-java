//
// You received this file as part of Finroc
// A framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//----------------------------------------------------------------------
package org.finroc.core.port.net;

import java.util.ArrayList;
import java.util.List;

import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.rpc.internal.AbstractCall;
import org.finroc.core.port.rpc.internal.RPCPort;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.port.PortListener;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.portdatabase.UnknownType;
import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.Serialization.DataEncoding;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Port that is used for networking.
 * Uniform wrapper class for Std, CC, and Interface ports.
 */
@SuppressWarnings("rawtypes")
public abstract class NetPort implements PortListener {

    /** Default timeout for pulling data over the net */
    public final static int PULL_TIMEOUT = 1000;

    /** TCPServerConnection or RemoteServer instance that this port belongs to */
    protected final Object belongsTo;

    /** Last time the value was updated (used to make sure that minimum update interval is kept) */
    public long lastUpdate = Long.MIN_VALUE;

    /** Wrapped port */
    private final AbstractPort wrapped;

    /** Handle of Remote port */
    protected int remoteHandle;

    /** Finroc Type info type */
    protected final FinrocTypeInfo.Type ftype;

    /** Data type to use when writing data to the network */
    private Serialization.DataEncoding encoding = Serialization.DataEncoding.BINARY;

    /** Data type to actually use when writing data to the network. This can be different from getEncoding() with adapter types. */
    private Serialization.DataEncoding internalEncoding = Serialization.DataEncoding.BINARY;

    public NetPort(PortCreationInfo pci, Object belongsTo) {
        // keep most these flags
        int f = pci.flags & (FrameworkElementFlags.ACCEPTS_DATA | FrameworkElementFlags.EMITS_DATA | FrameworkElementFlags.IS_OUTPUT_PORT |
                             FrameworkElementFlags.EXPRESS_PORT | FrameworkElementFlags.NON_STANDARD_ASSIGN /*| PortFlags.IS_CC_PORT | PortFlags.IS_INTERFACE_PORT*/ |
                             FrameworkElementFlags.ALTERNATIVE_LINK_ROOT | FrameworkElementFlags.GLOBALLY_UNIQUE_LINK | FrameworkElementFlags.FINSTRUCTED);

        // set either emit or accept data
        f |= ((f & FrameworkElementFlags.OUTPUT_PORT) == FrameworkElementFlags.OUTPUT_PORT) ? FrameworkElementFlags.EMITS_DATA : FrameworkElementFlags.ACCEPTS_DATA;
        f |= FrameworkElementFlags.NETWORK_ELEMENT | FrameworkElementFlags.VOLATILE;
        if ((f & FrameworkElementFlags.OUTPUT_PORT) != FrameworkElementFlags.OUTPUT_PORT) { // we always have a queue with (remote) input ports - to be able to switch
            f |= FrameworkElementFlags.HAS_QUEUE;
            if (pci.maxQueueSize > 1) {
                f |= FrameworkElementFlags.USES_QUEUE;
            }
        }
        ftype = FinrocTypeInfo.get(pci.dataType).getType();
        UnknownType ut = (pci.dataType instanceof UnknownType) ? (UnknownType)pci.dataType : null;
        if (isStdType() || isMethodType() || (ut != null && ut.isAdaptable())) {
            f |= FrameworkElementFlags.MULTI_TYPE_BUFFER_POOL; // different data types may be incoming - cc types are thread local
        }
        pci.flags = f;
        this.belongsTo = belongsTo;

        if (isUnknownType()) {
            if (ut.isAdaptable()) {
                encoding = ut.determineEncoding();
                internalEncoding = ut.determineInternalEncoding();
                wrapped = new StdNetPort(pci);
            } else {
                wrapped = new UnknownTypedNetPort(pci);
            }
            return;
        }

        wrapped = (isMethodType() ? (AbstractPort)new RPCNetPort(pci) :
                   (isCCType() ? (AbstractPort)new CCNetPort(pci) :
                    (AbstractPort)new StdNetPort(pci)));
    }

    /** Delete port */
    public void managedDelete() {
        getPort().managedDelete();
    }

    /** Helper methods for data type type */
    public boolean isStdType() {
        return ftype == FinrocTypeInfo.Type.STD;
    }

    public boolean isCCType() {
        return ftype == FinrocTypeInfo.Type.CC;
    }

    public boolean isUnknownType() {
        return ftype.ordinal() >= FinrocTypeInfo.Type.UNKNOWN_STD.ordinal();
    }

    public boolean isUnknownAdaptableType() {
        return isUnknownType() && ((UnknownType)getDataType()).isAdaptable();
    }

    public boolean isMethodType() {
        return ftype == FinrocTypeInfo.Type.METHOD;
    }

    public boolean isTransactionType() {
        return ftype == FinrocTypeInfo.Type.TRANSACTION;
    }

    public void updateFlags(int flags) {

        // process flags... keep DELETE and READY flags
        final int keepFlags = FrameworkElementFlags.STATUS_FLAGS;
        int curFlags = getPort().getAllFlags() & keepFlags;
        flags &= ~(keepFlags);
        flags |= curFlags;

        if (isUnknownType() && (!isUnknownAdaptableType())) {
            ((UnknownTypedNetPort)wrapped).updateFlags(flags);
            return;
        }

        if (isStdType() || isTransactionType() || isUnknownAdaptableType()) {
            ((StdNetPort)wrapped).updateFlags(flags);
        } else if (isCCType()) {
            ((CCNetPort)wrapped).updateFlags(flags);
        } else {
            ((RPCNetPort)wrapped).updateFlags(flags);
        }
    }

    /**
     * @return Last time the value was updated (used to make sure that minimum update interval is kept)
     */
    public long getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate Last time the value was updated (used to make sure that minimum update interval is kept)
     */
    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * @return TCPServerConnection or RemoteServer instance that this port belongs to
     */
    public Object getBelongsTo() {
        return belongsTo;
    }

    /**
     * @return Wrapped port
     */
    public AbstractPort getPort() {
        return wrapped;
    }

    /**
     * @return Handle of Remote port
     */
    public int getRemoteHandle() {
        return remoteHandle;
    }

    /**
     * @param remoteHandle Handle of Remote port
     */
    public void setRemoteHandle(int remoteHandle) {
        this.remoteHandle = remoteHandle;
    }

    // Overridable methods from ports
    /**
     * Initializes this runtime element.
     * The tree structure should be established by now (Uid is valid)
     * so final initialization can be made.
     *
     * Called before children are initialized
     */
    protected void preChildInit() {}

    /**
     * Initializes this runtime element.
     * The tree structure should be established by now (Uid is valid)
     * so final initialization can be made.
     *
     * Called before children are initialized
     */
    protected void postChildInit() {}

    /**
     * Prepares element for deletion.
     * Ports, for instance, are removed from edge lists etc.
     * The final deletion will be done by the GarbageCollector thread after
     * a few seconds (to ensure no other thread is working on this object
     * any more).
     */
    protected void prepareDelete() {}

    /**
     * Notify port of disconnect
     */
    protected void notifyDisconnect() {}

    /**
     * Called whenever current value of port has changed
     */
    protected void portChanged() {}

    /**
     * Called whenever connection was established
     */
    protected void connectionAdded() {}

    /**
     * Called whenever connection was removed
     */
    protected void connectionRemoved() {}

    /**
     * Decode incoming data from stream
     *
     * @param stream Stream to read from
     * @param dataEncoding Data encoding to use
     * @param readTimestamp Read timestamp from stream?
     */
    public void receiveDataFromStream(BinaryInputStream stream, DataEncoding dataEncoding, boolean readTimestamp) throws Exception {
        boolean anotherValue = false;
        do {
            byte changeType = stream.readByte();
            if (isStdType() || isTransactionType() || isUnknownAdaptableType()) {
                StdNetPort pb = (StdNetPort)wrapped;
                PortDataManager manager = pb.getUnusedBufferRaw();
                if (readTimestamp) {
                    manager.getTimestamp().deserialize(stream);
                }
                manager.getObject().deserialize(stream, getInternalEncoding());
                pb.publishFromNet(manager, changeType);
            } else if (isCCType()) {
                CCNetPort pb = (CCNetPort)wrapped;
                CCPortDataManagerTL manager = ThreadLocalCache.get().getUnusedBuffer(pb.getDataType());
                if (readTimestamp) {
                    manager.getTimestamp().deserialize(stream);
                }
                manager.getObject().deserialize(stream, getInternalEncoding());
                pb.publishFromNet(manager, changeType);
            } else { // interface port
                throw new RuntimeException("Method calls are not handled using this mechanism");
            }
            anotherValue = stream.readBoolean();
        } while (anotherValue);
    }

    /**
     * Wrapped cc port
     */
    public class CCNetPort extends CCPortBase {

        public CCNetPort(PortCreationInfo pci) {
            super(pci);
            super.addPortListenerRaw(NetPort.this);
        }

        public void publishFromNet(CCPortDataManagerTL readObject, byte changedFlag) {

            // Publish all pushed data from the network. This avoids problems in finstruct.
            // Compared to tranferring the stuff over the network,
            // effort for publishing the stuff locally, is negligible.
            /*if (changedFlag != AbstractPort.CHANGED_INITIAL) {
              check ...
            }*/

            ThreadLocalCache tc = ThreadLocalCache.getFast();
            if (isOutputPort()) {

                super.publish(tc, readObject, false, changedFlag);
            } else {
                // reverse push: reset changed flag - since this change comes from the net and needs not to be propagated
                getPort().resetChanged();
                // not entirely thread-safe: if changed flag is set now - value from the net will be published back - usually not a problem
                // dual-way ports are somewhat ugly anyway

                super.publish(tc, readObject, true, changedFlag);
                if (!isOutputPort() && getOutgoingConnectionCount() > 0) { // we have proxy port on server => input widget could be connected
                    super.publish(tc, readObject, false, changedFlag);
                }
            }
        }

        @Override
        public NetPort asNetPort() {
            return NetPort.this;
        }

        @Override
        protected synchronized void prepareDelete() {
            super.removePortListenerRaw(NetPort.this);
            super.prepareDelete();
            NetPort.this.prepareDelete();
        }

        @Override
        protected void postChildInit() {
            super.postChildInit();
            NetPort.this.postChildInit();
        }

        @Override
        protected void preChildInit() {
            super.preChildInit();
            NetPort.this.preChildInit();
        }

        @Override
        public void notifyDisconnect() {
            super.notifyDisconnect();
            NetPort.this.notifyDisconnect();
        }

        public void updateFlags(int flags) {
            setFlag(flags & Flag.NON_CONSTANT_FLAGS);
        }

        @Override
        public boolean propagateStrategy(AbstractPort pushWanter, AbstractPort newConnectionPartner) {
            if (isOutputPort() && isInitialized()) {
                if (super.propagateStrategy(null, null)) { // we don't want to push ourselves directly - unless there's no change
                    propagateStrategyOverTheNet();
                    return true;
                } else {
                    if (pushWanter != null) {
                        super.initialPushTo(pushWanter, false);
                    }
                }
                return false;
            } else {
                return super.propagateStrategy(pushWanter, newConnectionPartner);
            }
        }

        public void propagateStrategy(short strategy) {
            setFlag(Flag.PUSH_STRATEGY, strategy > 0);
            //this.strategy = strategy;
            super.setMaxQueueLength(strategy);
        }

        @Override
        protected void connectionRemoved(AbstractPort partner, boolean partnerIsDestination) {
            NetPort.this.connectionRemoved();
        }

        @Override
        protected void connectionAdded(AbstractPort partner, boolean partnerIsDestination) {
            NetPort.this.connectionAdded();
        }

        @Override
        protected void initialPushTo(AbstractPort target, boolean reverse) {
            if (reverse) {

                // do we have other reverse push listeners? - in this case, there won't be coming anything new from the network => immediately push
                ArrayWrapper<CCPortBase> it = edgesDest.getIterable();
                for (int i = 0, n = it.size(); i < n; i++) {
                    AbstractPort port = it.get(i);
                    if (port != null && port != target && port.isReady() && port.getFlag(Flag.PUSH_STRATEGY_REVERSE)) {
                        super.initialPushTo(target, reverse);
                        return;
                    }
                }

            } else {
                // do nothing ... since remote port will do this
            }
        }
    }

    /**
     * Wrapped standard port
     */
    public class StdNetPort extends PortBase {

        public StdNetPort(PortCreationInfo pci) {
            super(pci);
            super.addPortListenerRaw(NetPort.this);
        }

        public void publishFromNet(PortDataManager readObject, byte changedFlag) {
            if (!isOutputPort()) {
                // reverse push: reset changed flag - since this change comes from the net and needs not to be propagated
                getPort().resetChanged();
                // not entirely thread-safe: if changed flag is set now - value from the net will be published back - usually not a problem
                // dual-way ports are somewhat ugly anyway
            }
            super.publish(readObject, !isOutputPort(), changedFlag);
            if (!isOutputPort() && getOutgoingConnectionCount() > 0) { // we have proxy port on server => input widget could be connected
                super.publish(readObject, false, changedFlag);
            }
        }

        @Override
        public NetPort asNetPort() {
            return NetPort.this;
        }

        @Override
        protected synchronized void prepareDelete() {
            super.removePortListenerRaw(NetPort.this);
            super.prepareDelete();
            NetPort.this.prepareDelete();
        }

        @Override
        protected void postChildInit() {
            super.postChildInit();
            NetPort.this.postChildInit();
        }

        @Override
        protected void preChildInit() {
            super.preChildInit();
            NetPort.this.preChildInit();
        }

        @Override
        public void notifyDisconnect() {
            super.notifyDisconnect();
            NetPort.this.notifyDisconnect();
        }

        public void updateFlags(int flags) {
            setFlag(flags & Flag.NON_CONSTANT_FLAGS);
        }

        @Override
        public boolean propagateStrategy(AbstractPort pushWanter, AbstractPort newConnectionPartner) {
            if (isOutputPort() && isInitialized()) {
                if (super.propagateStrategy(null, null)) { // we don't want to push ourselves directly - unless there's no change
                    propagateStrategyOverTheNet();
                    return true;
                } else {
                    if (pushWanter != null) {
                        super.initialPushTo(pushWanter, false);
                    }
                }
                return false;
            } else {
                return super.propagateStrategy(pushWanter, newConnectionPartner);
            }
        }

        public void propagateStrategy(short strategy) {
            setFlag(Flag.PUSH_STRATEGY, strategy > 0);
            super.setMaxQueueLength(strategy);
        }

        @Override
        protected void connectionRemoved(AbstractPort partner, boolean partnerIsDestination) {
            NetPort.this.connectionRemoved();
        }

        @Override
        protected void connectionAdded(AbstractPort partner, boolean partnerIsDestination) {
            NetPort.this.connectionAdded();
        }

        @Override
        protected void initialPushTo(AbstractPort target, boolean reverse) {
            if (reverse) {

                // do we have other reverse push listeners? - in this case, there won't be coming anything new from the network => immediately push
                ArrayWrapper<PortBase> it = edgesDest.getIterable();
                for (int i = 0, n = it.size(); i < n; i++) {
                    AbstractPort port = it.get(i);
                    if (port != null && port != target && port.isReady() && port.getFlag(Flag.PUSH_STRATEGY_REVERSE)) {
                        super.initialPushTo(target, reverse);
                        return;
                    }
                }

            } else {
                // do nothing ... since remote port will do this
            }
        }
    }

    /**
     * Wrapped standard port
     */
    public class RPCNetPort extends RPCPort { /*implements Callable<MethodCall>*/

        public RPCNetPort(PortCreationInfo pci) {
            super(pci, null);
            //setCallHandler(this);
        }

        @Override
        public NetPort asNetPort() {
            return NetPort.this;
        }

        @Override
        protected synchronized void prepareDelete() {
            super.prepareDelete();
            NetPort.this.prepareDelete();
        }

        @Override
        protected void postChildInit() {
            super.postChildInit();
            NetPort.this.postChildInit();
        }

        @Override
        protected void preChildInit() {
            super.preChildInit();
            NetPort.this.preChildInit();
        }

        @Override
        public void notifyDisconnect() {
            super.notifyDisconnect();
            NetPort.this.notifyDisconnect();
        }

        public void updateFlags(int flags) {
            setFlag(flags & Flag.NON_CONSTANT_FLAGS);
        }

        @Override
        protected void connectionRemoved(AbstractPort partner, boolean partnerIsDestination) {
            NetPort.this.connectionRemoved();
        }

        @Override
        protected void connectionAdded(AbstractPort partner, boolean partnerIsDestination) {
            NetPort.this.connectionAdded();
        }

        @Override
        public void sendCall(AbstractCall callToSend) {
            NetPort.this.sendCall(callToSend);
        }

//        @Override
//        public void sendAsyncCall(MethodCall mc) {
//            mc.setupAsynchCall();
//            NetPort.this.sendCall(mc);
//        }
//
//        @Override
//        public void sendSyncCallReturn(MethodCall mc) {
//            NetPort.this.sendCall(mc);
//        }
//
//        @Override
//        public MethodCall synchCallOverTheNet(MethodCall mc, int timeout) throws MethodCallException {
//            assert(mc.getMethod() != null);
//            return SynchMethodCallLogic.performSynchCall(mc, this, timeout);
//        }
//
//        @Override
//        public void invokeCall(MethodCall call) {
//            NetPort.this.sendCall(call);
//        }
    }

    /**
     * Remote port with unknown type
     *
     * Artificial construct for finstruct.
     * This way, ports with unknown types can be displayed and connected.
     */
    public class UnknownTypedNetPort extends AbstractPort {

        /** Edges emerging from this port */
        protected final EdgeList<AbstractPort> edgesSrc = new EdgeList<AbstractPort>();

        /** Edges ending at this port */
        protected final EdgeList<AbstractPort> edgesDest = new EdgeList<AbstractPort>();

        public UnknownTypedNetPort(PortCreationInfo pci) {
            super(pci);
            assert(isUnknownType());
            initLists(edgesSrc, edgesDest);
        }

        @Override
        public NetPort asNetPort() {
            return NetPort.this;
        }

        @Override
        protected synchronized void prepareDelete() {
            super.prepareDelete();
            NetPort.this.prepareDelete();
        }

        @Override
        protected void postChildInit() {
            super.postChildInit();
            NetPort.this.postChildInit();
        }

        @Override
        protected void preChildInit() {
            super.preChildInit();
            NetPort.this.preChildInit();
        }

        @Override
        public void notifyDisconnect() {
            NetPort.this.notifyDisconnect();
        }

        public void updateFlags(int flags) {
            setFlag(flags & Flag.NON_CONSTANT_FLAGS);
        }

        @Override
        protected void connectionRemoved(AbstractPort partner, boolean partnerIsDestination) {
            NetPort.this.connectionRemoved();
        }

        @Override
        protected void connectionAdded(AbstractPort partner, boolean partnerIsDestination) {
            NetPort.this.connectionAdded();
        }

        @Override
        protected void initialPushTo(AbstractPort target, boolean reverse) {}

        @Override
        protected void setMaxQueueLengthImpl(int length) {}

        @Override
        protected int getMaxQueueLengthImpl() {
            return 0;
        }

        @Override
        protected void clearQueueImpl() {}

        @Override
        public void forwardData(AbstractPort other) {}

    }

    @Override
    public void portChanged(AbstractPort origin, Object value) {
        portChanged();
    }

//    /**
//     * Process incoming (pull) returning call from the network
//     *
//     * @param mc Call
//     */
//    public void handleCallReturnFromNet(AbstractCall mc) {
//        SynchMethodCallLogic.handleMethodReturn(mc);
//    }
//
    protected abstract void sendCall(AbstractCall mc);
//
//    public abstract void sendCallReturn(AbstractCall mc);

    protected abstract void propagateStrategyOverTheNet();

    /**
     * Process incoming strategy update from the net
     *
     * @param new strategy
     */
    public void propagateStrategyFromTheNet(short strategy) {
        if (!getPort().isOutputPort()) { // only input ports are relevant for strategy changes
            AbstractPort ap = getPort();
            if (ap instanceof StdNetPort) {
                ((StdNetPort)ap).propagateStrategy(strategy);
            } else if (ap instanceof CCNetPort) {
                ((CCNetPort)ap).propagateStrategy(strategy);
            } else {
                // ignore
            }
        }
    }

    /**
     * @return Targets of remote edges
     */
    public List<AbstractPort> getRemoteEdgeDestinations() {
        ArrayList<AbstractPort> result = new ArrayList<AbstractPort>();
        getRemoteEdgeDestinations(result);
        return result;
    }

    /**
     * @param resultList List to put the targets of the remote edges in (filled after call) - edges with reverse direction are the last in the list
     * @return Index of the first edge in reverse direction
     */
    public abstract int getRemoteEdgeDestinations(List<AbstractPort> resultList);

    /**
     * @return Data type of port in remote runtime (with unknown types this might be different from type of local port)
     */
    public DataTypeBase getDataType() {
        return wrapped.getDataType();
    }

    /**
     * @return Data type to use when writing data to the network
     */
    public Serialization.DataEncoding getEncoding() {
        return encoding;
    }

    /**
     * @param enc Data type to use when writing data to the network
     */
    public void setEncoding(Serialization.DataEncoding enc) {
        encoding = enc;
    }

    /**
     * @return Data type to actually use when writing data to the network. This can be different from getEncoding() with adapter types.
     */
    public Serialization.DataEncoding getInternalEncoding() {
        return internalEncoding;
    }
}
