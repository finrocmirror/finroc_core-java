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

import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.thread.Task;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.rpc.method.AbstractAsyncReturnHandler;
import org.finroc.core.port.rpc.method.AbstractMethod;
import org.finroc.core.port.rpc.method.AbstractMethodCallHandler;
import org.finroc.core.portdatabase.DataType;

/**
 * @author max
 *
 * This is the class for a complete method call.
 *
 * Such calls can be sent over the network via ports.
 * They can furthermore be asynchronous.
 *
 * Currently a method call may have max. 3 double parameters,
 * 3 long parameters and 2 object parameters. This should be
 * sufficient - since anything can be put into custom objects.
 */
public class MethodCall extends AbstractCall implements Task {

//  /** Maximum size of caller stack */
//  private final static int MAX_CALL_DEPTH = 4;
//
//  //private final TypedObject[] ccBufferRepository;
//
//  /** Parameters */
//  private byte tCount, iCount, dCount; // number of parameters of each type
//  @InCpp("TypedObject* tParams[2];")
//  private final TypedObject[] tParams = new TypedObject[2];
//  @InCpp("int64 iParams[3];")
//  private final long[] iParams = new long[3];
//  @InCpp("double dParams[3];")
//  private final double[] dParams = new double[3];
//
//  /** Type of return value */
//  public static final byte RETURN_OBJ = 1, RETURN_INT = 2, RETURN_DOUBLE = 3;
//  protected byte rType;
//
//  /** Return value - depending on type */
//  protected TypedObject rt; // either PortData or CCInterthreadContainer
//  protected long ri;
//  protected double rd;

//  /** method ID of call */
//  private byte methodID;

//  /** Data type of method */
//  public static DataType METHOD_TYPE;

    // Temporary data for processing and checking requests - no need to transfer over the network
    /** Server port that processes current request */
    //InterfacePort curServerPort;

//  /** Has return value been set by server port? */
//  boolean returnValueSet;
//
//  /** Automatically recycle these objects after call/return? */
//  boolean autoRecycleTParam1, autoRecycleTParam2, autoRecycleRetVal;
//
//  /** Defer method call? */
//  boolean defer;
//
//  /** Has method call been deferred at least once? */
//  boolean alreadyDeferred;
//
//  /** Time when method call arrived - optional */
//  private long arrivalTime;

//  public static void staticInit() {
//      // JavaOnlyBlock
//      METHOD_TYPE = DataTypeRegister.getInstance().addDataType(MethodCall.class);
//
//      //Cpp METHOD_TYPE = DataTypeRegister::getInstance()->addDataType<MethodCall>("MethodCall");
//  }

    /** Method that is called */
    private AbstractMethod method;

    /**
     * Data type of interface that method belong, too
     * (method may belong to multiple - so this is the one
     *  we wan't to actually use)
     */
    private DataType portInterfaceType;

    /** Needed when executed as a task: Handler that will handle this call */
    private @Ptr AbstractMethodCallHandler handler;

    /** Needed when executed as a task and method has return value: Handler that will handle return of this call */
    private AbstractAsyncReturnHandler retHandler;

    /** Needed when executed as a task with synch call over the net - Port over which call is sent */
    private InterfaceNetPort netPort;

    /** Needed when executed as a task with synch call over the net - Network timeout in ms */
    private int netTimeout;

    /** Needed when executed as a task with synch forward over the net - Port from which call originates */
    private InterfaceNetPort sourceNetPort;


    /** (Typically not instantiated directly - possible though) */
    public MethodCall() {
        super(/*MAX_CALL_DEPTH*/);
        //System.out.println("New method call");

        ////Cpp type = METHOD_TYPE;

        //tParams[0] = null;
        //tParams[1] = null;
    }

//  @Override @JavaOnly
//  public DataType getType() {
//      return METHOD_TYPE;
//  }

    /**
     * @return the methodID
     */
    public AbstractMethod getMethod() {
        return method;
    }

    /**
     * @param m The Method that will be called (may not be changed - to avoid ugly programming errors)
     * @param portInterface Data type of interface that method belongs to
     */
    public void setMethod(AbstractMethod m, @Ptr DataType portInterface) {
        method = m;
        portInterfaceType = portInterface;
        assert(typeCheck());
    }

