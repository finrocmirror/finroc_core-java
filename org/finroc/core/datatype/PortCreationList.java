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
package org.finroc.core.datatype;

import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.rpc.InterfacePort;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataImpl;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Struct;
import org.finroc.jc.container.SimpleList;
import org.finroc.log.LogLevel;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * List of ports to create.
 * Is only meant to be used in StructureParameters
 * For this reason, it is not real-time capable and a little more memory-efficient.
 */
public class PortCreationList extends PortDataImpl {

    /** Relevant flags for comparison */
    private static final int RELEVANT_FLAGS = CoreFlags.SHARED | PortFlags.IS_VOLATILE;

    /** Data Type */
    public static DataType TYPE = DataTypeRegister.getInstance().getDataType(PortCreationList.class);

    /**
     * Entry in list
     */
    @Struct @PassByValue @AtFront
    public static class Entry {

        /** Port name */
        public String name;

        /** Port type - as string (used remote) */
        public DataTypeReference type = new DataTypeReference();

        /** Output port? */
        public boolean outputPort;

        public Entry(@Const @Ref String name, @Const @Ref String type, boolean outputPort) {
            this.name = name;
            this.type.deserialize(type);
            this.outputPort = outputPort;
        }
    }

    /** Should output port selection be visible in finstruct? */
    private boolean showOutputPortSelection;

    /** List backend (for remote Runtimes) */
    private SimpleList<Entry> list = new SimpleList<Entry>();

    /** FrameworkElement that list is wrapping (for local Runtimes) */
    private FrameworkElement ioVector = null;

    /** Flags for port creation */
    private int flags = -1;

    /** (Local) change listener */
    private Listener listener;

    public PortCreationList() {}

    /**
     * Initially set up list for local operation
     *
     * @param managedIoVector FrameworkElement that list is wrapping
     * @param portCreationFlags Flags for port creation
     * @param showOutputPortSelection Should output port selection be visible in finstruct?
     */
    public void initialSetup(FrameworkElement managedIoVector, int portCreationFlags, boolean showOutputPortSelection) {
        assert((ioVector == null || ioVector == managedIoVector) && list.isEmpty());
        ioVector = managedIoVector;
        flags = portCreationFlags;
        this.showOutputPortSelection = showOutputPortSelection;
    }

    @Override
    public void serialize(CoreOutput os) {
        os.writeBoolean(showOutputPortSelection);
        if (ioVector == null) {
            int size = list.size();
            os.writeInt(size);
            for (int i = 0; i < size; i++) {
                @Const @Ref Entry e = list.get(i);
                os.writeString(e.name);
                os.writeString(e.type.toString());
                os.writeBoolean(e.outputPort);
            }
        } else {
            synchronized (ioVector) {
                @PassByValue SimpleList<AbstractPort> ports = new SimpleList<AbstractPort>();
                getPorts(ioVector, ports);
                int size = ports.size();
                os.writeInt(size);
                for (int i = 0; i < size; i++) {
                    AbstractPort p = ports.get(i);
                    os.writeString(p.getCDescription());
                    os.writeString(p.getDataType().getName());
                    os.writeBoolean(p.isOutputPort());
                }
            }
        }
    }

    /**
     * Returns all child ports of specified framework element
     *
     * @param elem Framework Element
     * @param result List containing result
     */
    private static void getPorts(@Const FrameworkElement elem, @Ref SimpleList<AbstractPort> result) {
        result.clear();
        FrameworkElement.ChildIterator ci = new FrameworkElement.ChildIterator(elem);
        AbstractPort ap = null;
        while ((ap = ci.nextPort()) != null) {
            result.add(ap);
        }
    }

    @Override
    public void deserialize(CoreInput is) {
        if (ioVector == null) {
            showOutputPortSelection = is.readBoolean();
            @SizeT int size = is.readInt();
            list.clear();
            for (@SizeT int i = 0; i < size; i++) {
                list.add(new Entry(is.readString(), is.readString(), is.readBoolean()));
            }
        } else {
            synchronized (ioVector) {
                showOutputPortSelection = is.readBoolean();
                @SizeT int size = is.readInt();
                @PassByValue SimpleList<AbstractPort> ports = new SimpleList<AbstractPort>();
                getPorts(ioVector, ports);
                for (@SizeT int i = 0; i < size; i++) {
                    AbstractPort ap = i < ports.size() ? ports.get(i) : null;
                    String name = is.readString();
                    DataType dt = DataTypeRegister.getInstance().getDataType(is.readString());
                    if (dt == null) {
                        throw new RuntimeException("Type " + dt + " not available");
                    }
                    boolean output = is.readBoolean();
                    checkPort(ap, ioVector, flags, name, dt, output, null);
                }
                for (@SizeT int i = size; i < ports.size(); i++) {
                    ports.get(i).managedDelete();
                }
            }
        }
    }

