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
package org.finroc.core.port.rpc;

import org.finroc.core.FrameworkElement;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.MultiTypePortDataBufferPool;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.CppDefault;
import org.rrlib.finroc_core_utils.jc.annotation.CustomPtr;
import org.rrlib.finroc_core_utils.jc.annotation.Friend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.SizeT;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;

/**
 * @author max
 *
 * This is a port that can be used for remote procedure calls -
 * synchronous and asynchronous
 *
 * Server is source port.
 * Client is target port.
 * One source may have multiple targets. However, a target may only
 * have one source in order to receive only one return value.
 */
@Friend(InterfaceClientPort.class)
public class InterfacePort extends AbstractPort {

    /** Edges emerging from this port */
    protected final EdgeList<InterfacePort> edgesSrc = new EdgeList<InterfacePort>();

    /** Edges ending at this port - maximum of one in this class */
    protected final EdgeList<InterfacePort> edgesDest = new EdgeList<InterfacePort>();

    /** Type of interface port (Server interface implementation, client interface, network port, simple call forwarding) */
    public static enum Type { Server, Client, Network, Routing }

    /** Type of interface port */
    private final Type type;

    /** Pool with diverse data buffers */
    final @Ptr MultiTypePortDataBufferPool bufPool;

    public InterfacePort(String description, FrameworkElement parent, @Const @Ref DataTypeBase dataType, Type type) {
        this(new PortCreationInfo(description, parent, dataType, 0), type, -1);
    }

    public InterfacePort(String description, FrameworkElement parent, @Const @Ref DataTypeBase dataType, Type type, int customFlags) {
        this(new PortCreationInfo(description, parent, dataType, customFlags), type, -1);
    }

    public InterfacePort(String description, FrameworkElement parent, @Const @Ref DataTypeBase dataType, Type type, int customFlags, int lockLevel) {
        this(new PortCreationInfo(description, parent, dataType, customFlags), type, lockLevel);
    }

    public InterfacePort(PortCreationInfo pci, Type type, int lockLevel) {
        super(processPci(pci, type, lockLevel));
        initLists(edgesSrc, edgesDest);
        bufPool = (type == Type.Routing) ? null : new MultiTypePortDataBufferPool();
        //deferred = (type == Type.Routing) ? null : new WonderQueueTL<MethodCall>();
        this.type = type;
    }

    /** makes adjustment to flags passed through constructor */
    private static PortCreationInfo processPci(PortCreationInfo pci, Type type, int lockLevel) {
        switch (type) {
        case Server:
            pci.flags |= PortFlags.EMITS_DATA | PortFlags.OUTPUT_PORT;
            break;
        case Client:
            pci.flags |= PortFlags.ACCEPTS_DATA;
            break;
        case Network:
        case Routing:
            pci.flags |= PortFlags.EMITS_DATA | PortFlags.ACCEPTS_DATA;
            break;
        }
        if (lockLevel >= 0) {
            pci.lockOrder = lockLevel;
        }
        return pci;
    }

    @Override
    protected void rawConnectToTarget(AbstractPort target, boolean finstructed) {
        InterfacePort target2 = (InterfacePort)target;

        // disconnect old port(s) - should always be max. one - however, defensive implementation
        while (target2.edgesDest.size() > 0) { // disconnect old port
            target2.edgesDest.getIterable().get(0).disconnectFrom(target2);
        }

        super.rawConnectToTarget(target, finstructed);
    }

    @Override
    public void delete() {
        /*Cpp
        if (bufPool != NULL) {
            delete bufPool;
        }
         */
    }

    /**
     * (for non-cc types only)
     * @param dt Data type of object to get buffer of
     * @return Unused buffer of type
     */
    @Override
    public PortDataManager getUnusedBufferRaw(@Const @Ref DataTypeBase dt) {
        assert(!FinrocTypeInfo.isCCType(dt));
        assert(bufPool != null);
        return bufPool.getUnusedBuffer(dt);
    }

    /**
     * (for cc types only)
     * @param dt Data type of object to get buffer of
     * @return Unused buffer of type
     */
    public CCPortDataManager getUnusedCCBuffer(@Const @Ref DataTypeBase dt) {
        assert(FinrocTypeInfo.isCCType(dt));
        return ThreadLocalCache.get().getUnusedInterThreadBuffer(dt);
    }

    @Override
    public void notifyDisconnect() {
        /* don't do anything here... only in network ports */
    }

    @Override
    public void setMaxQueueLength(int length) {
        throw new RuntimeException("InterfacePorts do not have a queue");
    }

    @Override
    protected short getStrategyRequirement() {
        return 0;
    }

    /**
     * @return type of interface port
     */
    public Type getType() {
        return type;
    }

    /**
     * (Usually called on client ports)
     *
     * @return "Server" Port that handles method call - either InterfaceServerPort or InterfaceNetPort (the latter if we have remote server)
     */
    public InterfacePort getServer() {
        @Ptr InterfacePort current = this;
        while (true) {
            @Ptr InterfacePort last = current;
            @Ptr ArrayWrapper<InterfacePort> it = current.edgesDest.getIterable();
            for (@SizeT int i = 0, n = it.size(); i < n; i++) {
                InterfacePort ip = (InterfacePort)it.get(i);
                if (ip != null) {
                    current = ip;
                    break;
                }
            }

            if (current == null || current == last) {
                return null;
            }

            if (current.getType() == Type.Server || current.getType() == Type.Network) {
                return current;
            }
        }
    }

    @Override
    protected void initialPushTo(AbstractPort target, boolean reverse) {
        // do nothing in interface port
    }

    @Override
    protected void clearQueueImpl() {
        // do nothing in interface port
    }

    @Override
    protected int getMaxQueueLengthImpl() {
        // do nothing in interface port
        return 0;
    }

    @Override
    protected void setMaxQueueLengthImpl(int length) {
        // do nothing in interface port
    }

    @Override
    public void forwardData(AbstractPort other) {
        // do nothing in interface port
    }

    /**
     * Get buffer to use in method call or return (has one lock)
     *
     * (for non-cc types only)
     * @param dt Data type of object to get buffer of
     * @return Unused buffer of type
     */
    @SuppressWarnings("unchecked")
    @InCpp( {"PortDataManager* mgr = getUnusedBufferRaw(dt != NULL ? dt : rrlib::serialization::DataType<T>());",
             "mgr->getCurrentRefCounter()->setOrAddLocks((int8_t)1);",
             "return PortDataPtr<T>(mgr);"
            })
    protected @CustomPtr("tPortDataPtr") <T> T getBufferForCall(@CppDefault("NULL") @Const @Ref DataTypeBase dt) {
        PortDataManager pdm = getUnusedBufferRaw(dt);
        T t = (T)pdm.getObject().getData();
        pdm.getCurrentRefCounter().setOrAddLocks((byte)1);
        return t;
    }
}