    @Override
    public void serialize(CoreOutput oos) {
        oos.writeByte(method == null ? -1 : method.getMethodId());
        assert(getStatus() != SYNCH_CALL || netTimeout > 0) : "Network timeout needs to be >0 with a synch call";
        oos.writeInt(netTimeout);
        super.serialize(oos);

//      oos.writeBoolean(autoRecycleRetVal);
//      byte mask = 0;
//      switch(status) {
//      case SYNCH_CALL:
//      case ASYNCH_CALL:
//          mask = (byte)((tCount << 6) | (iCount << 4) | (dCount << 2) | rType);
//          oos.writeByte(mask);
//          for (int i = 0; i < tCount; i++) {
//              oos.writeObject(tParams[i]);
//          }
//          for (int i = 0; i < iCount; i++) {
//              oos.writeLong(iParams[i]);
//          }
//          for (int i = 0; i < dCount; i++) {
//              oos.writeDouble(dParams[i]);
//          }
//          break;
//      case ASYNCH_RETURN:
//      case SYNCH_RETURN:
//          oos.writeByte(rType);
//          assert(rType != NONE);
//          // TODO optimize
//          oos.writeLong(ri);
//          oos.writeDouble(rd);
//          oos.writeObject(rt);
//          break;
//      case CONNECTION_EXCEPTION:
//          break;
//      }
    }



    @Override
    public void deserialize(CoreInput is) {
        throw new RuntimeException("Call deserializeCall instead, please!");
    }

