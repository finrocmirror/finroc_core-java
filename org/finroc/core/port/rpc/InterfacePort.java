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
package org.finroc.core.port.rpc;

import org.finroc.core.FrameworkElement;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.MultiTypePortDataBufferPool;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.std.PortData;
import org.finroc.core.portdatabase.DataType;
import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SizeT;

/**
 * @author max
 *
 * This is a port that can be used for remote procedure calls -
 * synchronous and asynchronous
 *
 * Server is source port.
 * Client is target port.
 * One source may have multiple targets. However, a target may only
 * have one source in order to receive only one return value.
 */
@Include("rrlib/finroc_core_utils/container/tSafeConcurrentlyIterableList.h")
public class InterfacePort extends AbstractPort {

    /** Edges emerging from this port */
    protected final EdgeList<InterfacePort> edgesSrc = new EdgeList<InterfacePort>();

    /** Edges ending at this port - maximum of one in this class */
    protected final EdgeList<InterfacePort> edgesDest = new EdgeList<InterfacePort>();

    /** Type of interface port (Server interface implementation, client interface, network port, simple call forwarding) */
    public static enum Type { Server, Client, Network, Routing }

    /** Type of interface port */
    private final Type type;

//  /** Does port handle method calls? In this case this points to the class that will handle the method calls */
//  private CallHandler callHandler;
//
//  /** Does port accept return values from asynchronous method calls? In this case, this points to the class that will handle them */
//  private ReturnHandler returnHandler;

    /** Pool with diverse data buffers */
    final @Ptr MultiTypePortDataBufferPool bufPool;

    public InterfacePort(String description, FrameworkElement parent, DataType dataType, Type type) {
        this(new PortCreationInfo(description, parent, dataType, 0), type);
    }

    public InterfacePort(String description, FrameworkElement parent, DataType dataType, Type type, int customFlags) {
        this(new PortCreationInfo(description, parent, dataType, customFlags), type);
    }

    public InterfacePort(PortCreationInfo pci, Type type) {
        super(processPci(pci, type));
        initLists(edgesSrc, edgesDest);
        bufPool = (type == Type.Routing) ? null : new MultiTypePortDataBufferPool();
        //deferred = (type == Type.Routing) ? null : new WonderQueueTL<MethodCall>();
        this.type = type;
    }

    /** makes adjustment to flags passed through constructor */
    private static PortCreationInfo processPci(PortCreationInfo pci, Type type) {
        switch (type) {
        case Server:
            pci.flags |= PortFlags.EMITS_DATA | PortFlags.OUTPUT_PORT;
            break;
        case Client:
            pci.flags |= PortFlags.ACCEPTS_DATA;
            break;
        case Network:
        case Routing:
            pci.flags |= PortFlags.EMITS_DATA | PortFlags.ACCEPTS_DATA;
            break;
        }
        return pci;
    }

    @Override
    protected void rawConnectToTarget(AbstractPort target) {
        InterfacePort target2 = (InterfacePort)target;

        // disconnect old port(s) - should always be max. one - however, defensive implementation
        while (target2.edgesDest.size() > 0) { // disconnect old port
            target2.edgesDest.getIterable().get(0).disconnectFrom(target2);
        }

        super.rawConnectToTarget(target);
    }

