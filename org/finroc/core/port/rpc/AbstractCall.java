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

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.container.SimpleList;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.jc.stream.MemoryBuffer;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.port.std.PortData;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.core.portdatabase.SerializableReusable;

/**
 * @author max
 *
 * This is the base abstract class for (possibly synchronous) calls
 * (such as Pull-Calls and method calls)
 */
@ForwardDecl(MethodCallSyncher.class)
@CppInclude( {"MethodCallSyncher.h"})
@Include( {"MethodCallException.h", "portdatabase/DataTypeRegister.h", "ParameterUtil.h"})
@Friend( {MethodCallSyncher.class, SynchMethodCallLogic.class})
public abstract class AbstractCall extends SerializableReusable {

    /** Method Syncher index of calling method - in case this is a synchronous method call - otherwise -1 - valid only on calling system */
    private byte syncherID = -1;

    /** Unique Thread ID of calling method */
    private int threadUid = 0;

    /** Status of this method call */
    public static final byte NONE = 0, SYNCH_CALL = 1, ASYNCH_CALL = 2, SYNCH_RETURN = 3, ASYNCH_RETURN = 4, CONNECTION_EXCEPTION = 5;
    private static final String[] STATUS_STRINGS = new String[] {"NONE", "SYNCH_CALL", "ASYNCH_CALL", "SYNCH_RETURN", "ASYNCH_RETURN", "CONNECTION_EXCEPTION"};
    protected byte status = NONE;

//  /** Caller stack - contains port handle to which return value will be forwarded - only relevant for network connections */
//  private final CallStack callerStack;

    /** Index of method call - used to filter out obsolete returns */
    private short methodCallIndex;

    /** Local port handle - only used while call is enqueued in network queue */
    private int localPortHandle;

    /** Destination port handle - only used while call is enqueued in network queue */
    private int remotePortHandle;

    /** Maximum number of parameters */
    private static final @SizeT int MAX_PARAMS = 4;

    /** Storage buffer for any (serialized) parameters or return values - for sending over the network */
    private @PassByValue MemoryBuffer paramStorage = new MemoryBuffer(150, 1);

    /** Storage for parameters that are used in call - for usage in local runtime (fixed size, since this is smaller & less hassle than dynamic array) */
    @InCpp("CallParameter params[MAX_PARAMS];")
    private CallParameter[] params = new CallParameter[MAX_PARAMS];

    /** To write to paramStorage... */
    @PassByValue private CoreOutput os = new CoreOutput(paramStorage);

    /** To read from paramStorage... */
    @PassByValue private CoreInput is = new CoreInput(paramStorage);

    /** Any (usually big) buffers that call is currently in charge of recycling */
    @CppType("util::SimpleList<const PortData*>")
    private SimpleList<PortData> responsibilities = new SimpleList<PortData>();

    /** For incoming commands: Is it possible to deserialize parameters in paramStorage? */
    private boolean deserializableParameters = false;