    /**
     * (Buffer source for CoreInput should have been set before calling with parameter deserialization enabled)
     *
     * @param is Input Stream
     * @param dt Method Data Type
     * @param skipParameters Skip deserialization of parameter stuff? (for cases when port has been deleted;
     * in this case we need to jump to skip target afterwards)
     */
    public void deserializeCall(CoreInput is, DataType dt, boolean skipParameters) {
        //assert(skipParameters || (dt != null && dt.isMethodType())) : "Method type required here";
        portInterfaceType = dt;
        byte b = is.readByte();
        method = (dt == null) ? null : dt.getPortInterface().getMethod(b);
        netTimeout = is.readInt();
        super.deserializeImpl(is, skipParameters);
//      methodID = is.readByte();
//      autoRecycleRetVal = is.readBoolean();
//      byte mask = 0;
//      switch(status) {
//      case SYNCH_CALL:
//      case ASYNCH_CALL:
//          mask = is.readByte();
//          tCount = (byte)((mask >> 6) & 3);
//          iCount = (byte)((mask >> 4) & 3);
//          dCount = (byte)((mask >> 2) & 3);
//          rType = (byte)(mask & 3);
//          for (int i = 0; i < tCount; i++) {
//              tParams[i] = readObject(is);
//          }
//          for (int i = 0; i < iCount; i++) {
//              iParams[i] = is.readLong();
//          }
//          for (int i = 0; i < dCount; i++) {
//              dParams[i] = is.readLong();
//          }
//          break;
//      case ASYNCH_RETURN:
//      case SYNCH_RETURN:
//          rType = is.readByte();
//          assert(rType != NONE);
//          ri = is.readLong();
//          rd = is.readDouble();
//          rt = readObject(is);
//          break;
//      case CONNECTION_EXCEPTION:
//          break;
//      }
//
//      // reset some values
//      arrivalTime = 0;
//      alreadyDeferred = false;
    }

//  /**
//   * Read object from strem
//   * (helper method for convenience - ensures that object has a lock)
//   *
//   * @param ci Input stream
//   */
//  public TypedObject readObject(CoreInput ci) {
//      TypedObject result = ci.readObjectInInterThreadContainer();
//      if (result != null && result.getType().isStdType()) {
//          ((PortData)result).getManager().getCurrentRefCounter().setLocks((byte)1);
//      }
//      return result;
//  }

//  /**
//   * (for call-handler) don't unlock/recycle parameter 1
//   */
//  public void dontRecycleParam1() {
//      autoRecycleTParam1 = false;
//  }
//
//  /**
//   * (for call-handler) don't unlock/recycle parameter 2
//   */
//  public void dontRecycleParam2() {
//      autoRecycleTParam2 = false;
//  }
//
//  /**
//   * (for return handler) don't recycle return value
//   */
//  public void dontRecycleReturnValue() {
//      autoRecycleRetVal = false;
//  }
//
//  @Inline void deferCall(boolean logArrivalTime) {
//      if (logArrivalTime && (!alreadyDeferred)) {
//          setArrivalTime();
//      }
//      defer = true;
//      alreadyDeferred = true;
//  }
//
//  /**
//   * Recycle/unlock parameters (provided they are not set not to be recycled)
//   */
//  void recycleParams() {
//      if (autoRecycleTParam1) {
//          recycleParam(tParams[0]);
//      }
//      tParams[0] = null;
//      if (autoRecycleTParam2) {
//          recycleParam(tParams[1]);
//      }
//      tParams[1] = null;
//  }
//
//  private void recycleParam(@Ptr TypedObject p) {
//      if (p == null) {
//          return;
//      }
//      if (p.getType().isCCType()) {
//          ((CCInterThreadContainer<?>)p).recycle2();
//      } else {
//          ((PortData)p).getManager().getCurrentRefCounter().releaseLock();
//      }
//  }
//
//  /**
//   * Recycle everything... regardless over whether it has been set not to be recycled
//   * (should only be called by network ports)
//   */
//  @Override
//  public void genericRecycle() {
//      if (isResponsible()) {
//          recycleParam(tParams[0]);
//          recycleParam(tParams[1]);
//          recycleParam(rt);
//          tParams[0] = null;
//          tParams[1] = null;
//          rt = null;
//          super.recycle();
//      }
//  }
//
//  /**
//   * Recycle method call and recycle/unlock any objects (provided they are not set not to be recycled)
//   */
//  void recycleComplete() {
//      recycleParams();
//      if (autoRecycleRetVal) {
//          recycleParam(rt);
//      }
//      rt = null;
//      super.recycle();
//  }

//  @Inline
//  protected void setupCall(byte retValueType, byte methodId2, long int1, long int2, long int3, double dbl1, double dbl2, double dbl3, TypedObject obj1, boolean lockedOrCopied1, TypedObject obj2, boolean lockedOrCopied2) {
//      // parameters
//      methodID = methodId2;
//      iCount = (byte)(int1 == 0 ? 0 : (int2 == 0 ? 1 : (int3 == 0 ? 2 : 3)));
//      dCount = (byte)(dbl1 == 0 ? 0 : (dbl2 == 0 ? 1 : (dbl3 == 0 ? 2 : 3)));
//      tCount = (byte)(obj1 == null ? 0 : (obj2 == null ? 1 : 2));
//      iParams[0] = int1;
//      iParams[1] = int2;
//      iParams[2] = int3;
//      dParams[0] = dbl1;
//      dParams[1] = dbl2;
//      dParams[2] = dbl3;
//      setTParam(0, obj1, lockedOrCopied1);
//      setTParam(1, obj2, lockedOrCopied2);
//
//      // return values
//      ri = 0;
//      rd = 0;
//      rt = null;
//
//      // other stuff
//      rType = retValueType;
//      arrivalTime = 0;
//  }
//
//  private void setTParam(@SizeT int i, @Ptr TypedObject obj, boolean lockedOrCopied) {
//      if (obj != null) {
//          if (!obj.getType().isCCType()) {
//              PortData tmp = (PortData)obj;
//              if (!lockedOrCopied) {
//                  tmp.getCurReference().getRefCounter().setOrAddLock();
//              } else {
//                  assert(tmp.getCurReference().getRefCounter().isLocked());
//              }
//          } else { // cc type
//              @Ptr CCContainerBase cb = (CCContainerBase)obj;
//              if (cb.isInterThreadContainer()) {
//                  @Ptr CCInterThreadContainer<?> ic = (CCInterThreadContainer<?>)cb;
//                  if (!lockedOrCopied) {
//                      @Ptr CCInterThreadContainer<?> citc = ThreadLocalCache.get().getUnusedInterThreadBuffer(obj.getType());
//                      citc.assign(ic.getDataPtr());
//                      obj = citc;
//                  }
//              } else {
//                  @Ptr CCPortDataContainer<?> ic = (CCPortDataContainer<?>)cb;
//                  @Ptr CCInterThreadContainer<?> citc = ThreadLocalCache.get().getUnusedInterThreadBuffer(obj.getType());
//                  citc.assign(ic.getDataPtr());
//                  obj = citc;
//              }
//          }
//      }
//      tParams[i] = obj;
//  }
//
//  /**
//   * @return Integer return value
//   */
//  protected long getReturnInt() {
//      assert(rType == RETURN_INT);
//      return ri;
//  }
//
//  /**
//   * @return Double return value
//   */
//  protected double getReturnDouble() {
//      assert(rType == RETURN_DOUBLE);
//      return rd;
//  }
//
//  /**
//   * @return Object return value (locked)
//   */
//  protected TypedObject getReturnObject() {
//      assert(rType == RETURN_OBJ);
//      return rt;
//  }
//
//  /**
//   * @param r Integer return value
//   */
//  public void setReturn(int r) {
//      assert(rType == RETURN_INT);
//      returnValueSet = true;
//      ri = r;
//  }
//
//  /**
//   * @param r Double return value
//   */
//  public void setReturn(double r) {
//      assert(rType == RETURN_DOUBLE);
//      returnValueSet = true;
//      rd = r;
//  }
//
//  /**
//   * Return null value
//   */
//  public void setReturnNull() {
//      assert(rType == RETURN_OBJ);
//      returnValueSet = true;
//      rt = null;
//  }
//
//  /**
//   * @param r Object return value (locked)
//   * @param locked Has return value already been locked? (so that it can be automatically unlocked by this class)
//   */
//  public void setReturn(PortData obj, boolean locked) {
//      assert(rType == RETURN_OBJ);
//      returnValueSet = true;
//      rt = obj;
//      if (obj == null) {
//          return;
//      }
//      if (!locked) {
//          obj.getCurReference().getRefCounter().setOrAddLock();
//      } else {
//          assert(obj.getCurReference().getRefCounter().isLocked());
//      }
//  }
//
//  /**
//   * @param obj Object return value (will be copied)
//   */
//  public void setReturn(CCPortDataContainer<?> obj) {
//      assert(rType == RETURN_OBJ);
//      if (obj == null) {
//          setReturnNull();
//          return;
//      }
//      CCInterThreadContainer<?> citc = ThreadLocalCache.get().getUnusedInterThreadBuffer(obj.getType());
//      citc.assign(obj.getDataPtr());
//      setReturn(citc, true);
//  }
//
//  /**
//   * @param obj Object return value
//   * @param extraCopy Is this already an extra copy that can be recycled automatically?
//   */
//  public void setReturn(CCInterThreadContainer<?> obj, boolean extraCopy) {
//      assert(rType == RETURN_OBJ);
//      returnValueSet = true;
//      if (obj == null) {
//          rt = null;
//          return;
//      }
//      if (!extraCopy) {
//          CCInterThreadContainer<?> citc = ThreadLocalCache.get().getUnusedInterThreadBuffer(obj.getType());
//          citc.assign(obj.getDataPtr());
//          rt = citc;
//      } else {
//          rt = obj;
//      }
//  }
//
//  /**
//   * Perform method call on specified call handler
//   *
//   * @param callHandler Call Handler
//   * @param deferred Is this a deferred call?
//   */
//  @Inline public void call(CallHandler callHandler, boolean deferred) {
//      callHandler.handleMethodCall(this, methodID, deferred, iParams[0], iParams[1], iParams[2], dParams[0], dParams[1], dParams[2], tParams[0], tParams[1]);
//  }
//
//  /**
//   * @return the arrivalTime
//   */
//  public long getArrivalTime() {
//      return arrivalTime;
//  }
//
//  /**
//   * @param arrivalTime the arrivalTime to set
//   */
//  public void setArrivalTime() {
//      if (arrivalTime == 0) {
//          arrivalTime = Time.getCoarse();
//      }
//  }
//
//  /**
//   * @param arrivalTime the arrivalTime to set
//   */
//  public void setArrivalTime(long time) {
//      arrivalTime = time;
//  }