    @Override
    public void delete() {
        /*Cpp
        if (bufPool != NULL) {
            delete bufPool;
        }
         */
    }

//  /**
//   * (low-level method - only use when you know what you're doing)
//   *
//   * Perform asynchronous method call with parameters in
//   * specified method call buffer.
//   *
//   * MethodCall buffer will be unlocked and recycled by receiver
//   *
//   * @param mc Filled Method call buffer
//   * @return Method call result - may be the same as parameter
//   */
//  protected void asynchMethodCall(MethodCall mc) {
//      mc.setupAsynchCall();
//      mc.pushCaller(this);
//      mc.alreadyDeferred = false;
//      sendMethodCall(mc);
//  }
//
//  /**
//   * Return from synchronous method call
//   *
//   * @param mc Method call data
//   */
//  @NonVirtual protected void returnValue(MethodCall mc) {
//      if (mc.callerStackSize() > 0) {
//
//          // return value to network port
//          mc.returnToCaller();
//
//      } else if (mc.getStatus() == MethodCall.SYNCH_RETURN || mc.getStatus() == MethodCall.CONNECTION_EXCEPTION) {
//
//          SynchMethodCallLogic.handleMethodReturn(mc);
//
//      } else if (mc.getStatus() == MethodCall.ASYNCH_RETURN) {
//
//          handleAsynchReturn(mc, false);
//      }
//  }
//
//  // These methods should typically not be called by subclasses
//
//  /**
//   * Receive method call (synch and asynch) - either forward or handle it
//   *
//   * @param mc Method call
//   */
//  @Inline
//  protected void receiveMethodCall(MethodCall mc) {
//      if (callHandler != null) {
//          handleCall(mc, false);
//      } else {
//          sendMethodCall(mc);
//      }
//  }
//
//  /**
//   * Handle method call (current or deferred)
//   *
//   * @param mc Method call
//   * @param deferredCall Is this a deferred call?
//   * @return Was call deferred?
//   */
//  protected boolean handleCall(MethodCall mc, boolean deferredCall) {
//      mc.autoRecycleTParam1 = true;
//      mc.autoRecycleTParam2 = true;
//      //mc.curServerPort = this;
//      mc.returnValueSet = false;
//      mc.defer = false;
//      mc.call(callHandler, deferredCall);
//      if (!mc.defer) {
//          if (mc.rType == MethodCall.NONE) {
//              mc.recycleComplete();
//              assert(!mc.returnValueSet);
//          } else {
//              mc.recycleParams();
//              assert(mc.returnValueSet);
//              mc.setStatus(mc.getStatus() == MethodCall.SYNCH_CALL ? MethodCall.SYNCH_RETURN : MethodCall.ASYNCH_RETURN);
//              returnValue(mc);
//          }
//      }
//      return mc.defer;
//  }
//
//  /**
//   * Handle method call (current or deferred)
//   *
//   * @param mc Method call
//   * @param deferredCall Deferred return call
//   * @return Was call deferred?
//   */
//  protected boolean handleAsynchReturn(MethodCall mc, boolean deferredCall) {
//      mc.autoRecycleRetVal = true;
//      //mc.curServerPort = this;
//      mc.defer = false;
//      returnHandler.handleMethodReturn(mc, mc.getMethodID(), mc.ri, mc.rd, mc.rt);
//      if (mc.defer) {
//          assert(mc.returnValueSet == false);
//      } else {
//          mc.recycleComplete();
//      }
//      return mc.defer;
//  }
//
//  /**
//   * Send/forward method call (synch and asynch)
//   *
//   * @param mc Method call data
//   */
//  @NonVirtual protected void sendMethodCall(MethodCall mc) {
//      @Ptr ArrayWrapper<InterfacePort> it = edgesDest.getIterable();
//      for (@SizeT int i = 0, n = it.size(); i < n; i++) {
//          InterfacePort ip = (InterfacePort)it.get(i);
//          if (ip != null) {
//              ip.receiveMethodCall(mc);
//              return;
//          }
//      }
//
//      // return NULL if not connected
//      if (mc.getStatus() == MethodCall.SYNCH_CALL) {
//          mc.setStatus(MethodCall.CONNECTION_EXCEPTION);
//          mc.autoRecycleTParam1 = true;
//          mc.autoRecycleTParam2 = true;
//          mc.recycleParams();
//          returnValue(mc);
//      } else {
//          mc.genericRecycle();
//      }
//  }

    /**
     * (for non-cc types only)
     * @param dt Data type of object to get buffer of
     * @return Unused buffer of type
     */
    public PortData getUnusedBuffer(DataType dt) {
        assert(!dt.isCCType());
        assert(bufPool != null);
        return bufPool.getUnusedBuffer(dt);
    }

    /**
     * (for cc types only)
     * @param dt Data type of object to get buffer of
     * @return Unused buffer of type
     */
    public CCInterThreadContainer<?> getUnusedCCBuffer(DataType dt) {
        assert(dt.isCCType());
        return ThreadLocalCache.get().getUnusedInterThreadBuffer(dt);
    }


//  protected void setReturnHandler(ReturnHandler rh) {
//      assert(returnHandler == null);
//      returnHandler = rh;
//  }
//
//  protected void setCallHandler(CallHandler ch) {
//      assert(callHandler == null);
//      callHandler = ch;
//  }

    @Override
    public void notifyDisconnect() { /* don't do anything here... only in network ports */ }

//  @Override
//  public TypedObject universalGetAutoLocked() {
//      System.out.println("warning: cannot get current value from interface port");
//      return null;
//  }

//  @Override
//  public void invokeCall(MethodCall call) {
//      call.pushCaller(this);
//      sendMethodCall(call);
//  }

    @Override
    public void setMaxQueueLength(int length) {
        throw new RuntimeException("InterfacePorts do not have a queue");
    }

    @Override
    protected short getStrategyRequirement() {
        return 0;
    }

    /**
     * @return type of interface port
     */
    public Type getType() {
        return type;
    }

    /**
     * (Usually called on client ports)
     *
     * @return "Server" Port that handles method call - either InterfaceServerPort or InterfaceNetPort (the latter if we have remote server)
     */
    public InterfacePort getServer() {
        @Ptr InterfacePort current = this;
        while (true) {
            @Ptr InterfacePort last = current;
            @Ptr ArrayWrapper<InterfacePort> it = current.edgesDest.getIterable();
            for (@SizeT int i = 0, n = it.size(); i < n; i++) {
                InterfacePort ip = (InterfacePort)it.get(i);
                if (ip != null) {
                    current = ip;
                    break;
                }
            }

            if (current == null || current == last) {
                return null;
            }

            if (current.getType() == Type.Server || current.getType() == Type.Network) {
                return current;
            }
        }
    }

    @Override
    protected void initialPushTo(AbstractPort target, boolean reverse) {
        // do nothing in interface port
    }

    @Override
    protected void clearQueueImpl() {
        // do nothing in interface port
    }

    @Override
    protected int getMaxQueueLengthImpl() {
        // do nothing in interface port
        return 0;
    }

    @Override
    protected void setMaxQueueLengthImpl(int length) {
        // do nothing in interface port
    }
}
