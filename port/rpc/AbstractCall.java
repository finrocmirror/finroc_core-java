/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2012 Max Reichardt,
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

import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.ConstMethod;
import org.rrlib.finroc_core_utils.jc.annotation.Friend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Include;
import org.rrlib.finroc_core_utils.jc.annotation.IncludeClass;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.finroc.core.portdatabase.SerializableReusable;

/**
 * @author max
 *
 * This is the base abstract class for (possibly synchronous) calls
 * (such as pull calls and method calls)
 */
@IncludeClass( {MethodCallException.class })
@Include("ParameterUtil.h")
@Friend( {MethodCallSyncher.class, SynchMethodCallLogic.class})
public abstract class AbstractCall extends SerializableReusable {

    /** Method Syncher index of calling method - in case this is a synchronous method call - otherwise -1 - valid only on calling system */
    private byte syncherID = -1;

    /** Unique Thread ID of calling method */
    private int threadUid = -1;

    /** Status of this method call */
    public enum Status { NONE, SYNCH_CALL, ASYNCH_CALL, SYNCH_RETURN, ASYNCH_RETURN, EXCEPTION }
    private Status status = Status.NONE;
    private MethodCallException.Type exceptionType = MethodCallException.Type.NONE;

    /** Index of method call - used to filter out obsolete returns */
    private short methodCallIndex = -1;

    /** Local port handle - only used while call is enqueued in network queue */
    private int localPortHandle = -1;

    /** Destination port handle - only used while call is enqueued in network queue */
    private int remotePortHandle = -1;

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
    public AbstractCall() {
    }

    @Override
    public void serialize(OutputStreamBuffer oos) {
        oos.writeEnum(status);
        oos.writeEnum(exceptionType);
        System.out.println("Writing " + syncherID + " " + threadUid + " " + methodCallIndex);
        oos.writeByte(syncherID);
        oos.writeInt(threadUid);
        oos.writeShort(methodCallIndex);
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        deserializeImpl(is);
    }

    public void deserializeImpl(@Ref InputStreamBuffer is) {
        status = is.readEnum(Status.class);
        exceptionType = is.readEnum(MethodCallException.Type.class);
        syncherID = is.readByte();
        threadUid = is.readInt();
        methodCallIndex = is.readShort();
        System.out.println("Reading " + syncherID + " " + threadUid + " " + methodCallIndex);
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

    @ConstMethod public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @ConstMethod public int getThreadUid() {
        return threadUid;
    }

    @ConstMethod public @Const @Ref String getStatusString() {
        return status.toString();
    }

    /**
     * (only called by interface port)
     * Prepare synchronous method call
     *
     * @param mcs MethodSyncher object to use
     */
    public void setupSynchCall(@Ptr MethodCallSyncher mcs) {
        status = Status.SYNCH_CALL;
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
        status = Status.ASYNCH_CALL;
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
        assert(status == Status.SYNCH_CALL || status == Status.ASYNCH_CALL);
        status = (status == Status.SYNCH_CALL) ? Status.SYNCH_RETURN : Status.ASYNCH_RETURN;
    }

    /**
     * @return Is call (already) returning?
     */
    @ConstMethod public boolean isReturning(boolean includeException) {
        return status == Status.ASYNCH_RETURN || status == Status.SYNCH_RETURN || (includeException && status == Status.EXCEPTION);
    }

    /**
     * @return Does call cause a connection exception
     */
    @ConstMethod public boolean hasException() {
        return status == Status.EXCEPTION;
    }

    public void genericRecycle() {
        recycle();
    }

    public void recycle() {
        methodCallIndex = -1;
        remotePortHandle = -1;
        status = Status.NONE;
        exceptionType = MethodCallException.Type.NONE;
        syncherID = -1;
        threadUid = -1;
        super.recycle();
    }

    /**
     * Clear parameters and set method call status to exception
     *
     * @param type Type of exception
     */
    public void setExceptionStatus(MethodCallException.Type type) {
        setStatus(Status.EXCEPTION);
        exceptionType = type;
    }

}
