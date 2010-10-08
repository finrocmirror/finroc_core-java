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
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.port.cc.CCPortDataContainer;
import org.finroc.core.port.cc.CCPortListener;
import org.finroc.core.port.cc.CCPullRequestHandler;
import org.finroc.core.port.cc.CCQueueFragment;
import org.finroc.core.port.rpc.AbstractCall;
import org.finroc.core.port.rpc.Callable;
import org.finroc.core.port.rpc.InterfaceNetPort;
import org.finroc.core.port.rpc.MethodCall;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.core.port.rpc.PullCall;
import org.finroc.core.port.rpc.SynchMethodCallLogic;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortData;
import org.finroc.core.port.std.PortDataReference;
import org.finroc.core.port.std.PortListener;
import org.finroc.core.port.std.PortQueueFragment;
import org.finroc.core.port.std.PullRequestHandler;
import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.InitInBody;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.log.LogUser;

/**
 * @author max
 *
 * Port that is used for networking.
 * Uniform wrapper class for Std, CC, and Interface ports.
 */
@SuppressWarnings("rawtypes")
@Ptr
@IncludeClass(PullCall.class)
public abstract class NetPort extends LogUser implements PortListener, CCPortListener {

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
        if (pci.dataType.isStdType() || pci.dataType.isMethodType()) {
            f |= PortFlags.SPECIAL_REUSE_QUEUE; // different data types may be incoming - cc types are thread local
        }
        pci.flags = f;
        this.belongsTo = belongsTo;
        wrapped = (pci.dataType.isMethodType() ? (AbstractPort)new InterfaceNetPortImpl(pci) :
                   (pci.dataType.isCCType() ? (AbstractPort)new CCNetPort(pci) :
                    (AbstractPort)new StdNetPort(pci)));
    }

    /** Delete port */
    public void managedDelete() {
        getPort().managedDelete();
    }

    public void updateFlags(int flags) {

        // process flags... keep DELETE and READY flags
        final int keepFlags = CoreFlags.STATUS_FLAGS;
        int curFlags = getPort().getAllFlags() & keepFlags;
        flags &= ~(keepFlags);
        flags |= curFlags;

        if (wrapped.getDataType().isStdType() || wrapped.getDataType().isTransactionType()) {
            ((StdNetPort)wrapped).updateFlags(flags);
        } else if (wrapped.getDataType().isCCType()) {
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
     * Port, for instance, are removed from edge lists etc.
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
     */
    public void receiveDataFromStream(CoreInput ci, long timestamp, byte changedFlag) {
        assert(getPort().isReady());
        if (wrapped.getDataType().isStdType() || wrapped.getDataType().isTransactionType()) {
            StdNetPort pb = (StdNetPort)wrapped;
            ci.setBufferSource(pb);
            do {
                pb.publishFromNet((PortData)ci.readObject(), changedFlag);
            } while (ci.readBoolean());
            ci.setBufferSource(null);
        } else if (wrapped.getDataType().isCCType()) {
            CCNetPort pb = (CCNetPort)wrapped;
            do {
                pb.publishFromNet((CCPortDataContainer<?>)ci.readObject(), changedFlag);
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
    public void writeDataToNetwork(CoreOutput co, long startTime) {

        boolean useQ = wrapped.getFlag(PortFlags.USES_QUEUE);
        boolean first = true;
        if (wrapped.getDataType().isStdType() || wrapped.getDataType().isTransactionType()) {
            StdNetPort pb = (StdNetPort)wrapped;
            if (!useQ) {
                @Const PortData pd = pb.getLockedUnsafeRaw();
                co.writeObject(pd);
                pd.getManager().releaseLock();
            } else {
                @InCpp("PortQueueFragment<PortData> fragment;")
                @PassByValue PortQueueFragment<PortData> fragment = ThreadLocalCache.getFast().tempFragment;
                pb.dequeueAllRaw(fragment);
                @Const PortDataReference pd = null;
                while ((pd = (PortDataReference)fragment.dequeue()) != null) {
                    if (!first) {
                        co.writeBoolean(true);
                    }
                    first = false;
                    co.writeObject(pd.getData());
                    pd.getManager().releaseLock();
                }
            }
        } else if (wrapped.getDataType().isCCType()) {
            CCNetPort pb = (CCNetPort)wrapped;
            if (!useQ) {
                CCInterThreadContainer<?> ccitc = pb.getInInterThreadContainer();
                co.writeObject(ccitc);
                ccitc.recycle2();
            } else {
                @InCpp("CCQueueFragment<CCPortData> fragment;")
                @PassByValue CCQueueFragment<CCPortData> fragment = ThreadLocalCache.getFast().tempCCFragment;
                pb.dequeueAllRaw(fragment);
                CCInterThreadContainer<?> pd = null;
                while ((pd = (CCInterThreadContainer<?>)fragment.dequeue()) != null) {
                    if (!first) {
                        co.writeBoolean(true);
                    }
                    first = false;
                    co.writeObject(pd);
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

        public void publishFromNet(CCPortDataContainer<?> readObject, byte changedFlag) {

            if (changedFlag != AbstractPort.CHANGED_INITIAL) { // always publish initial pushes

                // only publish if value has actually changed...
                CCPortDataContainer<?> curData = this.getLockedUnsafeInContainer();
                boolean equal = curData.contentEquals(readObject.getDataPtr());
                curData.releaseLock();  // unlock value that we just locked for comparison
                if (equal) {

                    // laborious version of recycle
                    readObject.setRefCounter(1);
                    readObject.releaseLock();
                    return;
                }
            }

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

//      protected void setCallHandler(CallHandler ch) {
//          setCallHandler(ch);
//      }

        public void updateFlags(int flags) {
            setFlag(flags & PortFlags.NON_CONSTANT_FLAGS);
        }

//      @Override
//      public boolean pullValueRaw(PullCall call, ThreadLocalCache tc) {
//          if (isOutputPort()) { // pull to forward over the network?
//              assert(!call.isReturning(true));
//              sendCall(call);
//              return false;
//          } else { // returning pull call
//              assert(call.isReturning(true));
//              return super.pullValueRaw(call, tc);
//          }
//      }
//
//      @Override
//      public void handleCallReturn(AbstractCall call) {
//          if (isOutputPort()) {
//              assert(call.isReturning(true));
//              super.handleCallReturn(call);
//          } else {
//              assert(!call.isReturning(true));
//              NetPort.this.sendCallReturn(call);
//          }
//      }

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

//      @Override
//      protected short getStrategyRequirement() {
//          if (isOutputPort()) {
//              return 0; // should be zero - since it's a proxy
//          } else {
//              return getStrategy();
//          }
//      }

        public void propagateStrategy(short strategy) {
            setFlag(PortFlags.PUSH_STRATEGY, strategy > 0);
            //this.strategy = strategy;
            super.setMaxQueueLength(strategy);
        }

//      public boolean handlePullFromNet(PullCall mc) {
//          return super.pullValueRaw(mc, ThreadLocalCache.getFast());
//      }
//
//      public void handleCallReturnFromNet(AbstractCall mc) {
//          super.handleCallReturn(mc);
//      }

        @Override
        protected void connectionRemoved(AbstractPort partner) {
            NetPort.this.connectionRemoved();
        }

        @Override
        protected void newConnection(AbstractPort partner) {
            NetPort.this.newConnection();
        }

        @Override
        public void pullRequest(CCPortBase origin, CCPortData resultBuffer) {
            PullCall pc = ThreadLocalCache.getFast().getUnusedPullCall();
            pc.setRemotePortHandle(remoteHandle);
//          pc.setLocalPortHandle(getHandle());
            try {
                pc = SynchMethodCallLogic.performSynchCall(pc, this, PULL_TIMEOUT);
                if (pc.hasException()) {
                    getRaw(resultBuffer);
                } else {
                    CCInterThreadContainer<?> c;

                    //JavaOnlyBlock
                    c = pc. < CCInterThreadContainer<? >> getParam(0);

                    //Cpp pc->getParam(0, c);

                    c.assignTo(resultBuffer);
                    c.recycle2();
                }
                pc.recycle();

            } catch (MethodCallException e) {
                getRaw(resultBuffer);
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

        public void publishFromNet(PortData readObject, byte changedFlag) {
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

//      @Override
//      public boolean pullValueRaw(PullCall call) {
//          if (isOutputPort()) { // pull to forward over the network?
//              assert(!call.isReturning(true));
//              sendCall(call);
//              return false;
//          } else { // returning pull call
//              assert(call.isReturning(true));
//              return super.pullValueRaw(call);
//          }
//      }
//
//      @Override
//      public void handleCallReturn(AbstractCall call) {
//          if (isOutputPort()) {
//              assert(call.isReturning(true));
//              super.handleCallReturn(call);
//          } else {
//              assert(!call.isReturning(true));
//              NetPort.this.sendCallReturn(call);
//          }
//      }

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

//      @Override
//      protected short getStrategyRequirement() {
//          if (isOutputPort()) {
//              return 0; // should be zero - since it's a proxy
//          } else {
//              return getStrategy();
//          }
//      }

        public void propagateStrategy(short strategy) {
            setFlag(PortFlags.PUSH_STRATEGY, strategy > 0);
            super.setMaxQueueLength(strategy);
        }

//      public boolean handlePullFromNet(PullCall mc) {
//          return super.pullValueRaw(mc);
//      }
//
//      public void handleCallReturnFromNet(AbstractCall mc) {
//          SynchMethodCallLogic.
//      }

        @Override
        protected void connectionRemoved(AbstractPort partner) {
            NetPort.this.connectionRemoved();
        }

        @Override
        protected void newConnection(AbstractPort partner) {
            NetPort.this.newConnection();
        }

        @Override
        public PortData pullRequest(PortBase origin, byte addLocks) {
            assert(addLocks > 0);
            PullCall pc = ThreadLocalCache.getFast().getUnusedPullCall();
            pc.setRemotePortHandle(remoteHandle);
//          pc.setLocalPortHandle(getHandle());
            try {
                pc = SynchMethodCallLogic.performSynchCall(pc, this, PULL_TIMEOUT);
                if (pc.hasException()) {
                    // return local port data
                    @Const PortData pd = getLockedUnsafeRaw();
                    pd.getManager().getCurrentRefCounter().addLocks((byte)(addLocks - 1)); // we already have one lock
                    pc.recycle();
                    return pd;
                } else {
                    PortData pd;

                    //JavaOnlyBlock
                    pd = pc.<PortData>getParam(0);

                    //Cpp pc->getParam(0, pd);

                    pd.getManager().getCurrentRefCounter().addLocks((byte)(addLocks - 1)); // we already have one lock
                    pc.recycle();
                    return pd;
                }

            } catch (MethodCallException e) {
                // return local port data
                @Const PortData pd = getLockedUnsafeRaw();
                pd.getManager().getCurrentRefCounter().addLocks((byte)(addLocks - 1)); // we already have one lock
                pc.recycle();
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

//      public void receiveDataFromStream(CoreInput ci) {
//          MethodCall mc = (MethodCall)ci.readObject();
//          if (!mc.isReturning(true)) {
//              mc.pushCaller(belongsTo);
//              this.sendMethodCall(mc);
//          } else {
//              mc.returnToCaller();
//          }
//      }

//      @Override
//      protected void setCallHandler(CallHandler ch) {
//          super.setCallHandler(ch);
//      }

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

//      @Override
//      protected boolean handleCall(MethodCall mc, boolean deferredCall) {
//          NetPort.this.handleCall(mc);
//          return false;
//      }
//
//      @Override
//      public void returnValue(MethodCall mc) {
//          super.returnValue(mc);
//      }
//
//      @Override
//      public void sendMethodCall(MethodCall mc) {
//          super.sendMethodCall(mc);
//      }
//
//      public void handleCallReturnFromNet(AbstractCall mc) {
//          SynchMethodCallLogic.handleMethodReturn(mc);
//      }
//
//      @Override
//      public void handleMethodCall(MethodCall mc, byte methodId,
//              boolean deferred, long int1, long int2, long int3, double dbl1,
//              double dbl2, double dbl3, TypedObject obj1, TypedObject obj2) {
//          assert(false) : "Programming error: should not be called";
//      }

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

    @Override
    public void portChanged(PortBase origin, PortData value) {
        portChanged();
    }

    @Override
    public void portChanged(CCPortBase origin, CCPortData value) {
        portChanged();
    }

//  /**
//   * Process incoming pull call from the network
//   *
//   * @param mc PullCall
//   * @return Did call immediately return with valid result? - Should often be the case
//   */
//  public boolean handlePullFromNet(PullCall mc) {
//      AbstractPort ap = getPort();
//      if (ap instanceof StdNetPort) {
//          return ((StdNetPort)ap).handlePullFromNet(mc);
//      } else if (ap instanceof CCNetPort) {
//          return ((CCNetPort)ap).handlePullFromNet(mc);
//      } else {
//          throw new RuntimeException("Port can't handle call from net");
//      }
//  }
//
//  /**
//   * Process incoming (pull) returning call from the network
//   *
//   * @param mc Call
//   */
//  public void handleCallReturnFromNet(AbstractCall mc) {
//      SynchMethodCallLogic.handleMethodReturn(mc);
//      AbstractPort ap = getPort();
//      if (ap instanceof StdNetPort) {
//          ((StdNetPort)ap).handleCallReturnFromNet(mc);
//      } else if (ap instanceof CCNetPort) {
//          ((CCNetPort)ap).handleCallReturnFromNet(mc);
//      } else if (ap instanceof InterfaceNetPortImpl) {
//          ((InterfaceNetPortImpl)ap).handleCallReturnFromNet(mc);
//      } else {
//          throw new RuntimeException("Port can't handle call return from net");
//      }
//  }

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
     * @return Sources of remote edges
     */
    //@JavaOnly
    //public abstract List<FrameworkElement> getRemoteEdgeSources();
}