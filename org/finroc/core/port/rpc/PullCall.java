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
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.jc.thread.Task;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.net.NetPort;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortData;

/**
 * @author max
 *
 * This class is used for port-pull-requests/calls - locally and over the net.
 *
 * (Caller stack will contain every port in chain - pulled value will be assigned to each of them)
 */
public class PullCall extends AbstractCall implements Task {

    /** Assign pulled value to ports in between? */
    public boolean intermediateAssign;

    /** Is this a pull call for a cc port? */
    public boolean ccPull;

    /** when received through network and executed in separate thread: Port to call pull on and port to send result back over */
    public NetPort port;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"rpc\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("rpc");

    public PullCall() {
        super(/*MAX_CALL_DEPTH*/);
        reset();
    }

    /**
     * Reset all variable in order to reuse object
     */
    private void reset() {
        recycleParameters();
    }

    @Override
    public void deserialize(CoreInput is) {
        super.deserialize(is);
        intermediateAssign = is.readBoolean();
        ccPull = is.readBoolean();
    }

    @Override
    public void serialize(CoreOutput oos) {
        super.serialize(oos);
        oos.writeBoolean(intermediateAssign);
        oos.writeBoolean(ccPull);
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

            if (port.getPort().getDataType().isCCType()) {
                CCPortBase cp = (CCPortBase)port.getPort();
                CCInterThreadContainer<?> cpd = cp.getPullInInterthreadContainerRaw(true);
                recycleParameters();

                //JavaOnlyBlock
                addParamForSending(cpd);

                //Cpp addParamForSending(cpd);

                sendParametersComplete();
                setStatusReturn();
                port.sendCallReturn(this);
            } else if (port.getPort().getDataType().isStdType()) {
                PortBase p = (PortBase)port.getPort();
                @Const PortData pd = p.getPullLockedUnsafe(true);
                recycleParameters();

                //JavaOnlyBlock
                addParamForSending(pd);

                //Cpp addParamForSending(pd);

                sendParametersComplete();
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
}
