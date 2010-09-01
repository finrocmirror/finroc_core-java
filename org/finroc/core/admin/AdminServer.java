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
package org.finroc.core.admin;

import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.rpc.InterfaceServerPort;
import org.finroc.core.port.rpc.MethodCallException;
import org.finroc.core.port.rpc.method.AbstractMethod;
import org.finroc.core.port.rpc.method.AbstractMethodCallHandler;
import org.finroc.core.port.rpc.method.PortInterface;
import org.finroc.core.port.rpc.method.Void1Handler;
import org.finroc.core.port.rpc.method.Void1Method;
import org.finroc.core.port.rpc.method.Void2Handler;
import org.finroc.core.port.rpc.method.Void2Method;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Superclass;
import org.finroc.log.LogLevel;

/**
 * @author max
 *
 * Administration interface server port
 */
@Superclass( {InterfaceServerPort.class, AbstractMethodCallHandler.class})
public class AdminServer extends InterfaceServerPort implements Void2Handler<Integer, Integer>, Void1Handler<Integer> {

    /** Admin interface */
    @PassByValue public static PortInterface METHODS = new PortInterface("Admin Interface");

    /** Connect */
    @PassByValue public static Void2Method<AdminServer, Integer, Integer> CONNECT =
        new Void2Method<AdminServer, Integer, Integer>(METHODS, "Connect", "source port handle", "destination port handle", false);

    /** Disconnect */
    @PassByValue public static Void2Method<AdminServer, Integer, Integer> DISCONNECT =
        new Void2Method<AdminServer, Integer, Integer>(METHODS, "Disconnect", "source port handle", "destination port handle", false);

    /** Disconnect All */
    @PassByValue public static Void1Method<AdminServer, Integer> DISCONNECT_ALL =
        new Void1Method<AdminServer, Integer>(METHODS, "DisconnectAll", "source port handle", false);

    /** Data Type of method calls to this port */
    public static final DataType DATA_TYPE = DataTypeRegister.getInstance().addMethodDataType("Administration method calls", METHODS);

    /** Port name of admin interface */
    public static final String PORT_NAME = "Administration";

    /** Qualified port name */
    public static final String QUALIFIED_PORT_NAME = "Unrelated/Administration";

    public AdminServer() {
        super(PORT_NAME, null, DATA_TYPE, null, PortFlags.SHARED);
        setCallHandler(this);
    }

    @Override
    public void handleVoidCall(AbstractMethod method, Integer p1, Integer p2) throws MethodCallException {
        RuntimeEnvironment re = RuntimeEnvironment.getInstance();
        AbstractPort src = re.getPort(p1);
        AbstractPort dest = re.getPort(p2);
        if (src == null || dest == null) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Can't (dis)connect ports that do not exists");
            return;
        }
        if (method == CONNECT) {
            if (src.mayConnectTo(dest)) {
                src.connectToTarget(dest);
            } else if (dest.mayConnectTo(src)) {
                dest.connectToTarget(src);
            }
            if (!src.isConnectedTo(dest)) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Could not connect ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
            } else {
                logDomain.log(LogLevel.LL_USER, getLogDescription(), "Connected ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
            }
        } else if (method == DISCONNECT) {
            src.disconnectFrom(dest);
            if (src.isConnectedTo(dest)) {
                logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Could not disconnect ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
            } else {
                logDomain.log(LogLevel.LL_USER, getLogDescription(), "Disconnected ports " + src.getQualifiedName() + " " + dest.getQualifiedName());
            }
        }
    }

    @Override
    public void handleVoidCall(AbstractMethod method, Integer p1) throws MethodCallException {
        RuntimeEnvironment re = RuntimeEnvironment.getInstance();
        AbstractPort src = re.getPort(p1);
        if (src == null) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Can't disconnect port that doesn't exist");
            return;
        }
        src.disconnectAll();
        logDomain.log(LogLevel.LL_USER, getLogDescription(), "Disconnected port " + src.getQualifiedName());
    }
}