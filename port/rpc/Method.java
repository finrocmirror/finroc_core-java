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
package org.finroc.core.port.rpc;

import org.finroc.core.port.rpc.annotation.FutureType;
import org.finroc.core.port.rpc.annotation.PromiseType;


/**
 * @author Max Reichardt
 *
 * Method that can be called via RPC ports.
 */
public class Method {

    /** Wrapped native method */
    private java.lang.reflect.Method wrappedMethod;

    /** Method index in RPC interface type (set when added to RPC interface type) */
    byte methodID = -1;

    /** Interface type that method belong to (set when added to RPC interface type) */
    RPCInterfaceType interfaceType;

    /**
     * Future and promise type - should method return future and/or promise.
     * In this case, method must have been annotated
     */
    private final Class<?> futureType, promiseType;


    /**
     * @param interfaceClass Class with methods to be called by RPC
     * @param methodName Name of method in specified interface
     */
    public Method(Class<?> interfaceClass, String methodName) {
        java.lang.reflect.Method found = null;
        for (java.lang.reflect.Method method : interfaceClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                found = method;
            }
        }
        if (found == null) {
            throw new RuntimeException("Method " + methodName + " not found");
        }
        this.wrappedMethod = found;

        FutureType futureTypeAnnotation = wrappedMethod.getAnnotation(FutureType.class);
        futureType = futureTypeAnnotation != null ? futureTypeAnnotation.value() : null;
        PromiseType promiseTypeAnnotation = wrappedMethod.getAnnotation(PromiseType.class);
        promiseType = promiseTypeAnnotation != null ? promiseTypeAnnotation.value() : null;
    }

    /**
     * @return Wrapped native method
     */
    public java.lang.reflect.Method getNativeMethod() {
        return wrappedMethod;
    }

    /**
     * @return Method index in RPC interface type
     */
    public byte getMethodID() {
        return methodID;
    }

    /**
     * @return Interface type that method belong to
     */
    public RPCInterfaceType getInterfaceType() {
        return interfaceType;
    }

    /**
     * @return Does method return a future?
     */
    public boolean hasFutureReturn() {
        return (Future.class.isAssignableFrom(wrappedMethod.getReturnType()));
    }

    /**
     * @return Type of future if method returns future - NULL otherwise
     */
    public Class<?> getFutureType() {
        return futureType;
    }

    /**
     * @return Type of promise if method returns promise - NULL otherwise
     */
    public Class<?> getPromiseType() {
        return promiseType;
    }

}
