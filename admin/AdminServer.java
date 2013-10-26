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
package org.finroc.core.admin;

import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.rpc.Method;
import org.finroc.core.port.rpc.RPCInterfaceType;
import org.finroc.core.port.rpc.ServerPort;

/**
 * @author Max Reichardt
 *
 * Administration interface server port
 */
public class AdminServer extends ServerPort<AdministrationService> {

    /** Methods in administration service */
    public static Method
    CONNECT = new Method(AdministrationService.class, "connect"),
    CREATE_MODULE = new Method(AdministrationService.class, "createModule"),
    DELETE_ELEMENT = new Method(AdministrationService.class, "deleteElement"),
    DISCONNECT = new Method(AdministrationService.class, "disconnect"),
    DISCONNECT_ALL = new Method(AdministrationService.class, "disconnectAll"),
    GET_ANNOTATION = new Method(AdministrationService.class, "getAnnotation"),
    GET_CREATE_MODULE_ACTIONS = new Method(AdministrationService.class, "getCreateModuleActions"),
    GET_MODULE_LIBRARIES = new Method(AdministrationService.class, "getModuleLibraries"),
    GET_PARAMETER_INFO = new Method(AdministrationService.class, "getParameterInfo"),
    IS_EXECUTING = new Method(AdministrationService.class, "isExecuting"),
    LOAD_MODULE_LIBRARY = new Method(AdministrationService.class, "loadModuleLibrary"),
    PAUSE_EXECUTION = new Method(AdministrationService.class, "pauseExecution"),
    SAVE_ALL_FINSTRUCTABLE_FILES = new Method(AdministrationService.class, "saveAllFinstructableFiles"),
    SAVE_FINSTRUCTABLE_GROUP = new Method(AdministrationService.class, "saveFinstructableGroup"),
    SET_ANNOTATION = new Method(AdministrationService.class, "setAnnotation"),
    SET_PORT_VALUE = new Method(AdministrationService.class, "setPortValue"),
    START_EXECUTION = new Method(AdministrationService.class, "startExecution");

    /** Data Type of method calls to this port */
    public static final RPCInterfaceType DATA_TYPE = new RPCInterfaceType("Administration Interface", CONNECT, CREATE_MODULE, DELETE_ELEMENT,
            DISCONNECT, DISCONNECT_ALL, GET_ANNOTATION, GET_CREATE_MODULE_ACTIONS, GET_MODULE_LIBRARIES, GET_PARAMETER_INFO, IS_EXECUTING,
            LOAD_MODULE_LIBRARY, PAUSE_EXECUTION, SAVE_ALL_FINSTRUCTABLE_FILES, SAVE_FINSTRUCTABLE_GROUP, SET_ANNOTATION, SET_PORT_VALUE, START_EXECUTION);

    public AdminServer() {
        super(new AdministrationService(), new PortCreationInfo(AdministrationService.PORT_NAME, AdminServer.DATA_TYPE, 0));
    }
}
