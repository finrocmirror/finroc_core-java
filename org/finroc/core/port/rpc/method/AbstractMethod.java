/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2010 Max Reichardt,
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
package org.finroc.core.port.rpc.method;

import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCPortData;
import org.finroc.core.port.rpc.InterfaceNetPort;
import org.finroc.core.port.rpc.MethodCall;
import org.finroc.core.port.std.PortData;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.jc.log.LogUser;
import org.finroc.log.LogDomain;

/**
 * @author max
 *
 * This is the base class for all static method objects used in
 * remote procedure calls (RPCs).
 *
 * Subclasses are statically instantiated in InterfaceServerPort
 * for every method that server supports.
 *
 * These static instances contain all infos and provide all methods
 * for methods that server supports.
 */
@ForwardDecl( {PortInterface.class, InterfaceNetPort.class}) @Ptr
@Friend(PortInterface.class)
@Include("ParameterUtil.h")
@CppInclude("PortInterface.h")
public abstract class AbstractMethod extends LogUser {

    /** Method name */
    private String name;

    /** Parameter names */
    private String[] parameterNames = new String[4];

    /** Number of paramaters */
    private int parameterCount = 4;

    /** Name for unused parameters */
    protected static final String NO_PARAM = "NO_PARAMETER";

    /** Handle call in extra thread by default (should be true, if call can block or needs significant time to complete) */
    private final boolean handleInExtraThread;

    /** Id of method - set by PortInterface class */
    protected byte methodId;

    /** PortInterface to which this method belongs */
    protected PortInterface type;

    /** Log domain for this class */
    @InCpp("_CREATE_NAMED_LOGGING_DOMAIN(logDomain, \"rpc\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("rpc");

    public AbstractMethod(@Ref PortInterface portInterface, @Const @Ref String name, @Const @Ref String p1Name, @Const @Ref String p2Name, @Const @Ref String p3Name, @Const @Ref String p4Name, boolean handleInExtraThread) {
        this.name = name;
        parameterNames[0] = p1Name;
        parameterNames[1] = p2Name;
        parameterNames[2] = p3Name;
        parameterNames[3] = p4Name;
        for (int i = 0; i < 4; i++) {
            if (parameterNames[i].equals(NO_PARAM)) {
                parameterCount = i;
            }
        }
        this.handleInExtraThread = handleInExtraThread;
        portInterface.addMethod(this);
    }

    @JavaOnly
    public boolean hasLock(Object o) {
        if (o == null) {
            return true;
        } else if (o instanceof Number || o instanceof CCPortData || o instanceof CCInterThreadContainer<?>) {
            return true;
        } else if (o instanceof PortData) {
            return ((PortData)o).getManager().isLocked();
        }
        throw new RuntimeException("Unsupported data type");
    }

    @JavaOnly
    public void cleanup(Object o) {
        if (o == null) {
            return;
        }
        if (o instanceof PortData) {
            ((PortData)o).getManager().releaseLock();
        } else if (o instanceof CCInterThreadContainer<?>) {
            ((CCInterThreadContainer<?>)o).recycle2();
        }
    }

    /*Cpp
    template <typename T>
    bool hasLock(T t) {
        return ParameterUtil<T>::hasLock(t);
    }

    template <typename T>
    void cleanup(T t) {
        ParameterUtil<T>::cleanup(t);
    }
    */

//  /*Cpp
//  bool hasLock(const PortData* pd) {
//      if (pd == NULL) {
//          return true;
//      }
//      return pd->getManager()->isLocked();
//  }
//
//  bool hasLock(const CCPortData* cpd) {
//      if (cpd == NULL) {
//          return true;
//      }
//      return true;
//  }
//
//  inline bool hasLock(const int64 cpd) {
//      return true;
//  }
//
//  inline bool hasLock(const double cpd) {
//      return true;
//  }
//
//  inline bool hasLock(const int cpd) {
//      return true;
//  }
//
//  inline bool hasLock(const int16 cpd) {
//      return true;
//  }
//
//  inline bool hasLock(const int8 cpd) {
//      return true;
//  }
//
//  inline bool hasLock(const float cpd) {
//      return true;
//  }
//
//  template <typename T>
//  bool hasLock(const CCInterThreadContainer<T>* c) {
//      if (c == NULL) {
//          return true;
//      }
//      return true;
//  }
//
//  template <typename T>
//  inline bool hasLock(T t) {
//      return hasLock(static_cast<const T>(t));
//  }
//
//
//  void cleanup(PortData* pd) {
//      if (pd == NULL) {
//          return;
//      }
//      pd->getManager()->releaseLock();
//  }
//
//  void cleanup(const CCPortData* pd) {}
//  inline void cleanup(const int num) {}
//  inline void cleanup(const double num) {}
//  inline void cleanup(const int64 num) {}
//  inline void cleanup(const int16 num) {}
//  inline void cleanup(const int8 num) {}
//  inline void cleanup(const float num) {}
//
//  template <typename T>
//  void cleanup(const CCInterThreadContainer<T>* c) {
//      if (c == NULL) {
//          return;
//      }
//      c->recycle2();
//  }
//
//  template <typename T>
//  inline void cleanup(T t) {
//      cleanup(static_cast<const T>(t));
//  }
//  */

    public boolean handleInExtraThread() {
        return handleInExtraThread;
    }

    public int getMethodId() {
        return methodId;
    }

    /**
     * If we have a method call object (either from network or from another thread):
     * Execute the call
     *
     * @param call Method call object (must not be null and will be recycled - exception: method call is from net; return value will be stored in it)
     * @param handler Handler to handle object (must not be null)
     * @param retHandler Return handler (optional)
     */
    public abstract void executeFromMethodCallObject(MethodCall call, @Ptr AbstractMethodCallHandler handler, AbstractAsyncReturnHandler retHandler);

    /**
     * (only for async non-void calls)
     * If we have a method call object (prepared for sending) - actually send it over the net
     *
     * @param call Method call
     * @param netPort Network port to send it over
     * @param retHandler Return handler
     * @param netTimeout Timeout for call
     */
    public abstract void executeAsyncNonVoidCallOverTheNet(MethodCall call, InterfaceNetPort netPort, AbstractAsyncReturnHandler retHandler, int netTimeout);

    /**
     * @return Port interface that method belongs to
     */
    public PortInterface getPortInterface() {
        return type;
    }

    /**
     * @return Name of method
     */
    public String getName() {
        return name;
    }

    /**
     * @return Is this a void method?
     */
    public abstract boolean isVoidMethod();
}

/**
 * Method that does not return anything.
 * Such methods typically don't block.
 */
abstract class AbstractVoidMethod extends AbstractMethod {

