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
package org.finroc.core.port.net;

import java.util.List;

import org.finroc.core.CoreFlags;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.cc.CCPullRequestHandler;
import org.finroc.core.port.cc.CCQueueFragmentRaw;
import org.finroc.core.port.rpc.AbstractCall;
import org.finroc.core.port.rpc.Callable;
import org.finroc.core.port.rpc.InterfaceNetPort;
import org.finroc.core.port.rpc.MethodCall;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.core.port.rpc.PullCall;
import org.finroc.core.port.rpc.SynchMethodCallLogic;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.port.std.PortDataReference;
import org.finroc.core.port.PortListener;
import org.finroc.core.port.std.PortQueueFragmentRaw;
import org.finroc.core.port.std.PullRequestHandler;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.portdatabase.SerializationHelper;
import org.finroc.core.portdatabase.UnknownType;
import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.CustomPtr;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.IncludeClass;
import org.rrlib.finroc_core_utils.jc.annotation.InitInBody;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.Virtual;
import org.rrlib.finroc_core_utils.jc.log.LogUser;
import org.rrlib.finroc_core_utils.rtti.GenericObject;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.Serialization;

/**
 * @author max
 *
 * Port that is used for networking.
 * Uniform wrapper class for Std, CC, and Interface ports.
 */
@SuppressWarnings("rawtypes")
@Ptr
@IncludeClass(PullCall.class)
public abstract class NetPort extends LogUser implements PortListener {

    /** Default timeout for pulling data over the net */
    public final static int PULL_TIMEOUT = 1000;

    /** TCPServerConnection or RemoteServer instance that this port belongs to */
    protected final @Ptr Object belongsTo;

    /** Last time the value was updated (used to make sure that minimum update interval is kept) */
    public long lastUpdate = Long.MIN_VALUE;

    /** Wrapped port */
    private final @Ptr AbstractPort wrapped;

    /** Handle of Remote port */
    protected int remoteHandle;

    /** Finroc Type info type */
    protected final FinrocTypeInfo.Type ftype;

    /** Data type to use when writing data to the network */
    private Serialization.DataEncoding encoding = Serialization.DataEncoding.BINARY;

    @InitInBody("wrapped")
    public NetPort(PortCreationInfo pci, @Ptr Object belongsTo) {
        // keep most these flags
        int f = pci.flags & (PortFlags.ACCEPTS_DATA | PortFlags.EMITS_DATA | PortFlags.MAY_ACCEPT_REVERSE_DATA | PortFlags.IS_OUTPUT_PORT |
                             PortFlags.IS_BULK_PORT | PortFlags.IS_EXPRESS_PORT | PortFlags.NON_STANDARD_ASSIGN /*| PortFlags.IS_CC_PORT | PortFlags.IS_INTERFACE_PORT*/ |
                             CoreFlags.ALTERNATE_LINK_ROOT | CoreFlags.GLOBALLY_UNIQUE_LINK | CoreFlags.FINSTRUCTED);

        // set either emit or accept data
        f |= ((f & PortFlags.IS_OUTPUT_PORT) > 0) ? PortFlags.EMITS_DATA : PortFlags.ACCEPTS_DATA;
        f |= CoreFlags.NETWORK_ELEMENT | PortFlags.IS_VOLATILE;
        if ((f & PortFlags.IS_OUTPUT_PORT) == 0) { // we always have a queue with (remote) input ports - to be able to switch
            f |= PortFlags.HAS_QUEUE;
            if (pci.maxQueueSize > 1) {
                f |= PortFlags.USES_QUEUE;
            }
        }
        ftype = FinrocTypeInfo.get(pci.dataType).getType();
        UnknownType ut = (pci.dataType instanceof UnknownType) ? (UnknownType)pci.dataType : null;
        if (isStdType() || isMethodType() || (ut != null && ut.isAdaptable())) {
            f |= PortFlags.SPECIAL_REUSE_QUEUE; // different data types may be incoming - cc types are thread local
        }
        pci.flags = f;
        this.belongsTo = belongsTo;

        //JavaOnlyBlock
        if (isUnknownType()) {
            if (ut.isAdaptable()) {
                encoding = ut.determineEncoding();
                wrapped = new StdNetPort(pci);
            } else {
                wrapped = new UnknownTypedNetPort(pci);
            }
            return;
        }

        wrapped = (isMethodType() ? (AbstractPort)new InterfaceNetPortImpl(pci) :
                   (isCCType() ? (AbstractPort)new CCNetPort(pci) :
                    (AbstractPort)new StdNetPort(pci)));
    }

