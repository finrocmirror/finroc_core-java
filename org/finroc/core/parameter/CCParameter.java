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
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.cc.CCPort;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.portdatabase.DataType;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.NoOuterClass;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.RawTypeArgs;
import org.finroc.jc.annotation.Ref;
import org.finroc.log.LogLevel;

/**
 * @author max
 *
 * Parameter template class for cc types
 */
@Inline @NoCpp @PassByValue @RawTypeArgs
public class CCParameter<T extends CCPortData> extends CCPort<T> {

    /** Special Port class to load value when initialized */
    @AtFront @NoOuterClass @Inline @NoCpp
    public class PortImpl extends CCPortBase {

        /** Paramater info */
        public final ParameterInfo info = new ParameterInfo();

        public PortImpl(PortCreationInfo pci) {
            super(pci);
            addAnnotation(info);
        }

        @Override
        protected void postChildInit() {
            super.postChildInit();
            try {
                info.loadValue(true);
            } catch (Exception e) {
                log(LogLevel.LL_ERROR, FrameworkElement.logDomain, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public CCParameter(@Const @Ref String description, FrameworkElement parent, @Const @Ref T defaultValue, @Const @Ref String configEntry, @CppDefault("NULL") DataType dt) {
        this(description, parent, defaultValue, dt);
        ((PortImpl)wrapped).info.setConfigEntry(configEntry);
    }

    public CCParameter(@Const @Ref String description, FrameworkElement parent, @Const @Ref T defaultValue, @CppDefault("NULL") DataType dt) {
        wrapped = new PortImpl(new PortCreationInfo(description, parent, getType(dt), PortFlags.INPUT_PORT));
        setDefault(defaultValue);
    }

    @InCpp("return dt != NULL ? dt : DataTypeRegister::getInstance()->getDataType<T>();")
    private static DataType getType(DataType dt) {
        return dt;
    }

    /** For subclasses */
    protected CCParameter() {}
}
