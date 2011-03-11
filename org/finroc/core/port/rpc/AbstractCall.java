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
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.IncludeClass;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.serialization.GenericObject;
import org.finroc.serialization.GenericObjectManager;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.portdatabase.ReusableGenericObjectManager;
import org.finroc.core.portdatabase.SerializableReusable;

/**
 * @author max
 *
 * This is the base abstract class for (possibly synchronous) calls
 * (such as Pull-Calls and method calls)
 */
@IncludeClass( {MethodCallException.class })
@Include("ParameterUtil.h")
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

    /** Storage for parameters that are used in call - for usage in local runtime (fixed size, since this is smaller & less hassle than dynamic array) */
    @InCpp("CallParameter params[MAX_PARAMS];")
    private CallParameter[] params = new CallParameter[MAX_PARAMS];

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"rpc\");")
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

    @Override
    public void serialize(CoreOutput oos) {
        oos.writeByte(status);
        oos.writeByte(syncherID);
        oos.writeInt(threadUid);
        oos.writeShort(methodCallIndex);

        // Serialize parameters
        for (@SizeT int i = 0; i < MAX_PARAMS; i++) {
            params[i].serialize(oos);
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
        for (@SizeT int i = 0; i < MAX_PARAMS; i++) {
            params[i].deserialize(is);
        }
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
        for (@SizeT int i = 0; i < MAX_PARAMS; i++) {
            params[i].recycle();
        }
    }

    @JavaOnly
    public void addParam(int paramIndex, @Const Object o) {
        CallParameter p = params[paramIndex];
        if (o == null) {
            p.type = CallParameter.NULLPARAM;
            p.value = null;
        } else if (o instanceof Number) {
            p.type = CallParameter.NUMBER;
            p.number.setValue((Number)o);
        } else if (o instanceof GenericObject) {
            p.type = CallParameter.OBJECT;
            p.value = (GenericObject)o;
        } else {
            p.type = CallParameter.OBJECT;
            GenericObjectManager mgr = ReusableGenericObjectManager.getManager(o);
            assert(mgr != null && mgr instanceof ReusableGenericObjectManager);
            p.value = mgr.getObject();
        }
    }

    /*Cpp
    template <typename T>
    void getParam(int index, T& pd) {
        ParameterUtil<T>::getParam(&(params[index]), pd);
    }

    template <typename T>
    void addParam(int index, T pd) {
        ParameterUtil<T>::addParam(&(params[index]), pd);
    }
    */

    @InCpp("setExceptionStatus((int8)type);")
    public void setExceptionStatus(MethodCallException.Type type) {
        setExceptionStatus((byte)type.ordinal());
    }

//    @InCppFile
//    /**
//     * @return Unused interthread container of specified type. This method exists, because we cannot include ThreadLocalCache.h in .h-file
//     */
//    public CCPortDataManager<?> getInterThreadBuffer(DataType dt) {
//        return ThreadLocalCache.getFast().getUnusedInterThreadBuffer(dt);
//    }

    @SuppressWarnings("unchecked") @JavaOnly
    /**
     * Get parameter with specified index
     */
    public <P> P getParam(int index) {
        CallParameter p = params[index];
        if (p.type == CallParameter.NULLPARAM) {
            return null;
        } else if (p.type == CallParameter.NUMBER) {
            if (p.number.getNumberType() == CoreNumber.Type.INT) {
                return (P)(Integer)p.number.intValue();
            } else if (p.number.getNumberType() == CoreNumber.Type.LONG) {
                return (P)(Long)p.number.longValue();
            } else if (p.number.getNumberType() == CoreNumber.Type.DOUBLE) {
                return (P)(Double)p.number.doubleValue();
            } else {
                return (P)(Float)p.number.floatValue();
            }
        } else {
            P result = (P)p.value.getData();
            p.clear();
            return result;
        }
    }

    public @SharedPtr GenericObject getParamGeneric(int index) {
        CallParameter p = params[index];
        if (p.type == CallParameter.NULLPARAM || p.value == null) {

            //JavaOnlyBlock
            return null;

            //Cpp return std::shared_ptr<rrlib::serialization::GenericObject>();
        } else {
            @SharedPtr GenericObject go = p.value;
            p.clear();
            return go;
        }
    }


    /**
     * Clear parameters and set method call status to exception
     *
     * @param typeId Type of exception
     */
    public void setExceptionStatus(byte typeId) {
        recycleParameters();
        setStatus(CONNECTION_EXCEPTION);
        params[0].type = CallParameter.NUMBER;
        params[0].value = null;
        params[0].number.setValue(typeId);
    }

}