    /** Delete port */
    public void managedDelete() {
        getPort().managedDelete();
    }

    /** Helper methods for data type type */
    private boolean isStdType() {
        return ftype == FinrocTypeInfo.Type.STD;
    }

    public boolean isCCType() {
        return ftype == FinrocTypeInfo.Type.CC;
    }

    private boolean isUnknownType() {
        return ftype.ordinal() >= FinrocTypeInfo.Type.UNKNOWN_STD.ordinal();
    }

    private boolean isUnknownAdaptableType() {
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
        final int keepFlags = CoreFlags.STATUS_FLAGS;
        int curFlags = getPort().getAllFlags() & keepFlags;
        flags &= ~(keepFlags);
        flags |= curFlags;

        //JavaOnlyBlock
        if (isUnknownType() && (!isUnknownAdaptableType())) {
            ((UnknownTypedNetPort)wrapped).updateFlags(flags);
            return;
        }

        if (isStdType() || isTransactionType() || isUnknownAdaptableType()) {
            ((StdNetPort)wrapped).updateFlags(flags);
        } else if (isCCType()) {
            ((CCNetPort)wrapped).updateFlags(flags);
        } else {
            ((InterfaceNetPortImpl)wrapped).updateFlags(flags);
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
    public @Ptr Object getBelongsTo() {
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
    @Virtual protected void preChildInit() {}

    /**
     * Initializes this runtime element.
     * The tree structure should be established by now (Uid is valid)
     * so final initialization can be made.
     *
     * Called before children are initialized
     */
    @Virtual protected void postChildInit() {}

    /**
     * Prepares element for deletion.
     * Ports, for instance, are removed from edge lists etc.
     * The final deletion will be done by the GarbageCollector thread after
     * a few seconds (to ensure no other thread is working on this object
     * any more).
     */
    @Virtual protected void prepareDelete() {}

    /**
     * Notify port of disconnect
     */
    @Virtual protected void notifyDisconnect() {}

    /**
     * Called whenever current value of port has changed
     */
    @Virtual protected void portChanged() {}

    /**
     * Called whenever connection was established
     */
    @Virtual protected void newConnection() {}

    /**
     * Called whenever connection was removed
     */
    @Virtual protected void connectionRemoved() {}

    /**
     * Decode incoming data from stream
     *
     * @param ci Stream
     * @param timestamp Time stamp
     * @param changedFlag Type of change
     */
    public void receiveDataFromStream(@Ref InputStreamBuffer ci, long timestamp, byte changedFlag) {
        assert(getPort().isReady());
        Serialization.DataEncoding enc = ci.readEnum(Serialization.DataEncoding.class);
        if (isStdType() || isTransactionType() || isUnknownAdaptableType()) {
            StdNetPort pb = (StdNetPort)wrapped;
            ci.setFactory(pb);
            do {
                pb.publishFromNet((PortDataManager)SerializationHelper.readObject(ci, wrapped.getDataType(), null, enc).getManager(), changedFlag);
            } while (ci.readBoolean());
            ci.setFactory(null);
        } else if (isCCType()) {
            CCNetPort pb = (CCNetPort)wrapped;
            do {
                pb.publishFromNet((CCPortDataManagerTL)SerializationHelper.readObject(ci, wrapped.getDataType(), null, enc).getManager(), changedFlag);
            } while (ci.readBoolean());
        } else { // interface port
            throw new RuntimeException("Method calls are not handled using this mechanism");
        }
    }

    /**
     * Write data to stream
     *
     * @param co Stream
     * @param startTime Time stamp
     */
    public void writeDataToNetwork(@Ref OutputStreamBuffer co, long startTime) {

        boolean useQ = wrapped.getFlag(PortFlags.USES_QUEUE);
        boolean first = true;
        co.writeEnum(encoding);
        if (isStdType() || isTransactionType() || isUnknownAdaptableType()) {
            StdNetPort pb = (StdNetPort)wrapped;
            if (!useQ) {
                PortDataManager pd = pb.getLockedUnsafeRaw(true);
                SerializationHelper.writeObject(co, getDataType(), pd.getObject(), encoding);
                pd.releaseLock();
            } else {
                @InCpp("PortQueueFragmentRaw fragment;")
                @PassByValue PortQueueFragmentRaw fragment = ThreadLocalCache.getFast().tempFragment;
                pb.dequeueAllRaw(fragment);
                @Const PortDataReference pd = null;
                while ((pd = (PortDataReference)fragment.dequeue()) != null) {
                    if (!first) {
                        co.writeBoolean(true);
                    }
                    first = false;
                    SerializationHelper.writeObject(co, getDataType(), pd.getManager().getObject(), encoding);
                    pd.getManager().releaseLock();
                }
            }
        } else if (isCCType()) {
            CCNetPort pb = (CCNetPort)wrapped;
            if (!useQ) {
                CCPortDataManager ccitc = pb.getInInterThreadContainer(true);
                SerializationHelper.writeObject(co, getDataType(), ccitc.getObject(), encoding);
                ccitc.recycle2();
            } else {
                @InCpp("CCQueueFragmentRaw fragment;")
                @PassByValue CCQueueFragmentRaw fragment = ThreadLocalCache.getFast().tempCCFragment;
                pb.dequeueAllRaw(fragment);
                CCPortDataManager pd = null;
                while ((pd = fragment.dequeueUnsafe()) != null) {
                    if (!first) {
                        co.writeBoolean(true);
                    }
                    first = false;
                    SerializationHelper.writeObject(co, getDataType(), pd.getObject(), encoding);
                    pd.recycle2();
                }
            }
        } else { // interface port
            throw new RuntimeException("Method calls are not handled using this mechanism");
        }
        co.writeBoolean(false);
    }

    /**
     * Wrapped cc port
     */
    public class CCNetPort extends CCPortBase implements CCPullRequestHandler, Callable<PullCall> {

        public CCNetPort(PortCreationInfo pci) {
            super(pci);
            super.addPortListenerRaw(NetPort.this);
            super.setPullRequestHandler(this);
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
            setFlag(flags & PortFlags.NON_CONSTANT_FLAGS);
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
            setFlag(PortFlags.PUSH_STRATEGY, strategy > 0);
            //this.strategy = strategy;
            super.setMaxQueueLength(strategy);
        }

        @Override
        protected void connectionRemoved(AbstractPort partner) {
            NetPort.this.connectionRemoved();
        }

        @Override
        protected void newConnection(AbstractPort partner) {
            NetPort.this.newConnection();
        }

        @Override
        public boolean pullRequest(CCPortBase origin, CCPortDataManagerTL resultBuffer, boolean intermediateAssign) {
            PullCall pc = ThreadLocalCache.getFast().getUnusedPullCall();
            pc.setupPullCall(remoteHandle, intermediateAssign, encoding);
//          pc.setLocalPortHandle(getHandle());
            try {
                pc = SynchMethodCallLogic.performSynchCall(pc, this, PULL_TIMEOUT);
                if (pc.hasException()) {
                    getRaw(resultBuffer.getObject(), true);
                } else {
                    resultBuffer.getObject().deepCopyFrom(pc.getPulledBuffer(), null);
                }
                pc.recycle();
            } catch (MethodCallException e) {
                getRaw(resultBuffer.getObject(), true);
            }
            return true;
        }

        @Override
        public void invokeCall(PullCall call) {
            NetPort.this.sendCall(call);
        }

        @Override
        protected void initialPushTo(AbstractPort target, boolean reverse) {
            if (reverse) {

                // do we have other reverse push listeners? - in this case, there won't be coming anything new from the network => immediately push
                @Ptr ArrayWrapper<CCPortBase> it = edgesDest.getIterable();
                for (int i = 0, n = it.size(); i < n; i++) {
                    @Ptr AbstractPort port = it.get(i);
                    if (port != null && port != target && port.isReady() && port.getFlag(PortFlags.PUSH_STRATEGY_REVERSE)) {
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
    public class StdNetPort extends PortBase implements PullRequestHandler, Callable<PullCall> {

        public StdNetPort(PortCreationInfo pci) {
            super(pci);
            super.addPortListenerRaw(NetPort.this);
            super.setPullRequestHandler(this);
        }

        public void publishFromNet(PortDataManager readObject, byte changedFlag) {
            if (!isOutputPort()) {
                // reverse push: reset changed flag - since this change comes from the net and needs not to be propagated
                getPort().resetChanged();
                // not entirely thread-safe: if changed flag is set now - value from the net will be published back - usually not a problem
                // dual-way ports are somewhat ugly anyway
            }
            super.publish(readObject, !isOutputPort(), changedFlag);
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
            setFlag(flags & PortFlags.NON_CONSTANT_FLAGS);
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
            setFlag(PortFlags.PUSH_STRATEGY, strategy > 0);
            super.setMaxQueueLength(strategy);
        }

        @Override
        protected void connectionRemoved(AbstractPort partner) {
            NetPort.this.connectionRemoved();
        }

        @Override
        protected void newConnection(AbstractPort partner) {
            NetPort.this.newConnection();
        }

        @Override
        public PortDataManager pullRequest(PortBase origin, byte addLocks, boolean intermediateAssign) {
            assert(addLocks > 0);
            PullCall pc = ThreadLocalCache.getFast().getUnusedPullCall();
            pc.setupPullCall(remoteHandle, intermediateAssign, encoding);

//          pc.setLocalPortHandle(getHandle());
            try {
                pc = SynchMethodCallLogic.performSynchCall(pc, this, PULL_TIMEOUT);
                if (pc.hasException()) {
                    // return local port data
                    PortDataManager pd = lockCurrentValueForRead();
                    pd.getCurrentRefCounter().addLocks((byte)(addLocks - 1)); // we already have one lock
                    pc.recycle();
                    return pd;
                } else {
                    @CustomPtr("tPortDataPtr") GenericObject o = pc.getPulledBuffer();
                    PortDataManager pd = (PortDataManager)o.getManager();
                    assert(pd.isLocked());
                    pd.getCurrentRefCounter().addLocks((byte)(addLocks));
                    pc.recycle();
                    return pd;
                }

            } catch (MethodCallException e) {
                // return local port data
                PortDataManager pd = lockCurrentValueForRead();
                pd.getCurrentRefCounter().addLocks((byte)(addLocks - 1)); // we already have one lock
                //pc.recycle();  // Careful, this would recycle object twice
                return pd;
            }
        }


        @Override
        public void invokeCall(PullCall call) {
            NetPort.this.sendCall(call);
        }

        @Override
        protected void initialPushTo(AbstractPort target, boolean reverse) {
            if (reverse) {

                // do we have other reverse push listeners? - in this case, there won't be coming anything new from the network => immediately push
                @Ptr ArrayWrapper<PortBase> it = edgesDest.getIterable();
                for (int i = 0, n = it.size(); i < n; i++) {
                    @Ptr AbstractPort port = it.get(i);
                    if (port != null && port != target && port.isReady() && port.getFlag(PortFlags.PUSH_STRATEGY_REVERSE)) {
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
    public class InterfaceNetPortImpl extends InterfaceNetPort implements Callable<MethodCall> {

        public InterfaceNetPortImpl(PortCreationInfo pci) {
            super(pci);
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
            setFlag(flags & PortFlags.NON_CONSTANT_FLAGS);
        }

        @Override
        protected void connectionRemoved(AbstractPort partner) {
            NetPort.this.connectionRemoved();
        }

        @Override
        protected void newConnection(AbstractPort partner) {
            NetPort.this.newConnection();
        }

        @Override
        public void sendAsyncCall(MethodCall mc) {
            mc.setupAsynchCall();
            NetPort.this.sendCall(mc);
        }

        @Override
        public void sendSyncCallReturn(MethodCall mc) {
            NetPort.this.sendCall(mc);
        }

        @Override
        public MethodCall synchCallOverTheNet(MethodCall mc, int timeout) throws MethodCallException {
            assert(mc.getMethod() != null);
            return SynchMethodCallLogic.performSynchCall(mc, this, timeout);
        }

        @Override
        public void invokeCall(MethodCall call) {
            NetPort.this.sendCall(call);
        }
    }

    /**
     * Remote port with unknown type
     *
     * Artificial construct for finstruct.
     * This way, ports with unknown types can be displayed and connected.
     */
    @JavaOnly
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
            setFlag(flags & PortFlags.NON_CONSTANT_FLAGS);
        }

        @Override
        protected void connectionRemoved(AbstractPort partner) {
            NetPort.this.connectionRemoved();
        }

        @Override
        protected void newConnection(AbstractPort partner) {
            NetPort.this.newConnection();
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

    @Override @JavaOnly
    public void portChanged(AbstractPort origin, Object value) {
        portChanged();
    }

    /**
     * Process incoming (pull) returning call from the network
     *
     * @param mc Call
     */
    public void handleCallReturnFromNet(AbstractCall mc) {
        SynchMethodCallLogic.handleMethodReturn(mc);
    }

    protected abstract void sendCall(AbstractCall mc);

    public abstract void sendCallReturn(AbstractCall mc);

    protected abstract void propagateStrategyOverTheNet();

    /*Cpp
    virtual void portChanged(AbstractPort* origin, const void* const& value)
    {
      portChanged();
    }
     */

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
    @JavaOnly
    public abstract List<AbstractPort> getRemoteEdgeDestinations();

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
}