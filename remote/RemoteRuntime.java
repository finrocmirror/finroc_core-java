//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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

    /** UID */
    private static final long serialVersionUID = 62274497011893894L;

    /** Admin interface for remote runtime */
    private final AdminClient adminInterface;

    /** Remote types */
    private RemoteTypes remoteTypes;

    /**
     * Lookup for remote framework elements: remote handle => remote framework element
     * (should only be accessed by model thread)
     */
    public final HashMap<Integer, RemoteFrameworkElement> elementLookup = new HashMap<Integer, RemoteFrameworkElement>();


    public RemoteRuntime(String name, AdminClient adminInterface, RemoteTypes remoteTypes) {
        super(0, name);
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
}
