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

import java.util.HashMap;
import java.util.Map;

import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.BinarySerializable;
import org.rrlib.serialization.Serialization;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.serialization.rtti.GenericObject;

/**
 * @author Max Reichardt
 *
 * Remote Connect Options.
 * Bundles available options for connecting ports (bundled for convenience and locality of changes)
 *
 * Represents both finroc::core::tConnectOptions and finroc::core::tUriConnectOptions (which have compatible serialization)
 */
public class RemoteConnectOptions implements BinarySerializable, Comparable<RemoteConnectOptions> {

    /** Connection flags */
    public final static int
    // constant flags that are always transferred to finstruct
    FINSTRUCTED = 1 << 0,              //!< Was this connector created using finstruct (or loaded from XML file)?
    RECONNECT = 1 << 1,                //!< Recreate connection when connected port was deleted and a port with the same qualified name appears (Reconnecting is automatically done for connections to VOLATILE-flagged ports such as network ports)
    OPTIONAL = 1 << 2,                 //!< Flags optional connections. They are handled and reported as non-critical if they cannot be established.
    SCHEDULING_NEUTRAL = 1 << 3,       //!< Flags connections that should not have any effect on scheduling order

    // Connect direction
    // If none is set, connection direction is determined automatically. This should be appropriate for almost any situation. However, another direction may be enforced)
    // Both flags must not be set at the same time (typically causes an invalid_argument exception)
    DIRECTION_TO_DESTINATION = 1 << 4, //!< Specified port is destination port (port passed as argument to ConnectTo member function; or second port passed to static method)
    DIRECTION_TO_SOURCE = 1 << 5,      //!< Specified port is destination port (port passed as argument to ConnectTo member function; or second port passed to static method)

    // internal flag
    NON_PRIMARY_CONNECTOR = 1 << 6,    //!< Connector was created and is represented by higher-level connector (that still exists) (-> e.g. not necessary to transfer this connector to finstruct)

    // these flags are automatically set and transferred to Java
    CONVERSION = 1 << 14,              //!< Connector has type conversion operations attached that must be applied on publishing values
    NAMED_PARAMETERS = 1 << 15,        //!< Indicates whether connector options include named parameters (relevant mainly for uniform serialization of ConnectOptions and UriConnectorOptions (only the latter supports named parameters))

    // these flags are automatically set and not transferred to Java
    PUBLISHED = 1 << 16,               //!< Only relevant for URI connectors: Has been published
    DISCONNECTED = 1 << 17;            //!< Connector has been removed

    /** Flags for connection */
    public int flags;

    /** References to remote conversion operations */
    public RemoteTypeConversion operation1, operation2;

    /** Parameters for conversion operations */
    public GenericObject parameter1, parameter2;

    /** If sequence contains two operations: type after first operation (may be ambiguous otherwise) */
    public RemoteType intermediateType;

    /** Rating of conversion operations (is not serialized) */
    public Definitions.TypeConversionRating conversionRating;

    /** Additional parameters as key/value pairs */
    public HashMap<String, String> parameters;


    public RemoteConnectOptions() {};
    public RemoteConnectOptions(Definitions.TypeConversionRating conversionRating) {  // Constructor for possibly postponed conversion selection
        this.conversionRating = conversionRating;
    }
    public RemoteConnectOptions(Definitions.TypeConversionRating conversionRating, RemoteTypeConversion operation1) {
        this.conversionRating = conversionRating;
        this.operation1 = operation1;
    };
    public RemoteConnectOptions(Definitions.TypeConversionRating conversionRating, RemoteTypeConversion operation1, RemoteType intermediateType, RemoteTypeConversion operation2) {
        this.conversionRating = conversionRating;
        this.operation1 = operation1;
        this.intermediateType = intermediateType;
        this.operation2 = operation2;
    };

    /**
     * @return Number of conversion operations
     */
    public int conversionOperationCount() {
        return operation1 == null ? 0 : (operation2 == null ? 1 : 2);
    }

    /**
     * @return Whether a conversion operation needs to be selected
     */
    public boolean requiresOperationSelection() {
        return operation1 == null && conversionRating.ordinal() < Definitions.TypeConversionRating.IMPLICIT_CAST.ordinal();
    }

