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

import org.finroc.jc.annotation.Virtual;

import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;

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

    /**
     * @param description Description of class
     * @param defaultAddress Default connection address (some string)
     */
    public ExternalConnection(String description, String defaultAddress) {
        super(description, RuntimeEnvironment.getInstance(), CoreFlags.ALLOWS_CHILDREN | CoreFlags.NETWORK_ELEMENT);
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
            e.printStackTrace();

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
            e.printStackTrace();
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

//  protected class ConnectionListenerManager extends WeakRefListenerManager<ConnectionListener> {
//
//      @Override
//      protected void notifyObserver(ConnectionListener observer, Object... param) {
//          observer.connectionEvent(ExternalConnection.this, (Event)param[0]);
//      }
//  }
}
