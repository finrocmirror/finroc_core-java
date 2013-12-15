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
package org.finroc.core.port.net;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import org.finroc.core.RuntimeEnvironment;
import org.rrlib.finroc_core_utils.jc.HasDestructor;
import org.rrlib.finroc_core_utils.jc.ListenerManager;
import org.rrlib.finroc_core_utils.jc.MutexLockOrder;

/**
 * @author Max Reichardt
 *
 * This is the abstract base class for "peer trackers".
 * Peer trackers look for other systems on the network that can be connected to.
 */
public abstract class AbstractPeerTracker implements HasDestructor {

    /** callIDs */
    private static final byte DISCOVERED = 0, REMOVED = 1;

    /** Tracker listeners */
    protected TrackerListenerManager listeners = new TrackerListenerManager();

    /** Peer tracker instances that are used - can be multiple */
    private static ArrayList<AbstractPeerTracker> instances = new ArrayList<AbstractPeerTracker>();

    /** Mutex for tracker */
    public final MutexLockOrder objMutex;

    /** "Lock" to above - for safe deinitialization */
    private ArrayList<AbstractPeerTracker> instancesLock = instances;

    /**
     * @param lockOrder Lock order of tracker
     */
    public AbstractPeerTracker(int lockOrder) {
        objMutex = new MutexLockOrder(lockOrder);
        synchronized (instances) {
            instances.add(this);
        }
    }

    /**
     * @param listener Listener to remove
     */
    public void removeListener(Listener listener) {

        // make sure: listener list can only be modified, while there aren't any other connection events being processed
        synchronized (RuntimeEnvironment.getInstance().getRegistryLock()) {
            synchronized (this) {
                listeners.remove(listener);
            }
        }
    }

    /**
     * @param listener Listener to add
     */
    public void addListener(Listener listener) {

        // make sure: listener list can only be modified, while there aren't any other connection events being processed
        synchronized (RuntimeEnvironment.getInstance().getRegistryLock()) {
            synchronized (this) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Called by subclass when TCP node has been discovered
     *
     * @param isa Node's network address
     * @param name Node's name
     */
    protected void notifyDiscovered(InetSocketAddress isa, String name) {
        listeners.notify(isa, name, DISCOVERED);
    }

    /**
     * Called by subclass when TCP node has been stopped/removed/deleted
     *
     * @param isa Node's network address
     * @param name Node's name
     */
    protected void notifyRemoved(InetSocketAddress isa, String name) {
        listeners.notify(null, name, REMOVED);
    }

    /**
     * Listens to discovery and removal of TCP nodes
     */
    public interface Listener {

        /**
         * Called when TCP node has been discovered
         *
         * @param isa Node's network address
         * @param name Node's name
         */
        public void nodeDiscovered(InetSocketAddress isa, String name);

        /**
         * Called when TCP node has been stopped/deleted
         *
         * @param isa Node's network address
         * @param name Node's name
         * @return Object to post-process without any locks (null if no post-processing necessary)
         *
         * (Called with runtime and Peer Tracker lock)
         */
        public Object nodeRemoved(InetSocketAddress isa, String name);

        /**
         * Called when TCP node has been deleted - and object for post-processing has been returned in method above
         *
         * @param obj Object to post-process
         *
         * (Called without/after runtime and Peer Tracker lock)
         */
        public void nodeRemovedPostLockProcess(Object obj);
    }

    public static class TrackerListenerManager extends ListenerManager<InetSocketAddress, String, Listener, TrackerListenerManager> {

        @Override
        public void singleNotify(Listener listener, InetSocketAddress origin, String parameter, int callId) {
            if (callId == AbstractPeerTracker.DISCOVERED) {
                listener.nodeDiscovered(origin, parameter);
            } else {
                listener.nodeRemoved(origin, parameter);
            }
        }
    }

    /**
     * Delete tracker
     */
    public void delete() {
        synchronized (instancesLock) {
            instancesLock.remove(this);
        }
    }

    /**
     * Register/publish server
     *
     * @param networkName Network name
     * @param name Name
     * @param port Port
     */
    public void registerServer(String networkName, String name, int port) {
        synchronized (instances) {
            for (int i = 0; i < instances.size(); i++) {
                instances.get(i).registerServerImpl(networkName, name, port);
            }
        }
    }

    /**
     * Register/publish server
     *
     * @param networkName Network name
     * @param name Name
     * @param port Port
     */
    protected void registerServerImpl(String networkName, String name, int port) {}

    /**
     * Unregister/unpublish server
     *
     * @param networkName Network name
     * @param name Name
     */
    public void unregisterServer(String networkName, String name) {
        synchronized (instances) {
            for (int i = 0; i < instances.size(); i++) {
                instances.get(i).unregisterServerImpl(networkName, name);
            }
        }
    }

    /**
     * UnRegister/unpublish server
     *
     * @param networkName Network name
     * @param name Name
     */
    protected void unregisterServerImpl(String networkName, String name) {}
}