    /**
     * Deserialize (when connection partner sends info on connectors)
     *
     * @param stream Stream to deserialize from
     */
    public void deserialize(BinaryInputStream stream) throws Exception {
        operation1 = null;
        operation2 = null;
        parameter1 = null;
        parameter2 = null;
        intermediateType = null;

        flags = stream.readShort() & 0xFFFF;
        if ((flags & CONVERSION) != 0) {
            int size = stream.readByte();
            if (size > 2) {
                throw new Exception("Invalid sequence size");
            }
            for (int i = 0; i < size; i++) {
                int flags = stream.readByte();
                if ((flags & SERIALIZATION_FLAG_FULL_OPERATION) == 0) {
                    throw new Exception("currently only resolved conversion operations are supported");  // TODO: Is support for unresolved conversion operations necessary? (I do not think so)
                }
                RemoteTypeConversion conversion = (RemoteTypeConversion)stream.readRegisterEntry(Definitions.RegisterUIDs.CONVERSION_OPERATION.ordinal());
                GenericObject parameter = null;
                if (conversion.getParameter() != null) {
                    DataTypeBase localType = conversion.getParameter().getType().getDefaultLocalDataType();
                    if (localType == null) {
                        throw new Exception("Only locally available types are supported as connector parameters");
                    }
                    parameter = localType.createInstanceGeneric(null);
                }
                if ((flags & SERIALIZATION_FLAG_PARAMETER) != 0) {
                    if (conversion.getParameter() == null) {
                        throw new Exception("No parameter defined in conversion operation");
                    }
                    parameter.deserialize(stream, Serialization.DataEncoding.BINARY);
                }
                if (i == 1) {
                    operation1 = conversion;
                    parameter1 = parameter;
                } else {
                    operation2 = conversion;
                    parameter2 = parameter;
                }
            }
            if (size >= 2) {
                intermediateType = RemoteType.deserialize(stream);
            }
        }

        if (parameters != null) {
            parameters.clear();
        }
        if ((flags & NAMED_PARAMETERS) != 0) {
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            int size = stream.readInt();
            if (!stream.readBoolean()) {
                throw new Exception("Must be const-type map");
            }
            for (int i = 0; i < size; i++) {
                parameters.put(stream.readString(), stream.readString());
            }

        }

        conversionRating = null;
    }

