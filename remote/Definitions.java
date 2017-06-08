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

import org.rrlib.serialization.PublishedRegisters;
import org.rrlib.serialization.Register;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Definitions for remote runtime info (equivalent to plugins/network_transport/runtime_info/definitions)
 */
public class Definitions {

    /** Release version number encoded in serialization info revision (YYMM) */
    public static final int SERIALIZATION_VERSION = 1703;

    /**
     * Register UIDs for data exchange
     */
    public enum RegisterUIDs {
        TYPE,
        STATIC_CAST,
        CONVERSION_OPERATION,
        SCHEME_HANDLER,
        CREATE_ACTION
    };

    /**
     * Enum on different levels of structure (framework elements and ports) exchanged among peers
     */
    public enum StructureExchange {
        NONE,               //<! No info on structure is sent
        SHARED_PORTS,       //<! Send info on shared ports to connection partner
        COMPLETE_STRUCTURE, //<! Send info on complete structure to connection partner (e.g. for fingui)
        FINSTRUCT,          //<! Send info on complete structure including port connections to partner (as required by finstruct)
    };

    /** Definitions of custom flags used in SerializationInfo */
    public static final int
    INFO_FLAG_STRUCTURE_EXCHANGE_FLAG_1 = 0x1,   //!< First four flags are used to encode tStructureExchange (two bits in reserve for future extensions)
    INFO_FLAG_STRUCTURE_EXCHANGE_FLAG_N = 0x8,
    INFO_FLAG_JAVA_CLIENT               = 0x10,  //!< Connected to Java Client?
    INFO_FLAG_DEBUG_PROTOCOL            = 0x20;  //!< Write debug info to protocol?


    /**
     * (only defined in Java)
     * Conversion rating: (Best) possible conversion from one data type to a destination type.
     * This enum can be used as a score (higher is preferred).
     */
    public enum TypeConversionRating {
        IMPOSSIBLE,
        DEPRECATED_CONVERSION,
        TWO_EXPLICIT_CONVERSIONS,
        EXPLICIT_CONVERSION_TO_GENERIC_TYPE,   // value for single operations only to detect deprecated casts
        EXPLICIT_CONVERSION_FROM_GENERIC_TYPE, // value for single operations only to detect deprecated casts
        EXPLICIT_CONVERSION_AND_IMPLICIT_CAST,
        EXPLICIT_CONVERSION,
        TWO_IMPLICIT_CASTS,
        IMPLICIT_CAST,
        NO_CONVERSION
    }

    // Registers that do not exist in Java runtimes
    private static Register<Object> STATIC_CAST_REGISTER = new Register<>(64, 64, 2);
    private static Register<Object> CONVERSION_OPERATION_REGISTER = new Register<>(64, 64, 2);
    private static Register<Object> SCHEME_HANDLER_REGISTER = new Register<>(16, 16, 1);
    private static Register<Object> CREATE_ACTION_REGISTER = new Register<>(64, 128, 4);

    static {
        try {
            DataTypeBase.registerForPublishing(RegisterUIDs.TYPE.ordinal(), RemoteType.class);
            PublishedRegisters.register(STATIC_CAST_REGISTER, RegisterUIDs.STATIC_CAST.ordinal(), RemoteStaticCast.class);
            PublishedRegisters.register(CONVERSION_OPERATION_REGISTER, RegisterUIDs.CONVERSION_OPERATION.ordinal(), RemoteTypeConversion.class);
            PublishedRegisters.register(SCHEME_HANDLER_REGISTER, RegisterUIDs.SCHEME_HANDLER.ordinal(), RemoteUriSchemeHandler.class);
            PublishedRegisters.register(CREATE_ACTION_REGISTER, RegisterUIDs.CREATE_ACTION.ordinal(), RemoteCreateAction.class);
            PublishedRegisters.setMinusOneElement(RemoteType.createLegacyRemoteNullTypeInstance());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

}
