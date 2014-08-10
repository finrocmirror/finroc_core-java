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
package org.finroc.core.port.cc;

import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.ThreadLocalCache;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;

/**
 * @author Max Reichardt
 *
 * Port with upper and lower bounds for values
 */
public class CCPortBoundedNumeric<T extends CoreNumber> extends CCPortBase {

    /** Bounds of this port */
    private final Bounds<T> bounds;

    /**
     * @param pci Construction parameters in Port Creation Info Object
     */
    public CCPortBoundedNumeric(PortCreationInfo pci, Bounds<T> b) {
        super(processPciBNP(pci));
        this.bounds = b;
    }

    /**
     * Make sure non-standard assign flag is set
     */
    private static PortCreationInfo processPciBNP(PortCreationInfo pci) {
        pci.flags = pci.flags | Flag.NON_STANDARD_ASSIGN;
        pci.dataType = CoreNumber.TYPE;
        return pci;
    }

    protected void nonStandardAssign(ThreadLocalCache tc) {
        CoreNumber cn = (CoreNumber)tc.data.getObject().getData();
        double val = cn.doubleValue();
        if (cn.getUnit() != null && getUnit() != null && cn.getUnit() != getUnit()) {
            val = cn.getUnit().convertTo(val, getUnit());
        }
        if (!bounds.inBounds(val)) {
            if (tc.ref.getContainer().getRefCounter() == 0) { // still unused
                Log.log(LogLevel.DEBUG_WARNING, this, "Attempt to publish value that is out-of-bounds of output (!) port. This is typically not wise.");
                tc.ref.getContainer().recycleUnused();
            }
            if (bounds.discard()) {
                tc.ref = value;
                tc.data = tc.ref.getContainer();
            } else if (bounds.adjustToRange()) {
                CCPortDataManagerTL container = super.getUnusedBuffer(tc);
                CoreNumber cnc = (CoreNumber)container.getObject().getData();
                tc.data = container;
                tc.ref = container.getCurrentRef();
                cnc.setValue(bounds.toBounds(val), cn.getUnit());
            } else if (bounds.applyDefault()) {
                tc.data = tc.getUnusedBuffer(CoreNumber.TYPE);
                CoreNumber cnc = (CoreNumber)tc.data.getObject().getData();
                tc.ref = tc.data.getCurrentRef();
                cnc.setValue(bounds.getOutOfBoundsDefault());
                tc.data.setRefCounter(0); // locks will be added during assign
            }
        }
        //super.assign(tc); done anyway
    }

    /**
     * @return the bounds of this port
     */
    public Bounds<T> getBounds() {
        return bounds;
    }

    /**
     * Set Bounds
     * (This is not thread-safe and must only be done in "pause mode")
     *
     * @param bounds2 new Bounds for this port
     */
    public void setBounds(Bounds<T> bounds2) {
        bounds.set(bounds2);
        CCPortDataManager mgr = super.getInInterThreadContainer();
        CoreNumber cn = (CoreNumber)mgr.getObject().getData();
        double val = cn.doubleValue();
        if (cn.getUnit() != null && getUnit() != null && cn.getUnit() != getUnit()) {
            val = cn.getUnit().convertTo(val, getUnit());
        }
        if (!bounds.inBounds(val)) {
            if (bounds.discard()) {
                Log.log(LogLevel.WARNING, this, "Cannot discard value - applying default");
                applyDefaultValue();
            } else if (bounds.adjustToRange()) {
                CCPortDataManagerTL buf = ThreadLocalCache.getFast().getUnusedBuffer(getDataType());
                ((CoreNumber)buf.getObject().getData()).setValue(bounds.toBounds(val));
                super.publish(buf);
            } else if (bounds.applyDefault()) {
                applyDefaultValue();
            }
        }
        mgr.recycle2();
    }

    @Override
    public String browserPublishRaw(CCPortDataManagerTL buffer) {
        if (buffer.getObject().getType() != CoreNumber.TYPE) {
            return "Buffer has wrong type";
        }
        CoreNumber cn = (CoreNumber)buffer.getObject().getData();
        double val = cn.doubleValue();
        if (cn.getUnit() != null && getUnit() != null && cn.getUnit() != getUnit()) {
            val = cn.getUnit().convertTo(val, getUnit());
        }
        if (!bounds.inBounds(val)) {
            return "Value " + val + " is out of bounds " + bounds.toString();
        }

        return super.browserPublishRaw(buffer);
    }
}