    /** Log domain for this class */
    @InCpp("_CREATE_NAMED_LOGGING_DOMAIN(logDomain, \"rpc\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("rpc");

    /**
     * @return Destination port handle - only used while call is enqueued in network queue
     */
    @ConstMethod public int getRemotePortHandle() {
        return remotePortHandle;
    }

    /**
     * @param remotePortHandle Destination port handle - only used while call is enqueued in network queue
     */
    public void setRemotePortHandle(int remotePortHandle) {
        this.remotePortHandle = remotePortHandle;
    }

    /**
     * @return Local port handle - only used while call is enqueued in network queue
     */
    @ConstMethod public int getLocalPortHandle() {
        return localPortHandle;
    }

    /**
     * @param localPortHandle Local port handle - only used while call is enqueued in network queue
     */
    public void setLocalPortHandle(int localPortHandle) {
        this.localPortHandle = localPortHandle;
    }

    /**
     * @param maxCallDepth Maximum size of call stack
     */
    public AbstractCall(/*int maxParameters*/) {
        //callerStack = new CallStack(maxCallDepth);

        //JavaOnlyBlock
        for (int i = 0; i < MAX_PARAMS; i++) {
            params[i] = new CallParameter();
        }
    }

//  /**
//   * (Should only be called by network port)
//   * Add caller to top of caller stack
//   *
//   * @param callerHandle caller handle
//   */
//  public void pushCaller(int callerHandle) {
//      callerStack.add(callerHandle);
//  }
//
//  /**
//   * (Should only be called by network port)
//   * Add caller to top of caller stack
//   *
//   * @param caller Caller
//   */
//  public void pushCaller(FrameworkElement caller) {
//      callerStack.add(caller.getHandle());
//  }
//
//  /**
//   * (Should only be called by network port)
//   * @return return current size of caller stack
//   */
//  @ConstMethod public @SizeT int callerStackSize() {
//      return callerStack.size();
//  }
//
//  /**
//   * (Should only be called by network port)
//   * Remove and return caller handle at top of stack
//   *
//   * @return Caller Handle
//   */
//  public int popCaller() {
//      return callerStack.removeLast();
//  }
//
//  /**
//   * Remove and return caller at top of stack
//   *
//   * @return Caller
//   */
//  public FrameworkElement popCallerElement() {
//      return RuntimeEnvironment.getInstance().getElement(popCaller());
//  }
//
//  /**
//   * Pop caller from stack and call its handleCallReturn method
//   */
//  public void returnToCaller() {
//      FrameworkElement fe = popCallerElement();
//      if (fe != null) {
//          fe.handleCallReturn(this);
//      } else {
//          //Cpp printf("%p ", this);
//          System.out.println("warning: lost method call, because caller seems to have been deleted... caller will hopefully timeout(?)");
//          //recycle();
//      }
//  }

    @Override
    public void serialize(CoreOutput oos) {
        oos.writeByte(status);
        oos.writeByte(syncherID);
        oos.writeInt(threadUid);
        oos.writeShort(methodCallIndex);

        // Serialize parameters
        paramStorage.serialize(oos);
        oos.writeByte(responsibilities.size());
        for (@SizeT int i = 0; i < responsibilities.size(); i++) {
            oos.writeObject(responsibilities.get(i));
        }
    }

    @Override
    public void deserialize(CoreInput is) {
        deserializeImpl(is, false);
    }

    public void deserializeImpl(CoreInput is, boolean skipParameters) {
        status = is.readByte();
        syncherID = is.readByte();
        threadUid = is.readInt();
        methodCallIndex = is.readShort();

        // deserialize parameters
        if (skipParameters) {
            return;
        }
        paramStorage.deserialize(is);
        int respSize = is.readByte();
        assert(responsibilities.size() == 0);
        for (int i = 0; i < respSize; i++) {
            PortData p = (PortData)is.readObject();
            p.getManager().getCurrentRefCounter().setLocks((byte)1); // one lock for us
            responsibilities.add(p);
        }

        deserializableParameters = true;
    }

    /**
     * @return Method Syncher index of calling method
     */
    @ConstMethod protected int getSyncherID() {
        return syncherID;
    }

    /**
     * @param syncherID Method Syncher index of calling method
     */
    protected void setSyncherID(int syncherID) {
        assert(syncherID <= 127);
        this.syncherID = (byte)syncherID;
    }

    @ConstMethod protected short getMethodCallIndex() {
        return methodCallIndex;
    }

    protected void setMethodCallIndex(short methodCallIndex) {
        this.methodCallIndex = methodCallIndex;
    }

    @ConstMethod public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    @ConstMethod public int getThreadUid() {
        return threadUid;
    }

    @ConstMethod public @Const @Ref String getStatusString() {
        return STATUS_STRINGS[status];
    }

    /**
     * (only called by interface port)
     * Prepare synchronous method call
     *
     * @param mcs MethodSyncher object to use
     */
    public void setupSynchCall(@Ptr MethodCallSyncher mcs) {
        status = SYNCH_CALL;
        threadUid = mcs.getThreadUid();
        setSyncherID(mcs.getIndex());
        setMethodCallIndex(mcs.getAndUseNextCallIndex());
        //callerStack.setSize(0);
    }

    /**
     * (only called by interface port)
     * Prepare asynchronous method call
     */
    public void setupAsynchCall() {
        status = ASYNCH_CALL;
        threadUid = -1;
        setSyncherID(-1);
        //callerStack.setSize(0);
    }

    /**
     * Set status to RETURNING.
     *
     * Depending on whether we have a synch or asynch call is will be SYNCH_RETURN or ASYNCH_RETURN
     */
    public void setStatusReturn() {
        assert(status == SYNCH_CALL || status == ASYNCH_CALL);
        status = (status == SYNCH_CALL) ? SYNCH_RETURN : ASYNCH_RETURN;
    }

    /**
     * @return Is call (already) returning?
     */
    @ConstMethod public boolean isReturning(boolean includeException) {
        return status == ASYNCH_RETURN || status == SYNCH_RETURN || (includeException && status == CONNECTION_EXCEPTION);
    }

    /**
     * @return Does call cause a connection exception
     */
    @ConstMethod public boolean hasException() {
        return status == CONNECTION_EXCEPTION;
    }

    public void genericRecycle() {
        recycle();
    }

    public void recycle() {
        recycleParameters();
        methodCallIndex = -1;
        remotePortHandle = -1;
        status = NONE;
        syncherID = -1;
        threadUid = -1;
        super.recycle();
    }

    /**
     * Recycle all parameters, but keep empty method call
     */
    public void recycleParameters() {
        paramStorage.clear();
        os.reset(paramStorage);
        for (@SizeT int i = 0; i < responsibilities.size(); i++) {
            responsibilities.get(i).getManager().releaseLock();
        }
        responsibilities.clear();
        for (@SizeT int i = 0; i < MAX_PARAMS; i++) {
            params[i].recycle();
        }
        deserializableParameters = false;
    }

//  /** Call stack of an abstract call */
//  @AtFront @Superclass2(util::IntArrayWrapper")
//  private class CallStack extends IntArrayWrapper {
//
//      /** Maximum size of call stack */
//      public CallStack(int size) {
//          super(size);
//      }
//  }

    /**
     * When a method call is transferred over the net,
     * this method should be used to add parameters.
     * (Most of them are directly serialized)
     */
    @JavaOnly
    public void addParamForSending(@Const Object o) {
        if (o == null) {
            os.writeByte(CallParameter.NULLPARAM);
        } else if (o instanceof Number) {
            Number n = (Number)o;
            if (o instanceof Integer) {
                os.writeByte(CallParameter.INT);
                os.writeInt(n.intValue());
            } else if (o instanceof Long) {
                os.writeByte(CallParameter.LONG);
                os.writeLong(n.longValue());
            } else if (o instanceof Float) {
                os.writeByte(CallParameter.FLOAT);
                os.writeFloat(n.floatValue());
            } else if (o instanceof Double) {
                os.writeByte(CallParameter.LONG);
                os.writeDouble(n.doubleValue());
            } else if (o instanceof Byte) {
                os.writeByte(CallParameter.BYTE);
                os.writeByte(n.byteValue());
            } else if (o instanceof Short) {
                os.writeByte(CallParameter.SHORT);
                os.writeShort(n.shortValue());
            }
        } else if (o instanceof CCInterThreadContainer<?>) {
            CCInterThreadContainer<?> c = (CCInterThreadContainer<?>)o;
            os.writeByte(CallParameter.CCCONTAINER);
            os.writeObject(c);
            c.recycle2();
        } else if (o instanceof CCPortData) {
            os.writeByte(CallParameter.CCDATA);
            DataType dt = DataTypeRegister.getInstance().getDataType(o.getClass());
            assert(dt != null);
            os.writeShort(dt.getUid());
            ((CCPortData)o).serialize(os);
        } else if (o instanceof PortData) {
            os.writeByte(CallParameter.PORTDATA);
            responsibilities.add((PortData)o);
        } else {
            throw new RuntimeException("Unsupported data type");
        }
    }

    @JavaOnly
    public void addParamForLocalCall(int paramIndex, @Const Object o) {
        CallParameter p = params[paramIndex];
        p.value = o;
        if (o == null) {
            p.type = CallParameter.NULLPARAM;
        } else if (o instanceof Number) {
            if (o instanceof Integer) {
                p.type = CallParameter.INT;
            } else if (o instanceof Long) {
                p.type = CallParameter.LONG;
            } else if (o instanceof Float) {
                p.type = CallParameter.FLOAT;
            } else if (o instanceof Double) {
                p.type = CallParameter.LONG;
            } else if (o instanceof Byte) {
                p.type = CallParameter.BYTE;
            } else if (o instanceof Short) {
                p.type = CallParameter.SHORT;
            }
        } else if (o instanceof CCInterThreadContainer<?>) {
            p.type = CallParameter.CCCONTAINER;
        } else if (o instanceof CCPortData) {
            DataType dt = DataTypeRegister.getInstance().getDataType(o.getClass());
            CCInterThreadContainer<?> cc = ThreadLocalCache.getFast().getUnusedInterThreadBuffer(dt);
            cc.assign((CCPortData)o);
            p.type = CallParameter.CCDATA;
            p.value = cc;
        } else if (o instanceof PortData) {
            p.type = CallParameter.PORTDATA;
        } else {
            throw new RuntimeException("Unsupported data type");
        }
    }

    /*Cpp
    template <typename T>
    void addParamForSending(T t) {
        ParameterUtil<T>::addParamForSending(responsibilities, os, t);
    }

    template <typename T>
    void getParam(int index, T& pd) {
        ParameterUtil<T>::getParam(&(params[index]), pd);
    }

    template <typename T>
    void addParamForLocalCall(int index, T pd) {
        ParameterUtil<T>::addParamForLocalCall(&(params[index]), pd);
    }

    */

//  /*Cpp
//  void addParamForSending(const PortData* pd) {
//      if (pd == NULL) {
//          os.writeByte(NULLPARAM);
//      }
//      os.writeByte(PORTDATA);
//      responsibilities.add(pd);
//  }
//
//  void addParamForSending(const int pd) {
//      os.writeByte(INT);
//      os.writeInt(pd);
//  }
//
//  void addParamForSending(const int64 pd) {
//      os.writeByte(LONG);
//      os.writeLong(pd);
//  }
//
//  void addParamForSending(const float pd) {
//      os.writeByte(FLOAT);
//      os.writeFloat(pd);
//  }
//
//  void addParamForSending(const double pd) {
//      os.writeByte(DOUBLE);
//      os.writeDouble(pd);
//  }
//
//  void addParamForSending(const int8 pd) {
//      os.writeByte(BYTE);
//      os.writeByte(pd);
//  }
//
//  void addParamForSending(const short pd) {
//      os.writeByte(SHORT);
//      os.writeShort(pd);
//  }
//
//
//  template <typename T>
//  void addParamForSending(CCInterThreadContainer<T>* container) {
//      if (container == NULL) {
//          os.writeByte(NULLPARAM);
//      }
//      //assert(container->isInterThreadContainer() && "only interthread container allowed in method calls");
//      os.writeByte(CCCONTAINER);
//      os.writeObject(container);
//      container->recycle2();
//  }
//
//  template <typename T>
//  void addParamForSending(const T* c) {
//      if (c == NULL) {
//          os.writeByte(NULLPARAM);
//      }
//      DataType* dt = DataTypeRegister::getInstance()->getDataType<T>();
//      assert(dt != NULL && dt->isCCType());
//      os.writeByte(CCDATA);
//      os.writeShort(dt->getUid());
//      writeParam(c, dt);
//  }
//
//  template <typename T>
//  void addParamForSending(T c) {
//      addParamForSending((const T)c);
//  }
//
//  void writeParam(const CoreSerializable* cs, DataType* dt) {
//      cs->serialize(os);
//  }
//
//  void writeParam(void* cs, DataType* dt) {
//      dt->directSerialize(cs, &os);
//  }
//
//  void addParamForLocalCall(int paramIndex, const PortData* pd) {
//      Parameter* p = &(params[paramIndex]);
//      if (pd == NULL) {
//          p->type = NULLPARAM;
//      }
//      p->type = PORTDATA;
//      p->value = pd;
//  }
//
//  void addParamForLocalCall(int paramIndex, const int pd) {
//      Parameter* p = &(params[paramIndex]);
//      p->type = INT;
//      p->ival = pd;
//  }
//
//  void addParamForLocalCall(int paramIndex, const int64 pd) {
//      Parameter* p = &(params[paramIndex]);
//      p->type = LONG;
//      p->lval = pd;
//  }
//
//  void addParamForLocalCall(int paramIndex, const float pd) {
//      Parameter* p = &(params[paramIndex]);
//      p->type = FLOAT;
//      p->fval = pd;
//  }
//
//  void addParamForLocalCall(int paramIndex, const double pd) {
//      Parameter* p = &(params[paramIndex]);
//      p->type = DOUBLE;
//      p->dval = pd;
//  }
//
//  void addParamForLocalCall(int paramIndex, const int8 pd) {
//      Parameter* p = &(params[paramIndex]);
//      p->type = BYTE;
//      p->bval = pd;
//  }
//
//  void addParamForLocalCall(int paramIndex, const int16 pd) {
//      Parameter* p = &(params[paramIndex]);
//      p->type = SHORT;
//      p->sval = pd;
//  }
//
//  template <typename T>
//  void addParamForLocalCall(int paramIndex, CCInterThreadContainer<T>* container) {
//      Parameter* p = &(params[paramIndex]);
//      if (container == NULL) {
//          p->type = NULLPARAM;
//      }
//      //assert(container->isInterThreadContainer() && "only interthread container allowed in method calls");
//      p->ccval = container;
//  }
//
//  template <typename T>
//  void addParamForLocalCall(int paramIndex, const T* c) {
//      Parameter* p = &(params[paramIndex]);
//      if (c == NULL) {
//          p->type = NULLPARAM;
//      }
//      DataType* dt = DataTypeRegister::getInstance()->getDataType<T>();
//      assert(dt != NULL && dt->isCCType());
//      CCInterThreadContainer<T>* cc = (CCInterThreadContainer<T>*)_M_getInterThreadBuffer(dt);
//      cc->assign(c);
//      p->type = CCDATA;
//      p->ccval = cc;
//  }
//
//  template <typename T>
//  void addParamForLocalCall(int paramIndex, T c) {
//      addParamForLocalCall(paramIndex, (const T)c);
//  }
//   */

    /**
     * Prepare method call received from the net for local call.
     *
     * Deserializes parameters from storage to param objects
     */
    public void deserializeParamaters() {
        if (!deserializableParameters) {
            logDomain.log(LogLevel.LL_DEBUG_WARNING, getLogDescription(), "warning: double deserialization of parameters");
            return;
        }
        deserializableParameters = false;
        is.reset();
        int curParam = 0;
        int curResp = 0;
        CCInterThreadContainer<?> container = null;
        while (is.moreDataAvailable()) {
            @Ptr CallParameter p = params[curParam];
            p.type = is.readByte();
            switch (p.type) {
            case CallParameter.NULLPARAM:
                p.value = null;
                break;
            case CallParameter.INT:
                //JavaOnlyBlock
                p.value = is.readInt();

                //Cpp p->ival = is.readInt();
                break;
            case CallParameter.LONG:
                //JavaOnlyBlock
                p.value = is.readLong();

                //Cpp p->lval = is.readLong();
                break;
            case CallParameter.FLOAT:
                //JavaOnlyBlock
                p.value = is.readFloat();

                //Cpp p->fval = is.readFloat();
                break;
            case CallParameter.DOUBLE:
                //JavaOnlyBlock
                p.value = is.readDouble();

                //Cpp p->dval = is.readDouble();
                break;
            case CallParameter.BYTE:
                //JavaOnlyBlock
                p.value = is.readByte();

                //Cpp p->bval = is.readByte();
                break;
            case CallParameter.SHORT:
                //JavaOnlyBlock
                p.value = is.readShort();

                //Cpp p->sval = is.readShort();
                break;
            case CallParameter.CCDATA:
            case CallParameter.CCCONTAINER:
                container = (CCInterThreadContainer<?>)is.readObjectInInterThreadContainer();

                //JavaOnlyBlock
                p.value = container;

                //Cpp p->ccval = container;
                break;
            case CallParameter.PORTDATA:
                p.value = responsibilities.get(curResp);

                //JavaOnlyBlock
                ((PortData)p.value).getManager().addLock();

                //Cpp p->value->getManager()->addLock();

                curResp++;
                break;
            }

            curParam++;
        }
    }

    @InCppFile
    /**
     * @return Unused interthread container of specified type. This method exists, because we cannot include ThreadLocalCache.h in .h-file
     */
    public CCInterThreadContainer<?> getInterThreadBuffer(DataType dt) {
        return ThreadLocalCache.getFast().getUnusedInterThreadBuffer(dt);
    }

    @SuppressWarnings("unchecked") @JavaOnly
    /**
     * Get parameter with specified index
     */
    public <P> P getParam(int index) {
        assert(!deserializableParameters);
        CallParameter p = params[index];
        if (p.type == CallParameter.CCDATA) {
            return (P)((CCInterThreadContainer<?>)p.value).getDataPtr();
        } else {
            P result = (P)p.value;
            p.clear();
            return result;
        }
    }

//  /*Cpp
//  void getParam(int index, const PortData*& pd) {
//      Parameter* p = &(params[index]);
//      pd = p->value;
//      p->clear();
//  }
//
//  void getParam(int index, PortData*& pd) {
//      Parameter* p = &(params[index]);
//      pd = const_cast<PortData*>(p->value); // not entirely clean... but everything else seems a lot of work (what's const and what isn't should be defined in methods)
//      p->clear();
//  }
//
//  void getParam(int index, int& pd) {
//      Parameter* p = &(params[index]);
//      pd = p->ival;
//      p->clear();
//  }
//
//  void getParam(int index, int64& pd) {
//      Parameter* p = &(params[index]);
//      pd = p->lval;
//      p->clear();
//  }
//
//  void getParam(int index, float& pd) {
//      Parameter* p = &(params[index]);
//      pd = p->fval;
//      p->clear();
//  }
//
//  void getParam(int index, double& pd) {
//      Parameter* p = &(params[index]);
//      pd = p->dval;
//      p->clear();
//  }
//
//  void getParam(int index, int8& pd) {
//      Parameter* p = &(params[index]);
//      pd = p->bval;
//      p->clear();
//  }
//
//  void getParam(int index, int16& pd) {
//      Parameter* p = &(params[index]);
//      pd = p->sval;
//      p->clear();
//  }
//
//  template <typename T>
//  void getParam(int index, CCInterThreadContainer<T>*& container) {
//      Parameter* p = &(params[index]);
//      container = p->ccval;
//      p->clear();
//  }
//
//  template <typename T>
//  void getParam(int index, T*& c) {
//      Parameter* p = &(params[index]);
//      c = p->ccval->getDataPtr();
//      p->clear();
//  }
//  */

    /**
     * Clear parameters and set method call status to exception
     *
     * @param typeId Type of exception
     */
    public void setExceptionStatus(byte typeId) {
        recycleParameters();
        setStatus(CONNECTION_EXCEPTION);
        addParamForSending(typeId);
        params[0].type = CallParameter.BYTE;

        //JavaOnlyBlock
        params[0].value = typeId;

        //Cpp params[0].bval = typeId;
        sendParametersComplete();
    }

    @InCpp("setExceptionStatus((int8)type);")
    public void setExceptionStatus(MethodCallException.Type type) {
        setExceptionStatus((byte)type.ordinal());
    }

    /**
     * When sending parameters over the network: Call this when all parameters have been added
     */
    public void sendParametersComplete() {
        os.close();
    }
}