    public AbstractVoidMethod(@Ref PortInterface portInterface, @Const @Ref String name, @Const @Ref String p1Name, @Const @Ref String p2Name, @Const @Ref String p3Name, @Const @Ref String p4Name, boolean handleInExtraThread) {
        super(portInterface, name, p1Name, p2Name, p3Name, p4Name, handleInExtraThread);
    }

    @Override
    public boolean isVoidMethod() {
        return true;
    }
}

/**
 * Method that returns something.
 * Can be called synchronous and asynchronous.
 *
 * Synchronous call will block current thread until return value is available.
 * Asynchronous call will call the provided ReturnHandler when return value is available (this
 * often starts method call in a separate thread, so be careful, not to send dozens at once over the network...)
 */
abstract class AbstractNonVoidMethod extends AbstractMethod {

    /** Default network timeout */
    protected static final int DEFAULT_NET_TIMEOUT = 2000;

    /** Default timeout for calls over the net */
    private final int defaultNetTimeout;

    public AbstractNonVoidMethod(@Ref PortInterface portInterface, @Const @Ref String name, @Const @Ref String p1Name, @Const @Ref String p2Name, @Const @Ref String p3Name, @Const @Ref String p4Name, boolean handleInExtraThread, int defaultNetTimeout) {
        super(portInterface, name, p1Name, p2Name, p3Name, p4Name, handleInExtraThread);
        this.defaultNetTimeout = defaultNetTimeout;
    }

    /**
     * @return Default timeout for calls over the net
     */
    public int getDefaultNetTimeout() {
        return defaultNetTimeout;
    }

    @Override
    public boolean isVoidMethod() {
        return false;
    }
}