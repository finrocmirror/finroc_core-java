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

import org.finroc.core.datatype.CoreNumber;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.PublishedRegisters;
import org.rrlib.serialization.rtti.GenericObject;

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
        LISTS,               //!< All list types are supported
        ALL,                 //!< All types are supported

        // Special operations defined in rrlib_rtti_conversion (known in Java tooling)
        STATIC_CAST,         //!< Types supported by static casts (only used for tStaticCastOperation)
        FOR_EACH,            //!< Types supported by for-each operation
        GET_LIST_ELEMENT,    //!< Types supported by get list element
        ARRAY_TO_VECTOR,     //!< Types supported by array to vector operation
        GET_TUPLE_ELEMENT    //!< Types supported by get tuple element operation
    }

    public static final String STATIC_CAST_NAME = "static_cast", TO_EVENT_NAME = "ToEvent";

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
     * @return Handle of conversion operation that this one is not usually combined with (-1 if there is no such operation)
     */
    public short getNotUsuallyCombinedWithHandle() {
        return notUsuallyCombinedWithHandle;
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
        return supportedSourceTypes == SupportedTypeFilter.STATIC_CAST;
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
        int flags = stream.readByte();
        final int HAS_PARAMETER = 1;
        final int NOT_USUALLY_COMBINED_WITH = 2;
        if ((flags & HAS_PARAMETER) == HAS_PARAMETER) {
            parameter = new ParameterDefinition();
            parameter.deserialize(stream);
            if (!parameter.getType().isBinarySerializationSupported()) {
                Log.log(LogLevel.WARNING, "Parameter of type '" + parameter.getType().getName() + "' in remote type conversion '" + name + "' is not supported");
            }
        }
        if ((flags & NOT_USUALLY_COMBINED_WITH) == NOT_USUALLY_COMBINED_WITH) {
            notUsuallyCombinedWithHandle = stream.readShort();
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

    /**
     * toString method that takes fixed parameter into account (for get tuple element operation)
     *
     * @param fixedParameter Fixed parameter (tuple element index)
     * @return String representation
     */
    public String toString(GenericObject fixedParameter) {
        if (supportedSourceTypes == SupportedTypeFilter.GET_TUPLE_ELEMENT && fixedParameter != null) {
            return name + "<" + ((CoreNumber)fixedParameter.getData()).intValue() + ">";
        }
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

    /** Handle of conversion operation that this one is not usually combined with (-1 if there is no such operation) */
    private short notUsuallyCombinedWithHandle = -1;
}