    /**
     * Serialize connect options (typically for connecting ports)
     *
     * @param stream Stream to serialize to
     */
    public void serialize(BinaryOutputStream stream) {
        if (operation1 != null) {
            flags |= CONVERSION;
        }
        if (parameters != null && parameters.size() > 0) {
            flags |= NAMED_PARAMETERS;
        }

        stream.writeShort(flags);
        if ((flags & CONVERSION) != 0) {
            int size = conversionOperationCount();
            stream.writeByte(size);
            for (int i = 0; i < size; i++) {
                GenericObject parameter = i == 0 ? parameter1 : parameter2;
                RemoteTypeConversion conversion = i == 0 ? operation1 : operation2;
                boolean sendParameter = operation1.getParameter() != null && parameter != null && ((operation1.getParameter().getDefaultValue() == null) || (!Serialization.equals(parameter, operation1.getParameter().getDefaultValue())));
                int flags = SERIALIZATION_FLAG_FULL_OPERATION | (sendParameter ? SERIALIZATION_FLAG_PARAMETER : 0);
                stream.writeByte(flags);
                stream.writeShort(conversion.getHandle());
                if (sendParameter) {
                    parameter.serialize(stream, Serialization.DataEncoding.BINARY);
                }
            }
            if (size >= 2) {
                stream.writeShort(intermediateType.getHandle());
            }
        }

        if ((flags & NAMED_PARAMETERS) != 0) {
            int size = parameters == null ? 0 : parameters.size();
            stream.writeInt(size);
            stream.writeBoolean(true);
            if (size > 0) {
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    stream.writeString(entry.getKey());
                    stream.writeString(entry.getValue());
                }
            }
        }
    }

    /** Serialization flags */
    private final static int
    SERIALIZATION_FLAG_FULL_OPERATION = 1,
    SERIALIZATION_FLAG_PARAMETER = 2;


    @Override
    public int compareTo(RemoteConnectOptions o) {
        if (conversionRating == null || o.conversionRating == null) {
            return 0;
        }
        int c = Integer.compare(o.conversionRating.ordinal(), conversionRating.ordinal());
        return c != 0 ? c : (toString().compareTo(o.toString()));
    }

    /**
     * @return Number of explicit conversions (that possibly have a parameter)
     */
    public int getExplicitConversionCount() {
        switch (conversionRating) {
        case TWO_EXPLICIT_CONVERSIONS:
            return 2;
        case EXPLICIT_CONVERSION:
        case EXPLICIT_CONVERSION_AND_IMPLICIT_CAST:
        case EXPLICIT_CONVERSION_FROM_GENERIC_TYPE:
        case EXPLICIT_CONVERSION_TO_GENERIC_TYPE:
            return 1;
        default:
            return 0;
        }
    }

    public String toString() {
        if (conversionRating == null) {
            return "Unrated conversion"; // TODO we can output something more sensible if this is ever required
        }
        switch (conversionRating) {
        case NO_CONVERSION:
            return "No conversion";
        case IMPLICIT_CAST:
            return "Implicit cast";
        case TWO_IMPLICIT_CASTS:
            return "Implicit casts";
        case EXPLICIT_CONVERSION_TO_GENERIC_TYPE:
        case EXPLICIT_CONVERSION_FROM_GENERIC_TYPE:
        case EXPLICIT_CONVERSION:
            return "'" + operation1.toString() + "'";
        case EXPLICIT_CONVERSION_AND_IMPLICIT_CAST:

        case TWO_EXPLICIT_CONVERSIONS:
            if (operation1.getSupportedDestinationTypes() == RemoteTypeConversion.SupportedTypeFilter.SINGLE || operation2.getSupportedSourceTypes() == RemoteTypeConversion.SupportedTypeFilter.SINGLE) {
                return "'" + operation1.toString() + "' and '" + operation2.toString() + "'";
            } else {
                return "'" + operation1.toString() + "' (to " + intermediateType.getName() + ") and '" + operation2.toString() + "'";
            }
        case DEPRECATED_CONVERSION:
            return "(depracated) '" + operation1.toString() + "' and '" + operation2.toString() + "'";
        case IMPOSSIBLE:
            return "Impossible";
        }
        return "N/A";
    }

    /**
     * Sets value of named parameter
     *
     * @param parameterName Parameter name
     * @param parameterValue Parameter values
     */
    public void setNamedParameter(String parameterName, String parameterValue) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(parameterName, parameterValue);
    }

    /**
     * Obtains type conversion rating for current conversion operations
     * If rating is not yet set, computes it.
     *
     * @param sourcePort Source port
     * @param runtime Runtime that contains both port
     * @param destinationPort Destination port
     * @return Type conversion rating
     */
    public Definitions.TypeConversionRating getTypeConversionRating(RemotePort sourcePort, RemoteRuntime runtime, RemotePort destinationPort) {
        if (conversionRating == null) {
            if (operation1 == null) {
                conversionRating = sourcePort.getDataType() == destinationPort.getDataType() ? Definitions.TypeConversionRating.NO_CONVERSION : Definitions.TypeConversionRating.IMPLICIT_CAST;
            } else if (operation2 == null) {
                if (operation1.getSupportedSourceTypes() == RemoteTypeConversion.SupportedTypeFilter.SINGLE && operation1.getSupportedDestinationTypes() == RemoteTypeConversion.SupportedTypeFilter.SINGLE) {
                    conversionRating = (operation1.getSupportedSourceType() == sourcePort.getDataType() && operation1.getSupportedDestinationType() == destinationPort.getDataType()) ? Definitions.TypeConversionRating.EXPLICIT_CONVERSION : Definitions.TypeConversionRating.EXPLICIT_CONVERSION_AND_IMPLICIT_CAST;
                } else {
                    Definitions.TypeConversionRating rating = Definitions.TypeConversionRating.IMPOSSIBLE;
                    for (RemoteConnectOptions options : runtime.getConversionOptions(sourcePort.getDataType(), destinationPort.getDataType(), false)) {
                        if ((options.operation1 == operation1 || options.operation2 == operation1) && options.conversionRating.ordinal() >= Definitions.TypeConversionRating.EXPLICIT_CONVERSION_TO_GENERIC_TYPE.ordinal()) {
                            rating = options.conversionRating;
                            break;
                        }
                    }
                    conversionRating = rating;
                }
            } else {
                boolean firstOperationImplicit = operation1.isStaticCast() && runtime.isStaticCastImplicit(sourcePort.getDataType(), intermediateType);
                boolean secondOperationImplicit = operation2.isStaticCast() && runtime.isStaticCastImplicit(intermediateType, destinationPort.getDataType());
                if (firstOperationImplicit && secondOperationImplicit) {
                    conversionRating = Definitions.TypeConversionRating.TWO_IMPLICIT_CASTS;
                } else if (firstOperationImplicit || secondOperationImplicit) {
                    conversionRating = Definitions.TypeConversionRating.EXPLICIT_CONVERSION_AND_IMPLICIT_CAST;
                } else {
                    if ((operation1.getName().equals(RemoteTypeConversion.TO_STRING) && operation2.getName().equals(RemoteTypeConversion.STRING_DESERIALIZATION)) ||
                            (operation1.getName().equals(RemoteTypeConversion.BINARY_SERIALIZATION) && operation2.getName().equals(RemoteTypeConversion.BINARY_DESERIALIZATION))) {
                        conversionRating = Definitions.TypeConversionRating.DEPRECATED_CONVERSION;
                    } else {
                        conversionRating = Definitions.TypeConversionRating.TWO_EXPLICIT_CONVERSIONS;
                    }
                }
            }
        }
        return conversionRating;
    }
}
