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

import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.jc.thread.Task;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.rtti.GenericObject;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.Serialization;
import org.rrlib.finroc_core_utils.serialization.Serialization.DataEncoding;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.net.NetPort;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.finroc.core.portdatabase.ReusableGenericObjectManager;
import org.finroc.core.portdatabase.SerializationHelper;

/**
 * @author Max Reichardt
 *
 * This class is used for port-pull-requests/calls - locally and over the net.
 *
 * (Caller stack will contain every port in chain - pulled value will be assigned to each of them)
 */
public class PullCall extends AbstractCall implements Task {

    /** Assign pulled value to ports in between? */
    private boolean intermediateAssign;

    /** when received through network and executed in separate thread: Port to call pull on and port to send result back over */
    private NetPort port;

    /** Desired data encoding for port data */
    private Serialization.DataEncoding desiredEncoding = Serialization.DataEncoding.BINARY;

    /** If pull call is returning: Pulled buffer */
    private ReusableGenericObjectManager pulledBuffer;

    /** Log domain for this class */
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("rpc");

    public PullCall() {
    }

    @Override
    public void recycle() {
        intermediateAssign = false;
        port = null;
        desiredEncoding = Serialization.DataEncoding.BINARY;
        if (pulledBuffer != null) {
            pulledBuffer.genericLockRelease();
            pulledBuffer = null;
        }
        super.recycle();
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        super.deserialize(is);
        intermediateAssign = is.readBoolean();
        desiredEncoding = is.readEnum(Serialization.DataEncoding.class);
        if (isReturning(false)) {
            pulledBuffer = (ReusableGenericObjectManager)SerializationHelper.readObject(is, null, this, desiredEncoding).getManager();
            if (pulledBuffer instanceof PortDataManager) {
                ((PortDataManager)pulledBuffer).getCurrentRefCounter().setOrAddLock();
            }
        }
    }

    @Override
    public void serialize(OutputStreamBuffer oos) {
        super.serialize(oos);
        oos.writeBoolean(intermediateAssign);
        oos.writeEnum(desiredEncoding);
        if (isReturning(false)) {
            assert(port != null);
            SerializationHelper.writeObject(oos, port.getDataType(), pulledBuffer.getObject(), desiredEncoding);
        }
    }

    /**
     * Prepare Execution of call received over network in extra thread
     *
     * @param port Port to execute pull on and to return value over later
     */
    public void prepareForExecution(NetPort port) {
        this.port = port;
    }

    @Override
    public void executeTask() {
        assert(port != null);
        synchronized (port.getPort()) {
            if (!port.getPort().isReady()) {
                log(LogLevel.LL_DEBUG, logDomain, "pull call received for port that will soon be deleted");
                recycle();
            }
            assert(pulledBuffer == null);

            if (FinrocTypeInfo.isCCType(port.getPort().getDataType())) {
                CCPortBase cp = (CCPortBase)port.getPort();
                pulledBuffer = cp.getPullInInterthreadContainerRaw(true, true);
                setStatusReturn();
                port.sendCallReturn(this);
            } else if (FinrocTypeInfo.isStdType(port.getPort().getDataType())) {
                PortBase p = (PortBase)port.getPort();
                pulledBuffer = p.getPullLockedUnsafe(true, true);
                setStatusReturn();
                port.sendCallReturn(this);
            } else {
                log(LogLevel.LL_WARNING, logDomain, "pull call received for port with invalid data type");
                recycle();
            }
        }
    }

    public String toString() {
        return "PullCall (" + getStatusString() + ", callid: " + super.getMethodCallIndex() + ", threaduid: " + super.getThreadUid() + ")";
    }

    /**
     * @return If pull call is returning: Pulled buffer
     */
    public GenericObject getPulledBuffer() {
        return pulledBuffer.getObject();
    }

    /**
     * Setup pull call for remote execution
     *
     * @param remoteHandle Destination port handle - only used while call is enqueued in network queue
     * @param intermediateAssign Assign pulled value to ports in between?
     * @param encoding Data encoding to use for serialization of pulled buffer
     */
    public void setupPullCall(int remoteHandle, boolean intermediateAssign, DataEncoding encoding) {
        setRemotePortHandle(remoteHandle);
        this.intermediateAssign = intermediateAssign;
        this.desiredEncoding = encoding;
    }
}
