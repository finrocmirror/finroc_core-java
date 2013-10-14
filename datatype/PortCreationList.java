//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core.datatype;

import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementFlags;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.rpc.ProxyPort;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.jc.container.SimpleList;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.RRLibSerializableImpl;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;
import org.rrlib.finroc_core_utils.xml.XMLNode;

/**
 * @author Max Reichardt
 *
 * List of ports to create.
 * Is only meant to be used in StaticParameters
 * For this reason, it is not real-time capable and a little more memory-efficient.
 */
public class PortCreationList extends RRLibSerializableImpl {

    /** Relevant flags for comparison */
    private static final int RELEVANT_FLAGS = FrameworkElementFlags.SHARED | FrameworkElementFlags.VOLATILE;

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<PortCreationList>(PortCreationList.class);

    /** Log domain for edges */
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("port_creation_list");

    /**
     * Entry in list
     */
    public static class Entry {

        /** Port name */
        public String name;

        /** Port type - as string (used remote) */
        public DataTypeReference type = new DataTypeReference();

        /** Output port? */
        public boolean outputPort;

        public Entry(String name, String type, boolean outputPort) {
            this.name = name;
            StringInputStream sis = new StringInputStream(type);
            this.type.deserialize(sis);
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
    public void serialize(OutputStreamBuffer os) {
        os.writeBoolean(showOutputPortSelection);
        if (ioVector == null) {
            int size = list.size();
            os.writeInt(size);
            for (int i = 0; i < size; i++) {
                Entry e = list.get(i);
                os.writeString(e.name);
                os.writeString(e.type.toString());
                os.writeBoolean(e.outputPort);
            }
        } else {
            synchronized (ioVector) {
                SimpleList<AbstractPort> ports = new SimpleList<AbstractPort>();
                getPorts(ioVector, ports);
                int size = ports.size();
                os.writeInt(size);
                for (int i = 0; i < size; i++) {
                    AbstractPort p = ports.get(i);
                    os.writeString(p.getName());
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
    private static void getPorts(FrameworkElement elem, SimpleList<AbstractPort> result) {
        result.clear();
        FrameworkElement.ChildIterator ci = new FrameworkElement.ChildIterator(elem, false);
        AbstractPort ap = null;
        while ((ap = ci.nextPort()) != null) {
            result.add(ap);
        }
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        if (ioVector == null) {
            showOutputPortSelection = is.readBoolean();
            int size = is.readInt();
            list.clear();
            for (int i = 0; i < size; i++) {
                list.add(new Entry(is.readString(), is.readString(), is.readBoolean()));
            }
        } else {
            synchronized (ioVector) {
                showOutputPortSelection = is.readBoolean();
                int size = is.readInt();
                SimpleList<AbstractPort> ports = new SimpleList<AbstractPort>();
                getPorts(ioVector, ports);
                for (int i = 0; i < size; i++) {
                    AbstractPort ap = i < ports.size() ? ports.get(i) : null;
                    String name = is.readString();
                    String dtName = is.readString();
                    DataTypeBase dt = DataTypeBase.findType(dtName);
                    if (dt == null) {
                        throw new RuntimeException("Type " + dtName + " not available");
                    }
                    boolean output = is.readBoolean();
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
     * @param output output port
     * @param prototype Port prototype (only interesting for listener)
     */
    private void checkPort(AbstractPort ap, FrameworkElement ioVector, int flags, String name, DataTypeBase dt, boolean output, AbstractPort prototype) {
        if (ap != null && ap.nameEquals(name) && ap.getDataType() == dt && (ap.getAllFlags() & RELEVANT_FLAGS) == (flags & RELEVANT_FLAGS)) {
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
            tmp = output ? FrameworkElementFlags.OUTPUT_PROXY : FrameworkElementFlags.INPUT_PROXY;
        }
        flags |= tmp;

        log(LogLevel.DEBUG_VERBOSE_1, logDomain, "Creating port " + name + " in IOVector " + ioVector.getQualifiedLink());
        if (FinrocTypeInfo.isStdType(dt)) {
            ap = new PortBase(new PortCreationInfo(name, ioVector, dt, flags));
        } else if (FinrocTypeInfo.isCCType(dt)) {
            ap = new CCPortBase(new PortCreationInfo(name, ioVector, dt, flags));
        } else if (FinrocTypeInfo.isMethodType(dt)) {
            ap = new ProxyPort(new PortCreationInfo(name, ioVector, dt, flags & FrameworkElementFlags.IS_OUTPUT_PORT)).getWrapped();
        } else {
            log(LogLevel.WARNING, logDomain, "Cannot create port with type: " + dt.getName());
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
            SimpleList<AbstractPort> ports1 = new SimpleList<AbstractPort>();
            getPorts(this.ioVector, ports1);
            SimpleList<AbstractPort> ports2 = new SimpleList<AbstractPort>();
            getPorts(ioVector, ports2);

            for (int i = 0; i < ports1.size(); i++) {
                AbstractPort ap1 = ports1.get(i);
                AbstractPort ap2 = i < ports2.size() ? ports2.get(i) : null;
                checkPort(ap2, ioVector, flags, ap1.getName(), ap1.getDataType(), ap1.isOutputPort(), ap1);
            }
            for (int i = ports1.size(); i < ports2.size(); i++) {
                ports2.get(i).managedDelete();
            }
        }
    }

    @Override
    public void serialize(XMLNode node) throws Exception {
        node.setAttribute("showOutputSelection", showOutputPortSelection);
        if (ioVector == null) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                XMLNode child = node.addChildNode("port");
                Entry e = list.get(i);
                child.setAttribute("name", e.name);
                child.setAttribute("type", e.type.toString());
                child.setAttribute("output", e.outputPort);
            }
        } else {
            synchronized (ioVector) {
                SimpleList<AbstractPort> ports = new SimpleList<AbstractPort>();
                getPorts(ioVector, ports);
                int size = ports.size();
                for (int i = 0; i < size; i++) {
                    AbstractPort p = ports.get(i);
                    XMLNode child = node.addChildNode("port");
                    child.setAttribute("name", p.getName());
                    child.setAttribute("type", p.getDataType().getName());
                    if (showOutputPortSelection) {
                        child.setAttribute("output", p.isOutputPort());
                    }
                }
            }
        }
    }

    @Override
    public void deserialize(XMLNode node) throws Exception {
        showOutputPortSelection = node.getBoolAttribute("showOutputSelection");
        if (ioVector == null) {
            for (XMLNode.ConstChildIterator port = node.getChildrenBegin(); port.get() != node.getChildrenEnd(); port.next()) {
                String portName = port.get().getName();
                assert(portName.equals("port"));
                list.add(new Entry(port.get().getStringAttribute("name"), port.get().getStringAttribute("type"), port.get().hasAttribute("output") ? port.get().getBoolAttribute("output") : false));
            }
        } else {
            assert(ioVector != null) : "Only available on local systems";
            synchronized (ioVector) {
                SimpleList<AbstractPort> ports = new SimpleList<AbstractPort>();
                getPorts(ioVector, ports);
                int i = 0;
                for (XMLNode.ConstChildIterator port = node.getChildrenBegin(); port.get() != node.getChildrenEnd(); port.next(), ++i) {
                    AbstractPort ap = i < ports.size() ? ports.get(i) : null;
                    String portName = port.get().getName();
                    assert(portName.equals("port"));
                    boolean b = false;
                    if (showOutputPortSelection) {
                        b = port.get().getBoolAttribute("output");
                    }
                    String dtName = port.get().getStringAttribute("type");
                    DataTypeBase dt = DataTypeBase.findType(dtName);
                    if (dt == null) {
                        throw new RuntimeException("Type " + dtName + " not available");
                    }
                    checkPort(ap, ioVector, flags, port.get().getStringAttribute("name"), dt, b, null);
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
     * @param output Output port? (possibly irrelevant)
     */
    public void add(String name, DataTypeBase dt, boolean output) {
        synchronized (ioVector) {
            checkPort(null, ioVector, flags, name, dt, output, null);
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
        Entry e = new Entry("Port" + list.size(), CoreNumber.TYPE.getName(), false);
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
        this.showOutputPortSelection = show;
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
}
