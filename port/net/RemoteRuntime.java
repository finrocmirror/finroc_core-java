/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010 Max Reichardt,
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
package org.finroc.core.port.net;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.admin.AdminClient;
import org.rrlib.finroc_core_utils.jc.annotation.CppDefault;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.serialization.DataType;

/**
 * @author max
 *
 * Annotation for remote runtime environments
 * (only meant for use on local system)
 */
@JavaOnly
public class RemoteRuntime extends FinrocAnnotation {

    /** Data Type */
    public final static DataType<RemoteRuntime> TYPE = new DataType<RemoteRuntime>(RemoteRuntime.class);

    /** Admin interface for remote runtime */
    private final AdminClient adminInterface;

    /** Remote types */
    private RemoteTypes remoteTypes;

    /** Lookup for remote handles */
    private final RemoteHandleLookup remoteHandleLookup;

    public RemoteRuntime(AdminClient adminInterface, RemoteTypes remoteTypes, @Ptr @CppType("void") @CppDefault("NULL") RemoteHandleLookup lookup) {
        this.adminInterface = adminInterface;
        this.remoteTypes = remoteTypes;

        //JavaOnlyBlock
        this.remoteHandleLookup = lookup;
    }

    /**
     * @return Admin interface for remote runtime
     */
    public AdminClient getAdminInterface() {
        return adminInterface;
    }

    /**
     * @param fe Remote framework
     * @return Handle of remote framework element (null if not found)
     */
    @JavaOnly
    public Integer getRemoteHandle(FrameworkElement fe) {
        return remoteHandleLookup.getRemoteHandle(fe);
    }

    /**
     * @param handle Remote handle
     * @return Framework element that represents remote framework element with this remote handle
     */
    @JavaOnly
    public FrameworkElement getRemoteElement(int handle) {
        return remoteHandleLookup.getRemoteElement(handle);
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
        return find(np.getPort());
    }

    /**
     * Find RemoteRuntime to which specified element belongs to
     *
     * @param remoteElement Remote Element
     * @return RemoteRuntime object - or null if none could be found
     */
    public static RemoteRuntime find(FrameworkElement remoteElement) {
        return (RemoteRuntime)FinrocAnnotation.findParentWithAnnotation(remoteElement, TYPE);
    }
}
