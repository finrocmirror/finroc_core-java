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
package org.finroc.core.datatype;

import java.util.ArrayList;

import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.rpc.ProxyPort;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.BinaryOutputStream;
import org.rrlib.serialization.BinarySerializable;
import org.rrlib.serialization.StringInputStream;
import org.rrlib.serialization.XMLSerializable;
import org.rrlib.serialization.rtti.Copyable;
import org.rrlib.serialization.rtti.DataType;
import org.rrlib.serialization.rtti.DataTypeBase;
import org.rrlib.xml.XMLNode;

/**
 * @author Max Reichardt
 *
 * List of ports to create.
 * Is only meant to be used in StaticParameters
 * For this reason, it is not real-time capable and a little more memory-efficient.
 */
public class PortCreationList implements BinarySerializable, XMLSerializable, Copyable<PortCreationList> {

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<PortCreationList>(PortCreationList.class);

    /**
     * Entry in list
     */
    public static class Entry {

        /** Port name */
        public String name;

        /** Port type - as string (used remote) */
        public DataTypeReference type = new DataTypeReference();

        /** Port creation options for this specific port (e.g. output port? shared port?) */
        public byte createOptions;

        public Entry(String name, String type, byte createOptions) {
            this.name = name;
            StringInputStream sis = new StringInputStream(type);
            this.type.deserialize(sis);
            this.createOptions = createOptions;
        }

        public Entry(String name, DataTypeReference type, byte createOptions) {
            this.name = name;
            this.type = new DataTypeReference(type);
            this.createOptions = createOptions;
        }
    }

    /** Flags in create options */
    public final static byte CREATE_OPTION_OUTPUT = 1, CREATE_OPTION_SHARED = 2, CREATE_OPTION_ALL = CREATE_OPTION_OUTPUT | CREATE_OPTION_SHARED;

    /**
     * Which creation options should be visible and selectable in finstruct?
     * (the user can e.g. select whether port is input or output port - or shared)
     */
    private byte selectableCreateOptions;

    /** List backend (for remote Runtimes) */
    private ArrayList<Entry> list = new ArrayList<Entry>();

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
     * @param selectableCreateOptions Which creation options should be visible and selectable in finstruct?
     */
    public void initialSetup(FrameworkElement managedIoVector, int portCreationFlags, byte selectableCreateOptions) {
        assert((ioVector == null || ioVector == managedIoVector) && list.isEmpty());
        ioVector = managedIoVector;
        flags = portCreationFlags;
        this.selectableCreateOptions = selectableCreateOptions;
    }

    @Override
    public void serialize(BinaryOutputStream os) {
        os.writeByte(selectableCreateOptions);
        if (ioVector == null) {
            int size = list.size();
            os.writeInt(size);
            for (int i = 0; i < size; i++) {
                Entry e = list.get(i);
                os.writeString(e.name);
                os.writeString(e.type.toString());
                os.writeByte(e.createOptions);
            }
        } else {
            synchronized (ioVector) {
                ArrayList<AbstractPort> ports = new ArrayList<AbstractPort>();
                getPorts(ioVector, ports);
                int size = ports.size();
                os.writeInt(size);
                for (int i = 0; i < size; i++) {
                    AbstractPort p = ports.get(i);
                    os.writeString(p.getName());
                    os.writeString(p.getDataType().getName());
                    os.writeByte(toPortCreateOptions(p.getAllFlags(), selectableCreateOptions));
                }
            }
        }
    }


    private static byte toPortCreateOptions(int flags, byte selectableCreateOptions) {
        byte result = ((selectableCreateOptions & CREATE_OPTION_SHARED) != 0) && ((flags & FrameworkElementFlags.SHARED) > 0) ? CREATE_OPTION_SHARED : 0;
        result |= ((selectableCreateOptions & CREATE_OPTION_OUTPUT) != 0) && ((flags & FrameworkElementFlags.IS_OUTPUT_PORT) > 0) ? CREATE_OPTION_OUTPUT : 0;
        return result;
    }

