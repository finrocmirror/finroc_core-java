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
package org.finroc.core.remote;

import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;

/**
 * @author Max Reichardt
 *
 * Remote Definition of single Parameter (see rrlib::rtti::tParameterDefinition)
 */
public class ParameterDefinition {

    /**
     * @return Default value of parameter (nullptr if not set)
     */
    public GenericObject getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return Name of parameter
     */
    public String getName() {
        return name;
    }

    /**
     * @return Type of parameter
     */
    public RemoteType getType() {
        return type;
    }

    /**
     * @return Whether parameter is static (or constant) and may not be changed at application runtime
     */
    public boolean isStatic() {
        return isStatic;
    }


    /**
     * @param stream Stream to deserialize from
     */
    public void deserialize(BinaryInputStream stream) throws Exception {
        name = stream.readString();
        type = (RemoteType)stream.readRegisterEntry(Definitions.RegisterUIDs.TYPE.ordinal());
        isStatic = stream.readBoolean();
        if (stream.readBoolean()) {
            DataTypeBase localType = type.getDefaultLocalDataType();
            if (localType == null) {
                throw new Exception("No local type available for default value (this is mandatory)");
            }
            defaultValue = localType.createInstanceGeneric(null);
            defaultValue.deserialize(stream, Serialization.DataEncoding.BINARY);
        } else {
            defaultValue = null;
        }
    }

    /** Name of parameter */
    private String name;

    /** Type of parameter */
    private RemoteType type;

    /** Default value of parameter (nullptr if not set) */
    private GenericObject defaultValue;

    /** Whether parameter is static (or constant) and may not be changed at application runtime */
    private boolean isStatic;
}