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
package org.finroc.core.parameter;

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.Unit;
import org.finroc.core.port.Port;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.Ref;
import org.finroc.serialization.DataTypeBase;
import org.finroc.serialization.RRLibSerializable;

/**
 * @author max
 *
 * Parameter template class for standard types
 */
@Inline @NoCpp @PassByValue @RawTypeArgs
public class Parameter<T extends RRLibSerializable> extends Port<T> {

    public Parameter(@Const @Ref String description, FrameworkElement parent, @Const @Ref String configEntry, @Const @Ref @CppDefault("NULL") DataTypeBase dt) {
        this(description, parent, dt);
        setConfigEntry(configEntry);
    }

    public Parameter(@Const @Ref String description, FrameworkElement parent, @Const @Ref @CppDefault("NULL") DataTypeBase dt) {
        super(new PortCreationInfo(description, parent, getType(dt), PortFlags.INPUT_PORT));
        wrapped.addAnnotation(new ParameterInfo());
    }

    public Parameter(@Const @Ref String description, FrameworkElement parent, @Const @Ref String configEntry, Bounds<T> b, @Const @Ref @CppDefault("NULL") DataTypeBase dt, @CppDefault("NULL") Unit u) {
        this(description, parent, b, dt, u);
        setConfigEntry(configEntry);
    }

    public Parameter(@Const @Ref String description, FrameworkElement parent, Bounds<T> b, @CppDefault("NULL") @Const @Ref DataTypeBase dt, @CppDefault("NULL") Unit u) {
        super(new PortCreationInfo(description, parent, getType(dt), PortFlags.INPUT_PORT, u), b);
        wrapped.addAnnotation(new ParameterInfo());
    }

    @InCpp("return dt != NULL ? dt : rrlib::serialization::DataType<T>();")
    private static DataTypeBase getType(@Const @Ref DataTypeBase dt) {
        return dt;
    }

    /**
     * @param configEntry New Place in Configuration tree, this parameter is configured from (nodes are separated with dots)
     */
    public void setConfigEntry(@Const @Ref String configEntry) {
        ParameterInfo info = (ParameterInfo)wrapped.getAnnotation(ParameterInfo.TYPE);
        info.setConfigEntry(configEntry);
    }
}
