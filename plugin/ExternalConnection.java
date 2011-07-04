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
package org.finroc.core.plugin;

import javax.swing.JOptionPane;

import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Virtual;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;

import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.core.LockOrderLevels;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.parameter.StructureParameterString;
import org.finroc.core.parameter.StructureParameterList;

/**
 * @author max
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

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"connections\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("connections");

    /** if set, this module automatically connects to this address */
    private StructureParameterString autoConnectTo = new StructureParameterString("Autoconnect to", "");

    /**
     * @param description Description of class
     * @param defaultAddress Default connection address (some string)
     */
    public ExternalConnection(String description, String defaultAddress) {
        super(RuntimeEnvironment.getInstance(), description, CoreFlags.ALLOWS_CHILDREN | CoreFlags.NETWORK_ELEMENT, LockOrderLevels.LEAF_GROUP);
        StructureParameterList.getOrCreate(this).add(autoConnectTo);
        lastAddress = defaultAddress;
    }

    /**
     * Universal connect method
     *
     * @param address Address to connect to
     */
    public synchronized void connect(String address) throws Exception {

        // JavaOnlyBlock
        if (needsAddress()) {
            if (address == null) {
                address = JOptionPane.showInputDialog(null, getClass().getSimpleName() + ": Please input connection address", lastAddress);
            }
            if (address == null || address.equals("")) {  // cancel pressed
                return;
            }
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
    @Virtual protected abstract void connectImpl(String address, boolean sameAddress) throws Exception;

    /**
     * Disconnect connection
     */
    public synchronized void disconnect() throws Exception {
        try {
            disconnectImpl();
        } catch (Exception e) {
            log(LogLevel.LL_WARNING, logDomain, e);

            //JavaOnlyBlock
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error disconnecting", JOptionPane.ERROR_MESSAGE);

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
        connect(lastAddress);
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
            log(LogLevel.LL_ERROR, logDomain, e);
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
    @Virtual
    public String getStatus(boolean detailed) {
        return getConnectionAddress();
    }

    @Override
    public void structureParametersChanged() {
        String s = autoConnectTo.get();
        if (s.length() > 0) {
            if (!s.equals(lastAddress)) {
                if (isConnected()) {
                    try {
                        disconnect();
                    } catch (Exception e) {
                        log(LogLevel.LL_ERROR, logDomain, e);
                    }
                }
                lastAddress = s;
            }
            if (!isConnected()) {
                try {
                    connect(s);
                } catch (Exception e) {
                    log(LogLevel.LL_ERROR, logDomain, e);
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

}
