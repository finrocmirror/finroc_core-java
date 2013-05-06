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
package org.finroc.core.portdatabase;

import java.util.concurrent.ConcurrentHashMap;

import org.finroc.core.datatype.Timestamp;
import org.rrlib.finroc_core_utils.jc.container.Reusable;
import org.rrlib.finroc_core_utils.rtti.GenericObject;
import org.rrlib.finroc_core_utils.rtti.GenericObjectManager;

/**
 * @author Max Reichardt
 *
 * Reusable GenericObjectManager
 */
public abstract class ReusableGenericObjectManager extends Reusable implements GenericObjectManager {

    /** Managed object */
    private GenericObject managedObject;

    /** Timestamp of attached data */
    public final Timestamp timestamp = new Timestamp();

    /** Lookup in Java to get manager from managed object */
    static final ConcurrentHashMap<Object, GenericObjectManager> managerLookup = new ConcurrentHashMap<Object, GenericObjectManager>();

    @Override
    public GenericObject getObject() {
        return managedObject;
    }

    /**
     * @return Timestamp of attached data
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    @Override
    public void setObject(GenericObject managedObject) {
        assert(this.managedObject == null);
        this.managedObject = managedObject;
        managerLookup.put(managedObject.getData(), this);
    }

    public void delete() {
        managerLookup.remove(getObject().getData());
        super.delete();
    }

    @Override
    protected void deleteThis() {
    }

    public String getContentString() {
        return getObject() != null && getObject().getData() != null ? (getObject().getType().getName() + ": " + getObject().getData().toString()) : "null content";
    }

    /**
     * Retrieve manager for port data
     *
     * @param data Port data
     * @param resetActiveFlag Reset active flag (set when unused buffers are handed to user)
     * @return Manager for port data
     */
    public static <T> GenericObjectManager getManager(T data) {
        return managerLookup.get(data);
    }

    /**
     * Release lock
     */
    public abstract void genericLockRelease();

    /**
     * @return Does generic object have a lock?
     */
    public abstract boolean genericHasLock();
}
