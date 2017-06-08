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
package org.finroc.core.net.generic_protocol;

import java.util.HashMap;

import org.finroc.core.remote.ParameterDefinition;
import org.finroc.core.remote.RemoteConnectOptions;
import org.rrlib.serialization.Serialization;


/**
 * @author Max Reichardt
 *
 * Definitions for generic_protocol (equivalent to plugins/network_transport/generic_protocol/definitions)
 */
public class Definitions extends org.finroc.core.remote.Definitions {

    /**
     * Flags relevant for some messages (used to be tDataEncoding): Encoding of data and handles
     * (important: first three constants must be identical to rrlib::serialization::tDataEncoding)
     */
    public static final int
    MESSAGE_FLAG_BINARY_ENCODING = 0,
    MESSAGE_FLAG_STRING_ENCODING = 1,
    MESSAGE_FLAG_XML_ENCODING = 2,
    MESSAGE_FLAG_BINARY_COMPRESSED_ENCODING = 3,
    MESSAGE_FLAG_TO_SERVER = 4,
    MESSAGE_FLAG_HIGH_PRIORITY = 8;

    /**
     * Protocol OpCodes
     */
    public enum OpCode {

        // Opcodes for management connection
        SUBSCRIBE_LEGACY,    // Subscribe to data port (legacy - only supported as client)
        UNSUBSCRIBE_LEGACY,  // Unsubscribe from data port (legacy - only supported as client)
        PULLCALL,            // Pull call
        PULLCALL_RETURN,     // Returning pull call
        RPC_CALL,            // RPC call
        TYPE_UPDATE,         // Update on remote type info (typically desired update time)
        STRUCTURE_CREATED,   // Update on remote framework elements: Element created
        STRUCTURE_CHANGED,   // Update on remote framework elements: Port changed
        STRUCTURE_DELETED,   // Update on remote framework elements: Element deleted
        PEER_INFO,           // Information about other peers

        // Change event opcodes (from subscription - or for plain setting of port)
        PORT_VALUE_CHANGE,                         // normal variant
        SMALL_PORT_VALUE_CHANGE,                   // variant with max. 256 byte message length (3 bytes smaller than normal variant)
        SMALL_PORT_VALUE_CHANGE_WITHOUT_TIMESTAMP, // variant with max. 256 byte message length and no timestamp (11 bytes smaller than normal variant)

        // new commands in 17.03
        CONNECT_PORTS,         // Connect ports in different processes
        CONNECT_PORTS_ERROR,   // Notifies client on connection error
        UPDATE_CONNECTION,     // Change dynamic connection data of connection created with 'CONNECT_PORTS'
        DISCONNECT_PORTS,      // Disconnect ports connected with 'CONNECT_PORT'
        CONNECTOR_CREATED,     // Connector created/added
        CONNECTOR_DELETED,     // Connector deleted
        URI_CONNECTOR_CREATED, // URI Connector created/added
        URI_CONNECTOR_UPDATED, // URI Connector changed/updated
        URI_CONNECTOR_DELETED, // URI Connector deleted

        // Used for custom additional messages - possibly without opcode
        OTHER
    }

    public enum MessageSize {
        FIXED,                   // fixed size message
        VARIABLE_UP_TO_255_BYTE, // variable message size up to 255 bytes
        VARIABLE_UP_TO_4GB       // variable message size up to 4GB
    };

    /** Message size encodings of different kinds of op codes */
    public static final MessageSize[] MESSAGE_SIZES = new MessageSize[] {
        MessageSize.FIXED,                   // SUBSCRIBE_LEGACY
        MessageSize.FIXED,                   // UNSUBSCRIBE_LEGACY
        MessageSize.FIXED,                   // PULLCALL
        MessageSize.VARIABLE_UP_TO_4GB,      // PULLCALL_RETURN
        MessageSize.VARIABLE_UP_TO_4GB,      // RPC_CALL
        MessageSize.VARIABLE_UP_TO_4GB,      // TYPE_UPDATE
        MessageSize.VARIABLE_UP_TO_4GB,      // STRUCTURE_CREATED
        MessageSize.VARIABLE_UP_TO_4GB,      // STRUCTURE_CHANGED
        MessageSize.FIXED,                   // STRUCTURE_DELETED
        MessageSize.VARIABLE_UP_TO_4GB,      // PEER_INFO
        MessageSize.VARIABLE_UP_TO_4GB,      // PORT_VALUE_CHANGE
        MessageSize.VARIABLE_UP_TO_255_BYTE, // SMALL_PORT_VALUE_CHANGE
        MessageSize.VARIABLE_UP_TO_255_BYTE, // SMALL_PORT_VALUE_CHANGE_WITHOUT_TIMESTAMP

        MessageSize.VARIABLE_UP_TO_4GB,      // CONNECT_PORTS
        MessageSize.VARIABLE_UP_TO_4GB,      // CONNECT_PORTS_ERROR
        MessageSize.FIXED,                   // UPDATE_CONNECTION
        MessageSize.FIXED,                   // DISCONNECT_PORTS
        MessageSize.VARIABLE_UP_TO_4GB,      // CONNECTOR_CREATED
        MessageSize.FIXED,                   // CONNECTOR_DELETED
        MessageSize.VARIABLE_UP_TO_4GB,      // URI_CONNECTOR_CREATED
        MessageSize.FIXED,                   // URI_CONNECTOR_UPDATED
        MessageSize.FIXED,                   // URI_CONNECTOR_DELETED

        MessageSize.VARIABLE_UP_TO_4GB,      // OTHER
    };