    /**
     * Check whether we need to make adjustments to port
     *
     * @param ap Port to check
     * @param ioVector Parent
     * @param flags Creation flags
     * @param name New name
     * @param dt new data type
     * @param output output port
     * @param prototype Port prototype (only interesting for listener)
     */
    private void checkPort(@Managed AbstractPort ap, FrameworkElement ioVector, int flags, @Const @Ref String name, DataType dt, boolean output, AbstractPort prototype) {
        if (ap != null && ap.descriptionEquals(name) && ap.getDataType() == dt && (ap.getAllFlags() & RELEVANT_FLAGS) == (flags & RELEVANT_FLAGS)) {
            if ((!showOutputPortSelection) || (output == ap.isOutputPort())) {
                return;
            }
        }
        if (ap != null) {
            ap.managedDelete();
        }

        // compute flags to use
        int tmp = 0;
        if (showOutputPortSelection) {
            tmp = output ? PortFlags.OUTPUT_PROXY : PortFlags.INPUT_PROXY;
        }
        flags |= tmp;

        log(LogLevel.LL_DEBUG_VERBOSE_1, logDomain, "Creating port " + name + " in IOVector " + ioVector.getQualifiedLink());
        if (dt.isStdType()) {
            ap = new PortBase(new PortCreationInfo(name, ioVector, dt, flags));
        } else if (dt.isCCType()) {
            ap = new CCPortBase(new PortCreationInfo(name, ioVector, dt, flags));
        } else if (dt.isMethodType()) {
            ap = new InterfacePort(name, ioVector, dt, InterfacePort.Type.Routing);
        } else {
            log(LogLevel.LL_WARNING, logDomain, "Cannot create port with type: " + dt.getName());
        }
        if (ap != null) {
            ap.init();
        }
        if (ap != null && listener != null) {
            listener.portCreated(ap, prototype);
        }
    }

    /**
     * Applies changes to another IO vector
     *
     * @param ioVector Other io vector
     * @param flags Flags to use for port creation
     */
    public void applyChanges(FrameworkElement ioVector, int flags) {
        synchronized (ioVector) {
            @PassByValue SimpleList<AbstractPort> ports1 = new SimpleList<AbstractPort>();
            getPorts(this.ioVector, ports1);
            @PassByValue SimpleList<AbstractPort> ports2 = new SimpleList<AbstractPort>();
            getPorts(ioVector, ports2);

            for (@SizeT int i = 0; i < ports1.size(); i++) {
                AbstractPort ap1 = ports1.get(i);
                AbstractPort ap2 = i < ports2.size() ? ports2.get(i) : null;
                checkPort(ap2, ioVector, flags, ap1.getDescription(), ap1.getDataType(), ap1.isOutputPort(), ap1);
            }
            for (@SizeT int i = ports1.size(); i < ports2.size(); i++) {
                ports2.get(i).managedDelete();
            }
        }
    }

    @Override
    public void serialize(XMLNode node) throws Exception {
        assert(ioVector != null) : "Only available on local systems";
        synchronized (ioVector) {
            node.setAttribute("showOutputSelection", showOutputPortSelection);
            @PassByValue SimpleList<AbstractPort> ports = new SimpleList<AbstractPort>();
            getPorts(ioVector, ports);
            int size = ports.size();
            for (int i = 0; i < size; i++) {
                AbstractPort p = ports.get(i);
                XMLNode n = node.addChildNode("port");
                n.setAttribute("name", p.getCDescription());
                n.setAttribute("type", p.getDataType().getName());
                if (showOutputPortSelection) {
                    n.setAttribute("output", p.isOutputPort());
                }
            }
        }

    }

    @Override
    public void deserialize(XMLNode node) throws Exception {
        assert(ioVector != null) : "Only available on local systems";
        synchronized (ioVector) {
            showOutputPortSelection = node.getBoolAttribute("showOutputSelection");
            @PassByValue SimpleList<XMLNode> children = new SimpleList<XMLNode>();
            children.addAll(node.getChildren());
            @PassByValue SimpleList<AbstractPort> ports = new SimpleList<AbstractPort>();
            getPorts(ioVector, ports);
            for (@SizeT int i = 0; i < children.size(); i++) {
                AbstractPort ap = i < ports.size() ? ports.get(i) : null;
                XMLNode port = children.get(i);
                String portName = port.getName();
                assert(portName.equals("port"));
                boolean b = false;
                if (showOutputPortSelection) {
                    b = port.getBoolAttribute("output");
                }
                DataType dt = DataTypeRegister.getInstance().getDataType(port.getStringAttribute("type"));
                if (dt == null) {
                    throw new RuntimeException("Type " + dt + " not available");
                }
                checkPort(ap, ioVector, flags, port.getStringAttribute("name"), dt, b, null);
            }
            for (@SizeT int i = children.size(); i < ports.size(); i++) {
                ports.get(i).managedDelete();
            }
        }
    }

    /**
     * Add entry to list
     *
     * @param name Port name
     * @param dt Data type
     * @param output Output port? (possibly irrelevant)
     */
    public void add(@Const @Ref String name, DataType dt, boolean output) {
        synchronized (ioVector) {
            checkPort(null, ioVector, flags, name, dt, output, null);
        }
    }

    /**
     * @return (Local) change listener
     */
    @ConstMethod public Listener getListener() {
        return listener;
    }

    /**
     * @param listener (Local) change listener
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * @return size of list
     */
    @ConstMethod public int getSize() {
        return ioVector == null ? list.size() : ioVector.childCount();
    }

    /**
     * (Remote lists only)
     *
     * @param index Index
     * @return List entry
     */
    @JavaOnly
    public Entry getEntry(int index) {
        return list.get(index);
    }

    /**
     * (Remote lists only)
     *
     * @param index Index
     * @return List entry
     */
    @JavaOnly
    public Entry addElement() {
        Entry e = new Entry("Port" + list.size(), CoreNumber.TYPE.getName(), false);
        list.add(e);
        return e;
    }

    /**
     * (Remote lists only)
     *
     * @param index Index of List entry to remove
     */
    @JavaOnly
    public void removeElement(int index) {
        list.remove(index);
    }

    /**
     * Callback interface for changes to ports
     */
    @Ptr @AtFront
    public interface Listener {

        /**
         * Called whenever a port was created
         *
         * @param ap Created port
         * @param prototype Prototype after which port was created
         */
        public void portCreated(AbstractPort ap, AbstractPort prototype);
    }
}