    public void genericRecycle() {
        recycle();
    }

    public void recycle() {
        method = null;
        handler = null;
        retHandler = null;
        netPort = null;
        netTimeout = -1;
        sourceNetPort = null;
        super.recycle();
    }

    @Override
    public void executeTask() {
        assert(method != null);
        if (sourceNetPort != null) {
            if (netPort != null) { // sync network forward in another thread
                sourceNetPort.executeNetworkForward(this, netPort);
            } else { // sync network call in another thread
                sourceNetPort.executeCallFromNetwork(this, handler);
            }
        } else if (netPort == null) { // async call in another thread
            assert(handler != null);
            method.executeFromMethodCallObject(this, handler, retHandler);
        } else { // sync network call in another thread
            method.executeAsyncNonVoidCallOverTheNet(this, netPort, retHandler, netTimeout);
        }
    }

    /**
     * Prepare method call object for execution in another thread (as a task)
     *
     * @param method Method that is to be called
     * @param portInterface Data type of interface that method belongs to
     * @param handler Handler (server port) that will handle method
     * @param retHandler asynchronous return handler (required for method calls with return value)
     */
    public void prepareExecution(AbstractMethod method, @Ptr DataType portInterface, @Ptr AbstractMethodCallHandler handler, AbstractAsyncReturnHandler retHandler) {
        assert(this.method == null && this.handler == null && method != null);
        this.method = method;
        this.portInterfaceType = portInterface;
        assert(typeCheck());
        this.handler = handler;
        this.retHandler = retHandler;
    }

