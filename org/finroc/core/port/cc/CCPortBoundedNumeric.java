/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010-2011 Max Reichardt,
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
package org.finroc.core.port.cc;

import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.Ref;
import org.finroc.log.LogLevel;

/**
 * @author max
 *
 * Port with upper and lower bounds for values
 */
@Inline @NoCpp @RawTypeArgs
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
        pci.flags = pci.flags | PortFlags.NON_STANDARD_ASSIGN;
        pci.dataType = CoreNumber.TYPE;
        return pci;
    }

    protected void nonStandardAssign(ThreadLocalCache tc) {
        @Const @Ptr CoreNumber cn = tc.data.getObject().<CoreNumber>getData();
        @InCpp("T val = cn->value<T>();")
        double val = cn.doubleValue();
        if (!bounds.inBounds(val)) {
            if (bounds.discard()) {
                tc.ref = value;
                tc.data = tc.ref.getContainer();
            } else if (bounds.adjustToRange()) {
                @Ptr CCPortDataManagerTL container = super.getUnusedBuffer(tc);
                @Ptr CoreNumber cnc = container.getObject().<CoreNumber>getData();
                tc.data = container;
                tc.ref = container.getCurrentRef();
                cnc.setValue(bounds.toBounds(val), cn.getUnit());
            } else if (bounds.applyDefault()) {
                tc.data = tc.getUnusedBuffer(CoreNumber.TYPE);
                @Ptr CoreNumber cnc = tc.data.getObject().<CoreNumber>getData();
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
    @ConstMethod public Bounds<T> getBounds() {
        return bounds;
    }

    /**
     * Set Bounds
     * (This is not thread-safe and must only be done in "pause mode")
     *
     * @param bounds2 new Bounds for this port
     */
    public void setBounds(@Const @Ref Bounds<T> bounds2) {
        bounds.set(bounds2);
        CCPortDataManager mgr = super.getInInterThreadContainer();
        @Const @Ptr CoreNumber cn = mgr.getObject().<CoreNumber>getData();
        @InCpp("T val = cn->value<T>();")
        double val = cn.doubleValue();
        if (!bounds.inBounds(val)) {
            if (bounds.discard()) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Cannot discard value - applying default");
                applyDefaultValue();
            } else if (bounds.adjustToRange()) {
                CCPortDataManagerTL buf = ThreadLocalCache.getFast().getUnusedBuffer(getDataType());
                buf.getObject().<CoreNumber>getData().setValue(bounds.toBounds(val));
                super.publish(buf);
            } else if (bounds.applyDefault()) {
                applyDefaultValue();
            }
        }
        mgr.recycle2();
    }
}
