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
package org.finroc.core.remote;

import java.util.HashMap;

import org.finroc.core.admin.AdminClient;
import org.finroc.core.port.net.NetPort;

/**
 * @author Max Reichardt
 *
 * Represents remote runtime environments
 * (only meant for use on local system)
 */
public class RemoteRuntime extends RemoteFrameworkElement {

    /** Admin interface for remote runtime */
    private final AdminClient adminInterface;

    /** Remote types */
    private RemoteTypes remoteTypes;

    /**
     * Lookup for remote framework elements: remote handle => remote framework element
     * (should only be accessed by model thread)
     */
    public final HashMap<Integer, RemoteFrameworkElement> elementLookup = new HashMap<Integer, RemoteFrameworkElement>();

    /** Remote Runtime's UUID */
    public final String uuid;

    public RemoteRuntime(String name, String uuid, AdminClient adminInterface, RemoteTypes remoteTypes) {
        super(0, name);
        this.uuid = uuid;
        this.adminInterface = adminInterface;
        this.remoteTypes = remoteTypes;
        elementLookup.put(0, this); // Runtime lookup
    }

    /**
     * @return Admin interface for remote runtime
     */
    public AdminClient getAdminInterface() {
        return adminInterface;
    }

    /**
     * @param remoteTypes Remote types
     */
    public void setRemoteTypes(RemoteTypes remoteTypes) {
        this.remoteTypes = remoteTypes;
    }

    /**
     * @return Remote types
     */
    public RemoteTypes getRemoteTypes() {
        return remoteTypes;
    }

    /**
     * Find RemoteRuntime to which specified port belongs to
     *
     * @param np remote port
     * @return RemoteRuntime object - or null if none could be found
     */
    public static RemoteRuntime find(NetPort np) {
        return find(RemotePort.get(np.getPort())[0]);
    }

    /**
     * Find RemoteRuntime to which specified element belongs to
     *
     * @param remoteElement Remote Element
     * @return RemoteRuntime object - or null if none could be found
     */
    public static RemoteRuntime find(ModelNode remoteElement) {
        if (remoteElement == null) {
            return null;
        }
        do {
            if (remoteElement instanceof RemoteRuntime) {
                return (RemoteRuntime)remoteElement;
            }
            remoteElement = (ModelNode)remoteElement.getParent();
        } while (remoteElement != null);
        return null;
    }

    /**
     * @param handle Remote handle
     * @return Framework element that represents remote framework element with this remote handle
     */
    public RemoteFrameworkElement getRemoteElement(int handle) {
        return elementLookup.get(handle);
    }

    /**
     * Find other remote runtime environment with the specified UUID
     *
     * @param uuid uuid of other runtime environment
     * @return Other runtime - or null if no runtime environment with specified id could be found
     */
    public RemoteRuntime findOther(String uuid) {

        // A more sophisticated search might be necessary in the future
        ModelNode parent = this.getParent();
        if (parent != null) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                ModelNode child = parent.getChildAt(i);
                if (child instanceof RemoteRuntime && ((RemoteRuntime)child).uuid.equals(uuid)) {
                    return (RemoteRuntime)child;
                }
            }
        }
        return null;
    }
}
