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
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.PublishedRegisters;

/**
 * @author Max Reichardt
 *
 * Remote type conversion operation
 *
 * Represents type conversion operation from rrlib_rtti_conversion in remote runtime.
 */
public class RemoteTypeConversion extends PublishedRegisters.RemoteEntryBase<Object> {

    /** Types supported by conversion operation */
    public enum SupportedTypeFilter {
        SINGLE,              //!< Only a single type is supported
        BINARY_SERIALIZABLE, //!< All binary-serializable types are supported
        STRING_SERIALIZABLE, //!< All string-serializable types are supported
        ALL,                 //!< All types are supported

        // Special operations defined in rrlib_rtti_conversion (known in Java tooling)
        STATIC_CAST,         //!< Types supported by static casts (only used for tStaticCastOperation)
        GENERIC_VECTOR_CAST, //!< Types supported by generic vector cast
        GET_LIST_ELEMENT     //!< Types supported by get list element
    }

    /** Names of some predefined operations in rrlib_rtti_conversion */
    public static final String STATIC_CAST = "static_cast", GET_LIST_ELEMENT = "[]", FOR_EACH = "For Each", BINARY_DESERIALIZATION = "Binary Deserialization",
                               BINARY_SERIALIZATION = "Binary Serialization", TO_STRING = "ToString", STRING_DESERIALIZATION = "String Deserialization";


    /**
     * @return Name of conversion operation
     */
    public String getName() {
        return name;
    }

    /**
     * @return Parameter definition if conversion operation has a parameter (otherwise null)
     */
    public ParameterDefinition getParameter() {
        return parameter;
    }

    /**
     * @return In case 'getSupportedDestinationTypes()' is SINGLE, contains uid of single supported destination type
     */
    public RemoteType getSupportedDestinationType() {
        return supportedDestinationType;
    }

    /**
     * @return Supported destination types: type filter
     */
    public SupportedTypeFilter getSupportedDestinationTypes() {
        return supportedDestinationTypes;
    }

    /**
     * @return In case 'getSupportedSourceTypes()' is SINGLE, contains uid of single supported source type
     */
    public RemoteType getSupportedSourceType() {
        return supportedSourceType;
    }

    /**
     * @return Supported source types: type filter
     */
    public SupportedTypeFilter getSupportedSourceTypes() {
        return supportedSourceTypes;
    }

    /**
     * @return Whether this conversion operation is a static cast
     */
    public boolean isStaticCast() {
        return name.equals(STATIC_CAST);
    }


    @Override
    public void serializeLocalRegisterEntry(BinaryOutputStream stream, Object entry) {
        throw new RuntimeException("Not implemented (currently no type conversion operations in Java)");
    }

    @Override
    public void deserializeRegisterEntry(BinaryInputStream stream) throws Exception {
        name = stream.readString();
        supportedSourceTypes = stream.readEnum(SupportedTypeFilter.class);
        if (supportedSourceTypes == SupportedTypeFilter.SINGLE) {
            supportedSourceType = RemoteType.deserialize(stream);
        } else {
            supportedSourceType = null;
        }
        supportedDestinationTypes = stream.readEnum(SupportedTypeFilter.class);
        if (supportedDestinationTypes == SupportedTypeFilter.SINGLE) {
            supportedDestinationType = RemoteType.deserialize(stream);
        } else {
            supportedDestinationType = null;
        }
        if (stream.readBoolean()) {
            parameter = new ParameterDefinition();
            parameter.deserialize(stream);
        }
    }

    @Override
    public int getHandleSize() {
        return 2;
    }

    @Override
    public String toString() {
        return name;
    }

    /** Name of conversion operation */
    private String name;

    /** Supported source types: type filter */
    private SupportedTypeFilter supportedSourceTypes;

    /** In case 'supportedSourceTypes' is SINGLE, contains uid of single supported source type */
    private RemoteType supportedSourceType;

    /** Supported destination types: type filter */
    private SupportedTypeFilter supportedDestinationTypes;

    /** In case 'supportedDestinationTypes' is SINGLE, contains uid of single supported destination type */
    private RemoteType supportedDestinationType;

    /** Parameter definition if conversion operation has a parameter (otherwise null) */
    private ParameterDefinition parameter;

}
