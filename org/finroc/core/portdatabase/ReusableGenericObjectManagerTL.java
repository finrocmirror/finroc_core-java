/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2011 Max Reichardt,
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
package org.finroc.core.portdatabase;

import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.container.ReusableTL;
import org.finroc.serialization.GenericObject;
import org.finroc.serialization.GenericObjectManager;

/**
 * @author max
 *
 * Reusable GenericObjectManager
 */
@Inline @NoCpp
public class ReusableGenericObjectManagerTL extends ReusableTL implements GenericObjectManager {

    /** Managed object */
    @JavaOnly
    private GenericObject managedObject;

    @Override
    @JavaOnly
    public GenericObject getObject() {
        return managedObject;
    }

    @Override
    @JavaOnly
    public void setObject(GenericObject managedObject) {
        assert(this.managedObject == null);
        this.managedObject = managedObject;
        ReusableGenericObjectManager.managerLookup.put(managedObject.getData(), this);
    }

    @JavaOnly
    public void delete() {
        ReusableGenericObjectManager.managerLookup.remove(getObject().getData());
        super.delete();
    }

    @Override
    protected void deleteThis() {
        //Cpp delete _M_getObject();
    }

    @ConstMethod
    @InCpp("return util::StringBuilder(getObject()->getType().getName()) + \" (\" + getObject()->getRawDataPtr() + \")\";")
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
    @JavaOnly
    public static <T> ReusableGenericObjectManagerTL getManager(@SharedPtr @Ref T data) {
        return (ReusableGenericObjectManagerTL)ReusableGenericObjectManager.managerLookup.get(data);
    }
}
