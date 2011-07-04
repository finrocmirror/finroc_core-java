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

import org.rrlib.finroc_core_utils.jc.annotation.CppDelegate;
import org.rrlib.finroc_core_utils.jc.annotation.CppName;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.NoCpp;
import org.rrlib.finroc_core_utils.jc.annotation.PostProcess;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author max
 *
 * Port containing numbers.
 *
 * In C++ this class is not required.
 * In Java several implementation details (runtime performance, CoreNumber as actual data type)
 * make introducing a separate class favorable.
 */
@Inline @NoCpp @JavaOnly @CppDelegate(Port.class) @CppName("Port")
public class PortNumeric<T extends Number> extends Port<CoreNumber> {

    /**
     * for subclasses
     */
    protected PortNumeric() {}

    public PortNumeric(PortCreationInfo pci) {
        super(pci.derive(CoreNumber.TYPE));
    }

    public PortNumeric(String description, boolean outputPort) {
        this(new PortCreationInfo(description, outputPort ? PortFlags.OUTPUT_PORT : PortFlags.INPUT_PORT));
    }

    public PortNumeric(String description, FrameworkElement parent, boolean outputPort) {
        this(new PortCreationInfo(description, parent, outputPort ? PortFlags.OUTPUT_PORT : PortFlags.INPUT_PORT));
    }

    @SuppressWarnings( { "unchecked", "rawtypes" })
    public PortNumeric(PortCreationInfo pci, Bounds<T> b) {
        super(pci.derive(CoreNumber.TYPE), (Bounds)b);
    }

    @SuppressWarnings( { "unchecked", "rawtypes" })
    public PortNumeric(String description, boolean outputPort, Bounds<T> b) {
        this(new PortCreationInfo(description, outputPort ? PortFlags.OUTPUT_PORT : PortFlags.INPUT_PORT), (Bounds)b);
    }

    @SuppressWarnings( { "unchecked", "rawtypes" })
    public PortNumeric(String description, FrameworkElement parent, boolean outputPort, Bounds<T> b) {
        this(new PortCreationInfo(description, parent, outputPort ? PortFlags.OUTPUT_PORT : PortFlags.INPUT_PORT), (Bounds)b);
    }

    /**
     * Set/Change port value.
     * (usually only called on output ports)
     *
     * @param d New Value
     */
    @Inline public void publish(double d) {
        @Ptr CCPortBase wrapped = (CCPortBase)super.wrapped;
        CCPortDataRef value = wrapped.value;
        if (value.getContainer().isOwnerThread() && value.getData().<CoreNumber>getData().isDouble(d, getUnit())) {
            return;
        }
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        CCPortDataManagerTL ccdc = wrapped.getUnusedBuffer(tc);
        @Ptr CoreNumber cnc = ccdc.getObject().getData();
        cnc.setValue(d, getUnit());
        wrapped.publish(tc, ccdc);
    }

    /**
     * Set/Change port value.
     * (usually only called on output ports)
     *
     * @param d New Value
     */
    @Inline public void publish(long d) {
        @Ptr CCPortBase wrapped = (CCPortBase)super.wrapped;
        CCPortDataRef value = wrapped.value;
        if (value.getContainer().isOwnerThread() && value.getData().<CoreNumber>getData().isLong(d, getUnit())) {
            return;
        }
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        CCPortDataManagerTL ccdc = wrapped.getUnusedBuffer(tc);
        @Ptr CoreNumber cnc = ccdc.getObject().getData();
        cnc.setValue(d, getUnit());
        wrapped.publish(tc, ccdc);
    }

    /**
     * Set/Change port value.
     * (usually only called on output ports)
     *
     * @param d New Value
     */
    @Inline public void publish(float d) {
        @Ptr CCPortBase wrapped = (CCPortBase)super.wrapped;
        CCPortDataRef value = wrapped.value;
        if (value.getContainer().isOwnerThread() && value.getData().<CoreNumber>getData().isFloat(d, getUnit())) {
            return;
        }
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        CCPortDataManagerTL ccdc = wrapped.getUnusedBuffer(tc);
        @Ptr CoreNumber cnc = ccdc.getObject().getData();
        cnc.setValue(d, getUnit());
        wrapped.publish(tc, ccdc);
    }

    /**
     * Set/Change port value.
     * (usually only called on output ports)
     *
     * @param d New Value
     */
    @Inline public void publish(int d) {
        @Ptr CCPortBase wrapped = (CCPortBase)super.wrapped;
        CCPortDataRef value = wrapped.value;
        if (value.getContainer().isOwnerThread() && value.getData().<CoreNumber>getData().isInt(d, getUnit())) {
            return;
        }
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        CCPortDataManagerTL ccdc = wrapped.getUnusedBuffer(tc);
        @Ptr CoreNumber cnc = ccdc.getObject().getData();
        cnc.setValue(d, getUnit());
        wrapped.publish(tc, ccdc);
    }

    /*Cpp

    // Set/Change port value.
    // (usually only called on output ports)
    //
    // \param d New Value
    inline void publish(T v) {
        CCPortDataRef value = wrapped->value;
        if (value.getContainer()->isOwn
    }



    inline T getRaw() {
        if (pushStrategy()) {
            for(;;) {
                CCPortDataRef* val = wrapped->value;
                Number* cn = (Number*)(val->getData());
                T d = cn->value<T>();
                if (val == wrapped->value) {
                    return d;
                }
            }
        } else {
            CCPortDataContainer<Number>* dc = (CCPortDataContainer<Number>*)wrapped->pullValueRaw();
            T result = dc->getData()->value<T>();
            dc->releaseLock();
            return result;
        }
    }

     */

    /**
     * Get double value of port ignoring unit
     */
    @PostProcess("org.finroc.j2c.NumericPort")
    public double getDoubleRaw() {
        @Ptr CCPortBase wrapped = (CCPortBase)super.wrapped;
        if (pushStrategy()) {
            for (;;) {
                CCPortDataRef val = wrapped.value;
                double d = val.getData().<CoreNumber>getData().doubleValue();
                if (val == wrapped.value) { // still valid??
                    return d;
                }
            }
        } else {
            CCPortDataManagerTL dc = wrapped.pullValueRaw();
            double result = dc.getObject().<CoreNumber>getData().doubleValue();
            dc.releaseLock();
            return result;
        }
    }

    /**
     * Get int value of port ignoring unit
     */
    @PostProcess("org.finroc.j2c.NumericPort")
    public int getIntRaw() {
        @Ptr CCPortBase wrapped = (CCPortBase)super.wrapped;
        if (pushStrategy()) {
            for (;;) {
                CCPortDataRef val = wrapped.value;
                int d = val.getData().<CoreNumber>getData().intValue();
                if (val == wrapped.value) { // still valid??
                    return d;
                }
            }
        } else {
            CCPortDataManagerTL dc = wrapped.pullValueRaw();
            int result = dc.getObject().<CoreNumber>getData().intValue();
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
        super.setDefault(new CoreNumber(newDefault, getUnit()));
    }

    public void setDefault(int newDefault) {
        super.setDefault(new CoreNumber(newDefault, getUnit()));
    }

    public void setDefault(long newDefault) {
        super.setDefault(new CoreNumber(newDefault, getUnit()));
    }

    public void setDefault(float newDefault) {
        super.setDefault(new CoreNumber(newDefault, getUnit()));
    }

    public void setDefault(CoreNumber newDefault) {
        super.setDefault(newDefault);
    }

    /**
     * @return Unit of port
     */
    public Unit getUnit() {
        return ((CCPortBase)wrapped).getUnit();
    }
}
