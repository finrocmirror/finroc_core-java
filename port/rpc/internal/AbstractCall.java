/**
 * You received this file as part of Finroc
 * A Framework for intelligent robot control
 *
 * Copyright (C) Finroc GbR (finroc.org)
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
package org.finroc.core.port.rpc.internal;

import java.util.concurrent.atomic.AtomicInteger;

import org.finroc.core.port.rpc.FutureStatus;
import org.finroc.core.port.rpc.Method;
import org.finroc.core.port.rpc.ResponseHandler;
import org.rrlib.finroc_core_utils.jc.container.Queueable;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;


/**
 * @author Max Reichardt
 *
 * This is the base class for all "calls" (requests, responses, messages)
 * For calls within the same runtime environment they are not required.
 * They are used to temporarily store such calls in queues for network threads
 * and to serialize calls.
 */
public class AbstractCall extends Queueable {

    public enum CallType {
        RPC_MESSAGE,
        RPC_REQUEST,
        RPC_RESPONSE,
        UNSPECIFIED
    }

    /** Log domain for this class */
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("rpc_ports");

    /** True while thread is waiting on condition variable */
    public boolean waiting;

    /** Status for future */
    public final AtomicInteger futureStatus = new AtomicInteger();

    /**
     * If not NULL, signals that call is complete now and can be sent
     * (it is possible to enqueue incomplete calls in network send queue)
     * (points to tFutureStatus atomic)
     */
    public AtomicInteger callReadyForSending;

    /** Pointer to (optional) response handler */
    public ResponseHandler responseHandler;

    /**
     * Does this contain a call that expects a response?
     * If yes, contains a timeout for this response in ms (otherwise -1)
     */
    long responseTimeout = -1;

    /** Identification of call in this process */
    long callId;

    /** Type of call */
    public AbstractCall.CallType callType;

    /** Handle of local port that call was sent from. Set automatically by classes in RPC plugin. */
    int localPortHandle;

    /** Handle of remote port that call is meant for: Custom variable for network transport implementation */
    int remotePortHandle;

    /** Method that this call originates from */
    public Method method;

    /** Buffer with result */
    public Object resultBuffer;

    /**
     * @return Does this contain a call that expects a response?
     */
    public boolean expectsResponse() {
        return responseTimeout != 0;
    }

    /**
     * @return Identification of call in this process
     */
    public long getCallId() {
        return callId;
    }

    /**
     * @return Type of call
     */
    public CallType getCallType() {
        return callType;
    }

    /**
     * @return Handle of local port that call was sent from.
     */
    public int getLocalPortHandle() {
        return localPortHandle;
    }

    /**
     * @return Handle of remote port that call is meant for: Custom variable for network transport implementation
     */
    public int getRemotePortHandle() {
        return remotePortHandle;
    }

    /**
     * @return If call expects a response, contains timeout for this response
     */
    public long getResponseTimeout() {
        return responseTimeout;
    }

    /**
     * @return Pointer to use inside tFuture
     * (ensures that access is safe as long as this pointer exists)
     * (probably not necessary in Java)
     */
    public AbstractCall obtainFuturePointer() {
        return this;
    }

    /**
     * @return Is call ready for sending?
     * (it is possible to enqueue calls that are not ready for sending yet in network send queues)
     */
    public boolean readyForSending() {
        return (callReadyForSending == null) || (callReadyForSending.get() != FutureStatus.PENDING.ordinal());
    }

    /**
     * Serializes call to stream
     */
    public void serialize(OutputStreamBuffer stream) {}

    /**
     * @param callId Call ID for call
     */
    public void setCallId(long callId) {
        this.callId = callId;
    }

    /**
     * Indicates and notifies any futures/response handlers that RPC call
     * caused an exception
     *
     * @param method Method that exception originates from
     * @param newStatus Type of exception
     */
    public void setException(FutureStatus newStatus) {
        FutureStatus current = FutureStatus.values()[futureStatus.get()];
        if (current != FutureStatus.PENDING) {
            log(LogLevel.LL_WARNING, logDomain, "Exception cannot be set twice. Ignoring.");
            return;
        }

        if (newStatus == FutureStatus.PENDING || newStatus == FutureStatus.READY) {
            throw new RuntimeException("Invalid value for exception");
        }

        synchronized (this) {
            futureStatus.set(newStatus.ordinal());
            this.notify();
        }
        if (responseHandler != null) {
            responseHandler.handleException(method, newStatus);
        }
    }

    /**
     * @param remotePortHandle Handle of remote port that call is meant for: Custom variable for network transport implementation
     */
    public void setRemotePortHandle(int remotePortHandle) {
        this.remotePortHandle = remotePortHandle;
    }
}
