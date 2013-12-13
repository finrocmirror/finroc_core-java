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
package org.finroc.core.parameter;

import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.rrlib.serialization.BinarySerializable;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Parameter template class for standard types
 */
public class Parameter<T extends BinarySerializable> extends Port<T> {

    public Parameter(String name, FrameworkElement parent, String configEntry, DataTypeBase dt) {
        super(new PortCreationInfo(name, parent, getType(dt), FrameworkElementFlags.INPUT_PORT));
        wrapped.addAnnotation(new ParameterInfo());
        setConfigEntry(configEntry);
    }

    public Parameter(String name, FrameworkElement parent, T defaultValue, Unit u, String configEntry, DataTypeBase dt) {
        super(new PortCreationInfo(name, parent, getType(dt), FrameworkElementFlags.INPUT_PORT, u));
        setDefault(defaultValue);
        wrapped.addAnnotation(new ParameterInfo());
        setConfigEntry(configEntry);
    }

    public Parameter(String name, FrameworkElement parent, DataTypeBase dt) {
        this(name, parent, "", dt);
    }

    //Cpp template <typename Q = T>
    public Parameter(String name, FrameworkElement parent, T defaultValue, Bounds<T> b, Unit u, String configEntry, DataTypeBase dt) {
        super(new PortCreationInfo(name, parent, getType(dt), FrameworkElementFlags.INPUT_PORT, u), b);
        setDefault(defaultValue);
        wrapped.addAnnotation(new ParameterInfo());
        setConfigEntry(configEntry);
    }

    private static DataTypeBase getType(DataTypeBase dt) {
        return dt;
    }

    /**
     * @param configEntry New Place in Configuration tree, this parameter is configured from (nodes are separated with dots)
     */
    public void setConfigEntry(String configEntry) {
        if (configEntry.length() > 0) {
            ParameterInfo info = (ParameterInfo)wrapped.getAnnotation(ParameterInfo.TYPE);
            info.setConfigEntry(configEntry);
        }
    }
}
