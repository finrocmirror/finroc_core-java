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

import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author Max Reichardt
 *
 * Port containing numbers.
 *
 * In C++ this class is not required.
 * In Java several implementation details (runtime performance, CoreNumber as actual data type)
 * make introducing a separate class favorable.
 */
public class PortNumeric<T extends Number> extends Port<CoreNumber> {

    /**
     * for subclasses
     */
    protected PortNumeric() {}

    public PortNumeric(PortCreationInfo pci) {
        super(pci.derive(CoreNumber.TYPE));
    }

    public PortNumeric(String name, boolean outputPort) {
        this(new PortCreationInfo(name, outputPort ? FrameworkElementFlags.OUTPUT_PORT : FrameworkElementFlags.INPUT_PORT));
    }

    public PortNumeric(String name, FrameworkElement parent, boolean outputPort) {
        this(new PortCreationInfo(name, parent, outputPort ? FrameworkElementFlags.OUTPUT_PORT : FrameworkElementFlags.INPUT_PORT));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PortNumeric(PortCreationInfo pci, Bounds<T> b) {
        super(pci.derive(CoreNumber.TYPE), (Bounds)b);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PortNumeric(String name, boolean outputPort, Bounds<T> b) {
        this(new PortCreationInfo(name, outputPort ? FrameworkElementFlags.OUTPUT_PORT : FrameworkElementFlags.INPUT_PORT), (Bounds)b);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PortNumeric(String name, FrameworkElement parent, boolean outputPort, Bounds<T> b) {
        this(new PortCreationInfo(name, parent, outputPort ? FrameworkElementFlags.OUTPUT_PORT : FrameworkElementFlags.INPUT_PORT), (Bounds)b);
    }

    /**
     * Set/Change port value.
     * (usually only called on output ports)
     *
     * @param d New Value
     */
    public void publish(double d) {
        CCPortBase wrapped = (CCPortBase)super.wrapped;
        CCPortDataRef value = wrapped.value;
        if (value.getContainer().isOwnerThread() && value.getData().<CoreNumber>getData().isDouble(d, getUnit())) {
            return;
        }
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        CCPortDataManagerTL ccdc = wrapped.getUnusedBuffer(tc);
        CoreNumber cnc = ccdc.getObject().getData();
        cnc.setValue(d, getUnit());
        wrapped.publish(tc, ccdc);
    }

    /**
     * Set/Change port value.
     * (usually only called on output ports)
     *
     * @param d New Value
     */
    public void publish(long d) {
        CCPortBase wrapped = (CCPortBase)super.wrapped;
        CCPortDataRef value = wrapped.value;
        if (value.getContainer().isOwnerThread() && value.getData().<CoreNumber>getData().isLong(d, getUnit())) {
            return;
        }
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        CCPortDataManagerTL ccdc = wrapped.getUnusedBuffer(tc);
        CoreNumber cnc = ccdc.getObject().getData();
        cnc.setValue(d, getUnit());
        wrapped.publish(tc, ccdc);
    }

    /**
     * Set/Change port value.
     * (usually only called on output ports)
     *
     * @param d New Value
     */
    public void publish(float d) {
        CCPortBase wrapped = (CCPortBase)super.wrapped;
        CCPortDataRef value = wrapped.value;
        if (value.getContainer().isOwnerThread() && value.getData().<CoreNumber>getData().isFloat(d, getUnit())) {
            return;
        }
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        CCPortDataManagerTL ccdc = wrapped.getUnusedBuffer(tc);
        CoreNumber cnc = ccdc.getObject().getData();
        cnc.setValue(d, getUnit());
        wrapped.publish(tc, ccdc);
    }

    /**
     * Set/Change port value.
     * (usually only called on output ports)
     *
     * @param d New Value
     */
    public void publish(int d) {
        CCPortBase wrapped = (CCPortBase)super.wrapped;
        CCPortDataRef value = wrapped.value;
        if (value.getContainer().isOwnerThread() && value.getData().<CoreNumber>getData().isInt(d, getUnit())) {
            return;
        }
        ThreadLocalCache tc = ThreadLocalCache.getFast();
        CCPortDataManagerTL ccdc = wrapped.getUnusedBuffer(tc);
        CoreNumber cnc = ccdc.getObject().getData();
        cnc.setValue(d, getUnit());
        wrapped.publish(tc, ccdc);
    }

    /**
     * Get double value of port ignoring unit
     */
    public double getDoubleRaw() {
        CCPortBase wrapped = (CCPortBase)super.wrapped;
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
    public int getIntRaw() {
        CCPortBase wrapped = (CCPortBase)super.wrapped;
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