    /** Finroc TCP protocol version */
    public static final short PROTOCOL_VERSION_MAJOR = 1;
    public static final int PROTOCOL_VERSION_MINOR = org.finroc.core.remote.Definitions.SERIALIZATION_VERSION;

    /** Inserted at the end of messages to debug TCP stream */
    public static final byte DEBUG_TCP_NUMBER = (byte)0xCD;

    /** Maximum not acknowledged Packet */
    public static final int MAX_NOT_ACKNOWLEDGED_PACKETS = 0x1F; // 32 (2^x for fast modulo)

    /** Packets considered when calculating avergage ping time */
    public static final int AVG_PING_PACKETS = 0x7; // 8 (2^x for fast modulo)

    /** Parameter names in URI connectors */
    public static final String URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_1 = "Server-side Conversion Operation 1",
                               URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_1_PARAMETER = "Server-side Conversion Operation 1 Parameter",
                               URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_2 = "Server-side Conversion Operation 2",
                               URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_2_PARAMETER = "Server-side Conversion Operation 2 Parameter",
                               URI_CONNECTOR_SERVER_SIDE_CONVERSION_INTERMEDIATE_TYPE = "Server-side Conversion Intermediate Type",
                               URI_CONNECTOR_SERVER_SIDE_CONVERSION_DESTINATION_TYPE = "Server-side Conversion Destination Type",
                               URI_CONNECTOR_MINIMAL_UPDATE_INTERVAL = "Minimal Update Interval";

    /**
     * @param options Object in which to set named parameters for server-side conversions
     * @param serverConversion Object that contains conversions to perform. These are set in 'options' as named parameters
     * @param serializedType Type that is sent over the network
     */
    public static void setServerSideConversionOptions(RemoteConnectOptions options, RemoteConnectOptions serverConversion, String serializedType) {
        if (options.parameters != null) {
            options.parameters.remove(URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_1);
            options.parameters.remove(URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_1_PARAMETER);
            options.parameters.remove(URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_2);
            options.parameters.remove(URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_2_PARAMETER);
            options.parameters.remove(URI_CONNECTOR_SERVER_SIDE_CONVERSION_INTERMEDIATE_TYPE);
            options.parameters.remove(URI_CONNECTOR_SERVER_SIDE_CONVERSION_DESTINATION_TYPE);
        } else {
            options.parameters = new HashMap<>();
        }
        if (serverConversion.conversionRating == Definitions.TypeConversionRating.NO_CONVERSION) {
            return;
        }

        options.parameters.put(URI_CONNECTOR_SERVER_SIDE_CONVERSION_DESTINATION_TYPE, serializedType);
        if (serverConversion.operation1 != null) {
            options.parameters.put(URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_1, serverConversion.operation1.toString());
            ParameterDefinition parameter = options.operation1.getParameter();
            if (serverConversion.parameter1 != null && parameter != null && (!Serialization.equals(serverConversion.parameter1, parameter.getDefaultValue() != null ? parameter.getDefaultValue() : parameter.getType().getDefaultLocalDataType().createInstanceGeneric(null)))) {
                options.parameters.put(URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_1_PARAMETER, serverConversion.operation1.toString());
            }
        }
        if (serverConversion.operation2 != null) {
            options.parameters.put(URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_2, serverConversion.operation2.toString());
            ParameterDefinition parameter = options.operation2.getParameter();
            if (serverConversion.parameter2 != null && parameter != null && (!Serialization.equals(serverConversion.parameter2, parameter.getDefaultValue() != null ? parameter.getDefaultValue() : parameter.getType().getDefaultLocalDataType().createInstanceGeneric(null)))) {
                options.parameters.put(URI_CONNECTOR_SERVER_SIDE_CONVERSION_OPERATION_2_PARAMETER, serverConversion.operation2.toString());
            }

            options.parameters.put(URI_CONNECTOR_SERVER_SIDE_CONVERSION_INTERMEDIATE_TYPE, serverConversion.intermediateType.getName());
        }
    }
}