    /**
     * Prepare method call object for blocking remote execution in another thread (as a task)
     *
     * @param method Method that is to be called
     * @param portInterface Data type of interface that method belongs to
     * @param retHandler asynchronous return handler (required for method calls with return value)
     * @param netPort Port over which call is sent
     * @param netTimeout Network timeout in ms for call
     */
    public void prepareSyncRemoteExecution(AbstractMethod method, @Ptr DataType portInterface, AbstractAsyncReturnHandler retHandler, InterfaceNetPort netPort, int netTimeout) {
        assert(this.method == null && this.handler == null && method != null);
        this.method = method;
        this.portInterfaceType = portInterface;
        assert(typeCheck());
        this.retHandler = retHandler;
        this.netPort = netPort;
        this.netTimeout = netTimeout;
    }

    /**
     * Prepare method call object for blocking remote execution in same thread (as a task)
     *
     * @param method Method that is to be called
     * @param portInterface Data type of interface that method belongs to
     * @param netTimeout Network timeout in ms for call
     */
    public void prepareSyncRemoteExecution(AbstractMethod method, @Ptr DataType portInterface, int netTimeout) {
        assert(this.method == null && this.handler == null && method != null);
        this.method = method;
        this.portInterfaceType = portInterface;
        assert(typeCheck());
        this.netTimeout = netTimeout;
    }

    /**
     * Sanity check for method and portInterfaceType.
     *
     * @return Is everything all right?
     */
    @InCppFile
    private boolean typeCheck() {
        return method != null && portInterfaceType != null && portInterfaceType.getPortInterface() != null && portInterfaceType.getPortInterface().containsMethod(method);
    }

    /**
     * Prepare method call object for blocking remote execution in another thread (as a task)
     * Difference to above: Method call was received from the net and is simply forwarded
     *
     * @param source Port that method call was received from
     * @param dest Port that method call will be forwarded to
     */
    public void prepareForwardSyncRemoteExecution(InterfaceNetPort source, InterfaceNetPort dest) {
        assert(retHandler == null && method != null);
        this.sourceNetPort = source;
        this.netPort = dest;
    }

    /**
     * Prepare method call object for blocking remote execution in another thread (as a task)
     * Difference to above: Method call was received from the net
     *
     * @param interfaceNetPort
     * @param mhandler
     */
    public void prepareExecutionForCallFromNetwork(InterfaceNetPort source, @Ptr AbstractMethodCallHandler mhandler) {
        assert(method != null);
        this.sourceNetPort = source;
        this.handler = mhandler;
        this.netPort = null;
    }

    /**
     * @return Needed when executed as a task with synch call over the net - Network timeout in ms
     */
    public int getNetTimeout() {
        return netTimeout;
    }

    /**
     * @return Data type of interface that method belongs to
     */
    public DataType getPortInterfaceType() {
        return portInterfaceType;
    }

}