    /**
     * Returns all child ports of specified framework element
     *
     * @param elem Framework Element
     * @param result List containing result
     */
    private static void getPorts(FrameworkElement elem, ArrayList<AbstractPort> result) {
        result.clear();
        FrameworkElement.ChildIterator ci = new FrameworkElement.ChildIterator(elem, false);
        AbstractPort ap = null;
        while ((ap = ci.nextPort()) != null) {
            result.add(ap);
        }
    }

    @Override
    public void deserialize(BinaryInputStream is) {
        if (ioVector == null) {
            selectableCreateOptions = is.readByte();
            int size = is.readInt();
            list.clear();
            for (int i = 0; i < size; i++) {
                list.add(new Entry(is.readString(), is.readString(), is.readByte()));
            }
        } else {
            synchronized (ioVector) {
                is.readByte(); // skip selectable create options, as this is not defined by finstruct
                int size = is.readInt();
                ArrayList<AbstractPort> ports = new ArrayList<AbstractPort>();
                getPorts(ioVector, ports);
                for (int i = 0; i < size; i++) {
                    AbstractPort ap = i < ports.size() ? ports.get(i) : null;
                    String name = is.readString();
                    String dtName = is.readString();
                    DataTypeBase dt = DataTypeBase.findType(dtName);
                    if (dt == null) {
                        throw new RuntimeException("Type " + dtName + " not available");
                    }
                    byte output = is.readByte();
                    checkPort(ap, ioVector, flags, name, dt, output, null);
                }
                for (int i = size; i < ports.size(); i++) {
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
     * @param createOptions Selected create options for port
     * @param prototype Port prototype (only interesting for listener)
     */
    private void checkPort(AbstractPort ap, FrameworkElement ioVector, int flags, String name, DataTypeBase dt, byte createOptions, AbstractPort prototype) {
        if (ap != null && ap.nameEquals(name) && ap.getDataType() == dt &&
                ap.getFlag(FrameworkElementFlags.VOLATILE) == (flags & FrameworkElementFlags.VOLATILE) > 0) {
            boolean createOutputPort = ((createOptions & CREATE_OPTION_OUTPUT) != 0) || ((flags & FrameworkElementFlags.IS_OUTPUT_PORT) != 0);
            boolean createSharedPort = ((createOptions & CREATE_OPTION_SHARED) != 0) || ((flags & FrameworkElementFlags.SHARED) != 0);
            if (((selectableCreateOptions & CREATE_OPTION_OUTPUT) != 0 || (ap.getFlag(FrameworkElementFlags.IS_OUTPUT_PORT) == createOutputPort)) &&
                    ((selectableCreateOptions & CREATE_OPTION_SHARED) != 0 || (ap.getFlag(FrameworkElementFlags.SHARED) == createSharedPort))) {
                return; // port is as it should be
            }
        }
        if (ap != null) {
            ap.managedDelete();
        }

        // compute flags to use
        flags |= FrameworkElementFlags.ACCEPTS_DATA | FrameworkElementFlags.EMITS_DATA; // proxy port
        flags |= toFlags(createOptions, selectableCreateOptions);

        Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Creating port " + name + " in IOVector " + ioVector.getQualifiedLink());
        if (FinrocTypeInfo.isStdType(dt)) {
            ap = new PortBase(new PortCreationInfo(name, ioVector, dt, flags));
        } else if (FinrocTypeInfo.isCCType(dt)) {
            ap = new CCPortBase(new PortCreationInfo(name, ioVector, dt, flags));
        } else if (FinrocTypeInfo.isMethodType(dt)) {
            ap = new ProxyPort(new PortCreationInfo(name, ioVector, dt, flags & FrameworkElementFlags.IS_OUTPUT_PORT)).getWrapped();
        } else {
            Log.log(LogLevel.WARNING, this, "Cannot create port with type: " + dt.getName());
        }
        if (ap != null) {
            ap.init();
        }
        if (ap != null && listener != null) {
            listener.portCreated(ap, prototype);
        }
    }

    private static int toFlags(byte createOptions, byte selectableCreateOptions) {
        int result = 0;
        if ((selectableCreateOptions & CREATE_OPTION_SHARED) != 0 && (createOptions & CREATE_OPTION_SHARED) != 0) {
            result |= FrameworkElementFlags.SHARED;
        }
        if ((selectableCreateOptions & CREATE_OPTION_OUTPUT) != 0 && (createOptions & CREATE_OPTION_OUTPUT) != 0) {
            result |= FrameworkElementFlags.IS_OUTPUT_PORT;
        }
        return result;
    }

    /**
     * Applies changes to another IO vector
     *
     * @param ioVector Other io vector
     * @param flags Flags to use for port creation
     */
    public void applyChanges(FrameworkElement ioVector, int flags) {
        synchronized (ioVector) {
            ArrayList<AbstractPort> ports1 = new ArrayList<AbstractPort>();
            getPorts(this.ioVector, ports1);
            ArrayList<AbstractPort> ports2 = new ArrayList<AbstractPort>();
            getPorts(ioVector, ports2);

            for (int i = 0; i < ports1.size(); i++) {
                AbstractPort ap1 = ports1.get(i);
                AbstractPort ap2 = i < ports2.size() ? ports2.get(i) : null;
                checkPort(ap2, ioVector, flags, ap1.getName(), ap1.getDataType(), toPortCreateOptions(ap1.getAllFlags(), selectableCreateOptions), ap1);
            }
            for (int i = ports1.size(); i < ports2.size(); i++) {
                ports2.get(i).managedDelete();
            }
        }
    }

    @Override
    public void serialize(XMLNode node) throws Exception {
        node.setAttribute("showOutputSelection", (selectableCreateOptions & CREATE_OPTION_OUTPUT) != 0);
        if (ioVector == null) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                XMLNode child = node.addChildNode("port");
                Entry e = list.get(i);
                child.setAttribute("name", e.name);
                child.setAttribute("type", e.type.toString());
                if ((selectableCreateOptions & CREATE_OPTION_OUTPUT) != 0) {
                    child.setAttribute("output", (e.createOptions & CREATE_OPTION_OUTPUT) != 0);
                }
            }
        } else {
            synchronized (ioVector) {
                ArrayList<AbstractPort> ports = new ArrayList<AbstractPort>();
                getPorts(ioVector, ports);
                int size = ports.size();
                for (int i = 0; i < size; i++) {
                    AbstractPort p = ports.get(i);
                    XMLNode child = node.addChildNode("port");
                    child.setAttribute("name", p.getName());
                    child.setAttribute("type", p.getDataType().getName());
                    if ((selectableCreateOptions & CREATE_OPTION_OUTPUT) != 0) {
                        child.setAttribute("output", p.isOutputPort());
                    }
                    if ((selectableCreateOptions & CREATE_OPTION_SHARED) != 0 && p.getFlag(FrameworkElementFlags.SHARED)) {
                        child.setAttribute("output", true);
                    }
                }
            }
        }
    }

    @Override
    public void deserialize(XMLNode node) throws Exception {
        selectableCreateOptions = node.hasAttribute("showOutputSelection") && node.getBoolAttribute("showOutputSelection") ? CREATE_OPTION_OUTPUT : 0;
        if (ioVector == null) {
            for (XMLNode port : node.children()) {
                String portName = port.getName();
                assert(portName.equals("port"));
                byte createOptions = 0;
                if ((selectableCreateOptions & CREATE_OPTION_OUTPUT) != 0 && port.hasAttribute("output") && port.getBoolAttribute("output")) {
                    createOptions |= CREATE_OPTION_OUTPUT;
                }
                if ((selectableCreateOptions & CREATE_OPTION_SHARED) != 0 && port.hasAttribute("shared") && port.getBoolAttribute("shared")) {
                    createOptions |= CREATE_OPTION_SHARED;
                }
                list.add(new Entry(port.getStringAttribute("name"), port.getStringAttribute("type"), createOptions));
            }
        } else {
            assert(ioVector != null) : "Only available on local systems";
            synchronized (ioVector) {
                ArrayList<AbstractPort> ports = new ArrayList<AbstractPort>();
                getPorts(ioVector, ports);
                int i = 0;
                for (XMLNode port : node.children()) {
                    AbstractPort ap = i < ports.size() ? ports.get(i) : null;
                    String portName = port.getName();
                    assert(portName.equals("port"));
                    byte createOptions = 0;
                    if ((selectableCreateOptions & CREATE_OPTION_OUTPUT) != 0 && port.hasAttribute("output") && port.getBoolAttribute("output")) {
                        createOptions |= CREATE_OPTION_OUTPUT;
                    }
                    if ((selectableCreateOptions & CREATE_OPTION_SHARED) != 0 && port.hasAttribute("shared") && port.getBoolAttribute("shared")) {
                        createOptions |= CREATE_OPTION_SHARED;
                    }
                    String dtName = port.getStringAttribute("type");
                    DataTypeBase dt = DataTypeBase.findType(dtName);
                    if (dt == null) {
                        throw new RuntimeException("Type " + dtName + " not available");
                    }
                    checkPort(ap, ioVector, flags, port.getStringAttribute("name"), dt, createOptions, null);
                }
                for (; i < ports.size(); i++) {
                    ports.get(i).managedDelete();
                }
            }
        }
    }

    /**
     * Add entry to list
     *
     * @param name Port name
     * @param dt Data type
     * @param createOptions Create options for this port
     */
    public void add(String name, DataTypeBase dt, byte createOptions) {
        synchronized (ioVector) {
            checkPort(null, ioVector, flags, name, dt, createOptions, null);
        }
    }

    /**
     * @return (Local) change listener
     */
    public Listener getListener() {
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
    public int getSize() {
        return ioVector == null ? list.size() : ioVector.childCount();
    }

    /**
     * (Remote lists only)
     *
     * @param index Index
     * @return List entry
     */
    public Entry getEntry(int index) {
        return list.get(index);
    }

    /**
     * (Remote lists only)
     *
     * @param index Index
     * @return List entry
     */
    public Entry addElement() {
        Entry e = new Entry("Port" + list.size(), CoreNumber.TYPE.getName(), (byte)0);
        list.add(e);
        return e;
    }

    /**
     * (Remote lists only)
     *
     * @param index Index of List entry to remove
     */
    public void removeElement(int index) {
        list.remove(index);
    }

    /**
     * @param show Should output port selection be visible in finstruct?
     */
    public void setShowOutputPortSelection(boolean show) {
        if (show) {
            selectableCreateOptions |= CREATE_OPTION_OUTPUT;
        } else {
            selectableCreateOptions &= ~CREATE_OPTION_OUTPUT;
        }
    }

    /**
     * @return Which creation options should be visible and selectable in finstruct?
     */
    public byte getSelectableCreateOptions() {
        return selectableCreateOptions;
    }

    /**
     * @param selectableCreateOptions Which creation options should be visible and selectable in finstruct?
     */
    public void setSelectableCreateOptions(byte selectableCreateOptions) {
        this.selectableCreateOptions = selectableCreateOptions;
    }

    /**
     * Callback interface for changes to ports
     */
    public interface Listener {

        /**
         * Called whenever a port was created
         *
         * @param ap Created port
         * @param prototype Prototype after which port was created
         */
        public void portCreated(AbstractPort ap, AbstractPort prototype);
    }

    @Override
    public void copyFrom(PortCreationList source) {
        flags = source.flags;
        ioVector = source.ioVector;
        list.clear();
        for (int i = 0; i < source.list.size(); i++) {
            Entry e = source.list.get(i);
            list.add(new Entry(e.name, e.type, e.createOptions));
        }
        selectableCreateOptions = source.selectableCreateOptions;
    }
}
