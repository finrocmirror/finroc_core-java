/**
 * You received this file as part of Finroc
 * A Framework for intelligent robot control
 *
 * Copyright (C) Finroc GbR (finroc.org)
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
package org.finroc.core.port.rpc.internal;

import org.finroc.core.port.rpc.ClientPort;
import org.finroc.core.port.rpc.Method;
import org.finroc.core.port.rpc.RPCInterfaceType;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;


/**
 * @author Max Reichardt
 *
 * This class stores and handles RPC calls that do not return any value.
 * For calls within the same runtime environment this class is not required.
 * Objects of this class are used to temporarily store such calls in queues
 * for network threads and to serialize them.
 */
public class RPCMessage extends AbstractCall {

    public RPCMessage(Method method, Object[] arguments) {
        super.method = method;
        this.parameters = arguments;
        super.callType = CallType.RPC_MESSAGE;
    }

    /** Parameters of RPC call */
    Object[] parameters;


    public static void deserializeAndExecuteCallImplementation(InputStreamBuffer stream, RPCPort port, byte methodId) {
        try {
            RPCInterfaceType type = (RPCInterfaceType) port.getDataType();
            Method method = type.getMethod(methodId);
            Object[] parameters = new Object[method.getNativeMethod().getParameterTypes().length];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = Serialization.deserializeObject(stream, method.getNativeMethod().getParameterTypes()[i]);
            }
            ClientPort clientPort = ClientPort.wrap(port, true);
            clientPort.call(method, parameters);
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_DEBUG, "RPCMessage", "Incoming RPC message caused exception: ", e);
        }
    }

    @Override
    public void serialize(OutputStreamBuffer stream) {
        // Deserialized by network transport implementation
        stream.writeType(method.getInterfaceType());
        stream.writeByte(method.getMethodID());

        // Deserialized by this class
        for (int i = 0; i < parameters.length; i++) {
            Serialization.serializeObject(stream, parameters[i], method.getNativeMethod().getParameterTypes()[i]);
        }
    }
}
