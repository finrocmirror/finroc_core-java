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
package org.finroc.core.port.cc;

import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.log.LogLevel;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author max
 *
 * Number port with upper and lower bounds for values
 */
public class PortNumericBounded extends PortNumeric {

    /** Special Port class to load value when initialized */
    @AtFront @Friend(PortNumericBounded.class)
    protected static class PortImpl extends PortImplNum {

        /** Bounds of this port */
        private final Bounds bounds;

        @InCppFile
        public PortImpl(PortCreationInfo pci, Bounds b, Unit u) {
            super(processPciBNP(pci), u);
            this.bounds = b;
        }

        @Override
        protected void nonStandardAssign(ThreadLocalCache tc) {
            @Const CoreNumber cn = (CoreNumber)(tc.data.getDataPtr());
            double val = cn.doubleValue();
            if (!bounds.inBounds(val)) {
                if (bounds.discard()) {
                    tc.ref = value;
                    tc.data = tc.ref.getContainer();
                } else if (bounds.adjustToRange()) {
                    @Ptr CCPortDataContainer<?> container = super.getUnusedBuffer(tc);
                    @Ptr CoreNumber cnc = (CoreNumber)container.getDataPtr();
                    tc.data = container;
                    tc.ref = container.getCurrentRef();
                    cnc.setValue(bounds.toBounds(val), cn.getUnit());
                } else if (bounds.applyDefault()) {
                    tc.data = tc.getUnusedBuffer(CoreNumber.TYPE);
                    tc.ref = tc.data.getCurrentRef();
                    tc.data.assign((CCPortData)bounds.getOutOfBoundsDefault());
                    tc.data.setRefCounter(0); // locks will be added during assign
                }
            }
            //super.assign(tc); done anyway
        }
    }

    /**
     * @param pci Port Creation info
     * @param b Bounds for this port
     */
    public PortNumericBounded(PortCreationInfo pci, Bounds b) {
        wrapped = new PortImpl(pci, b, pci.unit);
    }

    /**
     * (Constructor for subclasses with own port class)
     */
    protected PortNumericBounded() {
    }

    /**
     * Make sure non-standard assign flag is set
     */
    private static PortCreationInfo processPciBNP(PortCreationInfo pci) {
        pci.flags = pci.flags | PortFlags.NON_STANDARD_ASSIGN;
        pci.dataType = CoreNumber.TYPE;
        return pci;
    }

    /**
     * @return the bounds of this port
     */
    @ConstMethod public Bounds getBounds() {
        return ((PortImpl)wrapped).bounds;
    }

    /**
     * Set Bounds
     * (This is not thread-safe and must only be done in "pause mode")
     *
     * @param bounds2 new Bounds for this port
     */
    public void setBounds(@Const @Ref Bounds bounds2) {
        @Ptr Bounds bounds = ((PortImpl)wrapped).bounds;
        bounds.set(bounds2);
        double val = getDoubleRaw();
        if (!bounds.inBounds(val)) {
            if (bounds.discard()) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Cannot discard value - applying default");
                getWrapped().applyDefaultValue();
            } else if (bounds.adjustToRange()) {
                super.publish(bounds.toBounds(val));
            } else if (bounds.applyDefault()) {
                getWrapped().applyDefaultValue();
            }
        }
    }
}

