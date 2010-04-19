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

import org.finroc.jc.annotation.Ptr;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.rpc.method.AbstractMethod;
import org.finroc.core.port.rpc.method.AbstractMethodCallHandler;

/**
 * @author max
 *
 * Interface ports that will forward calls over the net
 */
public abstract class InterfaceNetPort extends InterfacePort {

    public InterfaceNetPort(PortCreationInfo pci) {
        super(pci, Type.Network);
    }

    /**
     * Send asynchronous call over network connection
     *
     * @param mc Call to send (recycles call including parameters afterwards)
     */
    public abstract void sendAsyncCall(MethodCall mc);

    /**
     * Send synchronous call return over network connection
     * (usually pretty similar to above method)
     *
     * @param mc Call to send (recycles call including parameters afterwards)
     */
    public abstract void sendSyncCallReturn(MethodCall mc);

    /**
     * Send synchronous call over network connection
     * Thread blocks until result is available or call timed out
     *
     * @param mc Method Call to process
     * @param timeout Network timeout for call (should be larger than any timeout in call due to network delays)
     * @return Method Call object containing results (may be a different one than mc - transfers responsibility for it to caller)
     */
    public abstract MethodCall synchCallOverTheNet(MethodCall mc, int timeout) throws MethodCallException;

    /**
     * Process method call that was received from a network connection
     *
     * @param mc Received method call (takes responsibility of recycling it)
     * @return MethodCall containing result
     */
    public void processCallFromNet(MethodCall mc) {

        InterfacePort ip = getServer();
        AbstractMethod m = mc.getMethod();
        if (ip != null && ip.getType() == InterfacePort.Type.Network) {
            InterfaceNetPort inp = (InterfaceNetPort)ip;
            if (m.isVoidMethod()) {
                inp.sendAsyncCall(mc);
            } else {
                mc.prepareForwardSyncRemoteExecution(this, inp); // always do this in extra thread
                RPCThreadPool.getInstance().executeTask(mc);
                sendSyncCallReturn(mc);
            }
        } else if (ip != null && ip.getType() == InterfacePort.Type.Server) {
            @Ptr AbstractMethodCallHandler mhandler = (AbstractMethodCallHandler)((InterfaceServerPort)ip).getHandler();
            if (mhandler == null) {
                if (m.isVoidMethod()) {
                    mc.recycle();
                } else {
                    mc.setExceptionStatus(MethodCallException.Type.NO_CONNECTION);
                    sendSyncCallReturn(mc);
                }
            } else {
                if (mc.getMethod().handleInExtraThread()) {
                    mc.prepareExecutionForCallFromNetwork(this, mhandler);
                    RPCThreadPool.getInstance().executeTask(mc);
                    return;
                } else {
                    mc.deserializeParamaters();
                    mc.getMethod().executeFromMethodCallObject(mc, mhandler, null);
                    if (!m.isVoidMethod()) {
                        sendSyncCallReturn(mc);
                    }
                }
            }
        } else {
            if (m.isVoidMethod()) {
                mc.recycle();
            } else {
                mc.setExceptionStatus(MethodCallException.Type.NO_CONNECTION);
                sendSyncCallReturn(mc);
            }
        }
    }

    /**
     * Called in extra thread for network forwarding of a call
     *
     * @param mc Call to forward
     * @param netPort Port to forward call over
     */
    public void executeNetworkForward(MethodCall mc, InterfaceNetPort netPort) {
        try {
            mc = netPort.synchCallOverTheNet(mc, mc.getNetTimeout());
        } catch (MethodCallException e) {
            mc.setExceptionStatus(e.getTypeId());
        }
        sendSyncCallReturn(mc);
    }

    /**
     * Called in extread thread for call received from network
     *
     * @param mc Method call
     * @param mhandler Server/Handler handling call
     */
    public void executeCallFromNetwork(MethodCall mc, @Ptr AbstractMethodCallHandler mhandler) {
        mc.deserializeParamaters();
        mc.getMethod().executeFromMethodCallObject(mc, mhandler, null);
        if (!mc.getMethod().isVoidMethod()) {
            sendSyncCallReturn(mc);
        }
    }
}
