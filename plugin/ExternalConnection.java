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
package org.finroc.core.plugin;


import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;

import org.finroc.core.FrameworkElement;
import org.finroc.core.LockOrderLevels;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.parameter.StaticParameterString;
import org.finroc.core.parameter.StaticParameterList;
import org.finroc.core.remote.ModelHandler;
import org.finroc.core.remote.ModelNode;


/**
 * @author Max Reichardt
 *
 * This is an abstract base class for all (network-like) connections to
 * outside the runtime environment.
 *
 * The GUI offers such Connections to be established in it's menu.
 */
public abstract class ExternalConnection extends FrameworkElement {

    /** Group name of external connections */
    //public final static String GROUP_NAME = "Connections";

    /** last connection address */
    private String lastAddress;

    /** is class currently connected ? */
    private boolean connected = false;

    /** Connection Listeners */
    private final ConnectionListenerManager listener = new ConnectionListenerManager();

    /** Is this the first connect ? */
    private boolean firstConnect = true;

    /** if set, this module automatically connects to this address */
    private StaticParameterString autoConnectTo = new StaticParameterString("Autoconnect to", "");

    /** Model handler/manager for this connection */
    private ModelHandler modelHandler = new EmptyModelHandler();

    /**
     * @param name Name of class
     * @param defaultAddress Default connection address (some string)
     */
    public ExternalConnection(String name, String defaultAddress) {
        super(RuntimeEnvironment.getInstance(), name, Flag.NETWORK_ELEMENT, LockOrderLevels.LEAF_GROUP);
        StaticParameterList.getOrCreate(this).add(autoConnectTo);
        lastAddress = defaultAddress;
    }

    /**
     * Universal connect method
     *
     * @param address Address to connect to
     * @param modelHandler Model handler/manager for this connection (optional - may be NULL)
     */
    public synchronized void connect(String address, ModelHandler modelHandler) throws Exception {

        if (needsAddress()) {
            if (address == null || address.equals("")) {  // cancel pressed
                Log.log(LogLevel.ERROR, this, "No address specified. Cancelling connection attempt.");
                return;
            }
        }

        this.modelHandler = modelHandler;
        if (modelHandler == null) {
            this.modelHandler = new EmptyModelHandler();
        }

        connectImpl(address, (!firstConnect) && address.equals(lastAddress));
        postConnect(address);
    }

    /**
     * Called if connecting was successful
     *
     * @param address Address that was connected to
     */
    public void postConnect(String address) {
        lastAddress = address;
        firstConnect = false;
        fireConnectionEvent(ConnectionListener.CONNECTED);
    }

    /**
     * Performs the actual connecting.
     * This methods needs to be implemented by the subclass.
     *
     * @param address Address that shall be connected to
     * @param sameAddress Is this the same address as with the last connect
     */
    protected abstract void connectImpl(String address, boolean sameAddress) throws Exception;

    /**
     * Disconnect connection
     */
    public synchronized void disconnect() throws Exception {
        try {
            disconnectImpl();
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, this, e);
            //JavaOnlyBlock
            //JOptionPane.showMessageDialog(null, e.getMessage(), "Error disconnecting", JOptionPane.ERROR_MESSAGE);
        }
        fireConnectionEvent(ConnectionListener.NOT_CONNECTED);
    }

    /**
     * Performs actual disconnecting.
     * This methods needs to be implemented by the subclass.
     */
    protected abstract void disconnectImpl() throws Exception;

    /**
     * @return Address that we are currently connected to or which we were last connected to
     */
    public String getConnectionAddress() {
        return lastAddress;
    }

    /**
     * Connect (to same address as last time)
     */
    public synchronized void reconnect() throws Exception {
        connect(lastAddress, modelHandler);
    }

    public void addConnectionListener(ConnectionListener l) {
        listener.add(l);
    }

    public void removeConnectionListener(ConnectionListener l) {
        listener.remove(l);
    }

    public void fireConnectionEvent(int e) {
        connected = (e == ConnectionListener.CONNECTED);
        listener.notify(this, null, e);
    }

    @Override
    protected synchronized void prepareDelete() {
        try {
            disconnect();
        } catch (Exception e) {
            Log.log(LogLevel.ERROR, this, e);
        }
        super.prepareDelete();
    }

    /**
     * @return Is there currently a connection to another peer
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * @return Does this connection require entering an address?
     */
    protected boolean needsAddress() {
        return true;
    }

    public String toString() {
        return getClass().getSimpleName() + " - " + lastAddress;
    }

    /**
     * may be overridden by subclass
     */
    public void setLoopTime(long ms) {
    }

    /**
     * @return Connection quality information (in a scale from 0 (bad) to 1 (good))
     */
    public abstract float getConnectionQuality();

    /**
     * @param detailed Return more detailed information?
     * @return Connection status information for user
     */
    public String getStatus(boolean detailed) {
        return getConnectionAddress();
    }

    @Override
    public void evaluateStaticParameters() {
        String s = autoConnectTo.get();
        if (s.length() > 0) {
            if (!s.equals(lastAddress)) {
                if (isConnected()) {
                    try {
                        disconnect();
                    } catch (Exception e) {
                        Log.log(LogLevel.ERROR, this, e);
                    }
                }
                lastAddress = s;
            }
            if (!isConnected()) {
                try {
                    connect(s, null);
                } catch (Exception e) {
                    Log.log(LogLevel.ERROR, this, e);
                }
            }
        }
    }

    /**
     * @param address New default address
     */
    public void setDefaultAddress(String address) {
        this.lastAddress = address;
    }

    /**
     * @return The model handler/manager for this connection
     */
    public ModelHandler getModelHandler() {
        return modelHandler;
    }

    /**
     * Empty model for cases where no other model handle is set
     */
    public class EmptyModelHandler implements ModelHandler {

        @Override
        public void addNode(ModelNode parent, ModelNode newChild) {}

        @Override
        public void changeNodeName(ModelNode node, String newName) {}

        @Override
        public void removeNode(ModelNode childToRemove) {}

        @Override
        public void replaceNode(ModelNode oldNode, ModelNode newNode) {}

        @Override
        public void setModelRoot(ModelNode root) {}

        @Override
        public void updateModel(Runnable updateTask) {}
    }
}
