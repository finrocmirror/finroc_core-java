//
// You received this file as part of Finroc
// A framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//----------------------------------------------------------------------
package org.finroc.core.port.rpc.internal;

import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.log.LogLevel;


/**
 * @author Max Reichardt
 *
 * RPC port implementation. Type-less, derived from AbstractPort.
 *
 * Server is source port.
 * Client is target port.
 * One source may have multiple targets. However, a target may only
 * have one source in order to receive only one return value.
 */
public class RPCPort extends AbstractPort {

    /** Edges emerging from this port */
    protected final EdgeList<RPCPort> edgesSrc = new EdgeList<RPCPort>();

    /** Edges ending at this port - maximum of one in this class */
    protected final EdgeList<RPCPort> edgesDest = new EdgeList<RPCPort>();


    public RPCPort(PortCreationInfo creationInfo, Object callHandler) {
        super(creationInfo);
        initLists(edgesSrc, edgesDest);
        this.callHandler = callHandler;
    }

    /**
     * @return Pointer to object that handles calls on server side
     */
    public Object getCallHandler() {
        return callHandler;
    }

    /**
     * (Usually called on client ports)
     *
     * @param includeNetworkPorts Also return network ports?
     * @return "Server" Port that handles method call (or null if there is no such port)
     */
    public RPCPort getServer(boolean includeNetworkPorts) {
        RPCPort current = this;
        while (true) {
            if (current.isServer() || (includeNetworkPorts && current.getFlag(Flag.NETWORK_ELEMENT))) {
                return current;
            }

            RPCPort last = current;
            ArrayWrapper<RPCPort> it = edgesSrc.getIterable();
            for (int i = 0; i < it.size(); i++) {
                current = it.get(i);
                break;
            }

            if (current == null || current == last) {
                return null;
            }
        }
    }

    /**
     * @return Is this a server rpc port?
     */
    public boolean isServer() {
        return getFlag(Flag.ACCEPTS_DATA) && (!getFlag(Flag.EMITS_DATA));
    }

    /**
     * Sends call to somewhere else
     * (Meant to be called on network ports that forward calls to other runtime environments)
     *
     * @param callToSend Call that is sent
     */
    public void sendCall(AbstractCall callToSend) {
        throw new RuntimeException("Not a network port");
    }


    /** Object that handles calls on server side */
    private Object callHandler;

    @Override
    protected void connectionAdded(AbstractPort partner, boolean partnerIsDestination) {
        // Disconnect any server ports we might already be connected to.
        if (partnerIsDestination) {
            ArrayWrapper<RPCPort> it = edgesSrc.getIterable();
            for (int i = 0; i < it.size(); i++) {
                RPCPort outgoingConnection = it.get(i);
                if (outgoingConnection != partner) {
                    log(LogLevel.WARNING, logDomain, "Port was already connected to a server. Removing connection to '" + outgoingConnection.getQualifiedName() + "' and adding the new one to '" + partner.getQualifiedName() + "'.");
                    outgoingConnection.disconnectFrom(this);
                }
            }
        }
    }

    @Override
    protected ConnectDirection inferConnectDirection(AbstractPort other) {

        // Check whether one of the two ports is connected to a server
        RPCPort otherInterfacePort = (RPCPort)other;
        RPCPort serverPortOfThis = (isServer() || getFlag(Flag.NETWORK_ELEMENT)) ? this : getServer(false);
        RPCPort serverPortOfOther = (otherInterfacePort.isServer() || otherInterfacePort.getFlag(Flag.NETWORK_ELEMENT)) ? otherInterfacePort : otherInterfacePort.getServer(false);
        if (serverPortOfThis != null && serverPortOfOther != null) {
            log(LogLevel.WARNING, logDomain, "Both ports (this and " + other.getQualifiedLink() + ") are connected to a server already.");
        } else if (serverPortOfThis != null) {
            return ConnectDirection.TO_SOURCE;
        } else if (serverPortOfOther != null) {
            return ConnectDirection.TO_TARGET;
        }

        return super.inferConnectDirection(other);
    }

    @Override
    protected void initialPushTo(AbstractPort target, boolean reverse) {
        // do nothing in RPC port
    }

    @Override
    public void notifyDisconnect() {
        // do nothing in RPC port
    }

    @Override
    protected void setMaxQueueLengthImpl(int length) {
        // do nothing in RPC port
    }

    @Override
    protected int getMaxQueueLengthImpl() {
        // do nothing in RPC port
        return 0;
    }

    @Override
    protected void clearQueueImpl() {
        // do nothing in RPC port
    }

    @Override
    public void forwardData(AbstractPort other) {
        // do nothing in RPC port
    }

    @Override
    public void setMaxQueueLength(int length) {
        throw new RuntimeException("InterfacePorts do not have a queue");
    }

    @Override
    protected short getStrategyRequirement() {
        return 0;
    }



//    static boolean isFuturePointer(CallStorage& callStorage) {
//        return callStorage.callReadyForSending == &(callStorage.futureStatus); // slightly ugly... but memory efficient (and we have the assertions)
//    }
}
