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

import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ptr;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author max
 *
 * Port containing numbers.
 */
@Inline @NoCpp
public class NumberPort extends CCPort<CoreNumber> {

    /** Unit of numerical port */
    private final @Ptr Unit unit;

    public NumberPort(PortCreationInfo pci) {
        super(processPciNP(pci));
        unit = pci.unit != null ? pci.unit : Unit.NO_UNIT;
    }

    @Init("unit(&(Unit::NO_UNIT))")
    public NumberPort(String description, boolean outputPort) {
        this(new PortCreationInfo(description, outputPort ? PortFlags.OUTPUT_PORT : PortFlags.INPUT_PORT));
    }

    private static PortCreationInfo processPciNP(PortCreationInfo pci) {
        pci.dataType = CoreNumber.getDataType();
        return pci;
    }

    /**
     * Set/Change port value.
     * (usually only called on output ports)
     *
     * @param d New Value
     */
    @Inline public void publish(double d) {
        CCPortDataRef value = this.value;
        if (value.getContainer().isOwnerThread() && ((CoreNumber)value.getData()).isDouble(d, unit)) {
            return;
        }
        ThreadLocalCache tc = ThreadLocalCache.get();
        CCPortDataContainer<?> ccdc = super.getUnusedBuffer(tc);
        @Ptr CoreNumber cnc = (CoreNumber)ccdc.getDataPtr();
        cnc.setValue(d, unit);
        super.publish(tc, ccdc);
    }

    /**
     * Set/Change port value.
     * (usually only called on output ports)
     *
     * @param d New Value
     */
    @Inline public void publish(int d) {
        if (value.getContainer().isOwnerThread() && ((CoreNumber)value.getData()).isInt(d, unit)) {
            return;
        }
        ThreadLocalCache tc = ThreadLocalCache.get();
        CCPortDataContainer<?> ccdc = super.getUnusedBuffer(tc);
        @Ptr CoreNumber cnc = (CoreNumber)ccdc.getDataPtr();
        cnc.setValue(d, unit);
        super.publish(tc, ccdc);
    }

    /*Cpp

    template<typename T>
    inline T getRaw() {
        if (pushStrategy()) {
            for(;;) {
                CCPortDataRef* val = value;
                CoreNumber* cn = (CoreNumber*)(val->getData());
                T d = cn->value<T>();
                if (val == value) {
                    return d;
                }
            }
        } else {
            CCPortDataContainer<CoreNumber>* dc = (CCPortDataContainer<CoreNumber>*)_M_pullValueRaw();
            T result = dc->getData()->value<T>();
            dc->releaseLock();
            return result;
        }
    }

     */

    /**
     * Get double value of port ignoring unit
     */
    @SuppressWarnings("unchecked")
    @Inline @InCpp("return getRaw<double>();")
    public double getDoubleRaw() {
        if (pushStrategy()) {
            for (;;) {
                CCPortDataRef val = value;
                double d = ((CoreNumber)val.getData()).doubleValue();
                if (val == value) { // still valid??
                    return d;
                }
            }
        } else {
            CCPortDataContainer<CoreNumber> dc = (CCPortDataContainer<CoreNumber>)pullValueRaw();
            double result = dc.getData().doubleValue();
            dc.releaseLock();
            return result;
        }
    }

    /**
     * Get int value of port ignoring unit
     */
    @SuppressWarnings("unchecked")
    @Inline @InCpp("return getRaw<int>();")
    public int getIntRaw() {
        if (pushStrategy()) {
            for (;;) {
                CCPortDataRef val = value;
                int d = ((CoreNumber)val.getData()).intValue();
                if (val == value) { // still valid??
                    return d;
                }
            }
        } else {
            CCPortDataContainer<CoreNumber> dc = (CCPortDataContainer<CoreNumber>)pullValueRaw();
            int result = dc.getData().intValue();
            dc.releaseLock();
            return result;
        }
    }

    /**
     * Set default value for port (default default value is zero)
     * (call before port is initialized)
     *
     * @param newDefault New default value
     */
    public void setDefault(double newDefault) {
        super.getDefaultBuffer().setValue(newDefault, unit);
    }

    public void setDefault(int newDefault) {
        super.getDefaultBuffer().setValue(newDefault, unit);
    }

    public void setDefault(long newDefault) {
        super.getDefaultBuffer().setValue(newDefault, unit);
    }

    public void setDefault(float newDefault) {
        super.getDefaultBuffer().setValue(newDefault, unit);
    }

    /**
     * @return Unit of port
     */
    public Unit getUnit() {
        return unit;
    }
}
