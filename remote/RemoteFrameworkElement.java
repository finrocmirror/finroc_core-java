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
package org.finroc.core.remote;

import java.util.ArrayList;
import java.util.List;

import org.finroc.core.Annotatable;
import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElementFlags;
import org.rrlib.serialization.rtti.DataTypeBase;


/**
 * @author Max Reichardt
 *
 * This class contains information about remote framework element
 */
public class RemoteFrameworkElement extends ModelNode {

    /** Handle of remote element */
    private final int remoteHandle;

    /** Framework element tags */
    private ArrayList<String> tags;

    /** Flags of this remote framework element */
    private int flags;

    /** Manages annotations for this element */
    private Annotatable annotationManager = new Annotatable();

    /** Has element been checked for editable interfaces? */
    private boolean editableInterfacesChecked = false;

    /** List of editable interfaces of this element */
    private ArrayList<RemoteFrameworkElement> editableInterfaces;


    /**
     * @param remoteHandle Handle of remote framework element handle
     * @param name Name of remote framework element (may be changed later)
     */
    public RemoteFrameworkElement(int remoteHandle, String name) {
        super(name);
        this.remoteHandle = remoteHandle;
    }

    /**
     * @return Handle of remote element
     */
    public int getRemoteHandle() {
        return remoteHandle;
    }

    /**
     * @return Flags of this remote framework element
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @return Tags of this element (may be null)
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * @param flags Flags of this remote framework element
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * @param tags Tags of this element (may be null)
     */
    public void setTags(List<String> tags) {
        this.tags = new ArrayList<String>();
        this.tags.addAll(tags);
    }

    /**
     * @see org.finroc.core.Annotatable#addAnnotation(org.finroc.core.FinrocAnnotation)
     */
    public void addAnnotation(FinrocAnnotation ann) {
        annotationManager.addAnnotation(ann);
    }

    /**
     * @see org.finroc.core.Annotatable#getAnnotation(org.rrlib.serialization.rtti.DataTypeBase)
     */
    public FinrocAnnotation getAnnotation(DataTypeBase dt) {
        return annotationManager.getAnnotation(dt);
    }

    /**
     * @see org.finroc.core.Annotatable#getAnnotation(java.lang.Class)
     */
    public <C extends FinrocAnnotation> C getAnnotation(Class<C> c) {
        return annotationManager.getAnnotation(c);
    }

    /**
     * @return Qualified link of remote framework element
     */
    public String getQualifiedLink() {
        if (getParent() == null || (!(getParent() instanceof RemoteFrameworkElement)) || getParent() instanceof RemoteRuntime) {
            return toString();
        } else {
            return ((RemoteFrameworkElement)getParent()).getQualifiedLink() + "/" + toString();
        }
    }

    /**
     * Is specified flag set?
     * for convenience - don't use in places with absolute maximum performance requirements (?)
     *
     * @param flag Flag to check
     * @return Answer
     */
    public boolean getFlag(int flag) {
        return (flags & flag) == flag;
    }

    /**
     * @param tag Tag to check
     *
     * @return True if framework element is tagged with the specified tag
     */
    public boolean isTagged(String tag) {
        return (tags != null) && tags.contains(tag);
    }

    /**
     * @return Flags of this remote framework element
     */
    public int getAllFlags() {
        return flags;
    }

    /**
     * Returns first parent that has the specified flags
     *
     * @param node Model node to start searching from
     * @param flags Flags to look for
     * @param includeThis Include this element in search (in other words: check this first)?
     * @return Parent or null
     */
    public static RemoteFrameworkElement getParentWithFlags(ModelNode node, int flags, boolean includeThis) {
        if (includeThis && (node instanceof RemoteFrameworkElement) && ((RemoteFrameworkElement)node).getFlag(flags)) {
            return (RemoteFrameworkElement)node;
        }
        if (node.getParent() != null) {
            return getParentWithFlags((ModelNode)node.getParent(), flags, true);
        }
        return null;
    }

    /**
     * @return Is this an interface with only output data ports? (if it has no data ports, checks whether name contains 'output')
     */
    public boolean isOutputOnlyInterface() {
        classifyInterface();
        return isInterface() && getFlag(FrameworkElementFlags.INTERFACE_FOR_OUTPUTS) && (!getFlag(FrameworkElementFlags.INTERFACE_FOR_INPUTS));
    }

    /**
     * @return Is this an interface with only input data ports? (if it has no data ports, checks whether name contains 'input')
     */
    public boolean isInputOnlyInterface() {
        classifyInterface();
        return isInterface() && getFlag(FrameworkElementFlags.INTERFACE_FOR_INPUTS) && (!getFlag(FrameworkElementFlags.INTERFACE_FOR_OUTPUTS));
    }

    /**
     * @return Is this an interface for sensor data (only)?
     */
    public boolean isSensorInterface() {
        return isInterface() && getFlag(FrameworkElementFlags.SENSOR_DATA) && (!getFlag(FrameworkElementFlags.CONTROLLER_DATA));
    }

    /**
     * @return Is this an interface for controller data (only)?
     */
    public boolean isControllerInterface() {
        return isInterface() && getFlag(FrameworkElementFlags.CONTROLLER_DATA) && (!getFlag(FrameworkElementFlags.SENSOR_DATA));
    }

    /**
     * @return Is this remote element a component?
     */
    public boolean isComponent() {
        return isTagged("module") || isCompositeComponent();
    }

    /**
     * @return Is this remote element a composite component?
     */
    public boolean isCompositeComponent() {
        return isTagged("group");
    }

    /**
     * @return Is this a proxy interface? (interface of a composite component)
     */
    public boolean isProxyInterface() {
        classifyInterface();
        return isInterface() && getFlag(FrameworkElementFlags.PROXY_INTERFACE);
    }

    /**
     * @return Is this an RPC-only interface
     */
    public boolean isRpcOnlyInterface() {
        classifyInterface();
        return isInterface() && (flags & (FrameworkElementFlags.INTERFACE_FOR_RPC_PORTS | FrameworkElementFlags.INTERFACE_FOR_DATA_PORTS)) == FrameworkElementFlags.INTERFACE_FOR_RPC_PORTS;
    }

    /**
     * @return Is this an editable interface?
     */
    public boolean isEditableInterface() {
        return isInterface() && isTagged("edit");
    }

    @Override
    public boolean isInterface() {
        return getFlag(FrameworkElementFlags.INTERFACE);
    }

    /**
     * Unlike getEditableInterfacesObject() this method does not block
     *
     * @return Editable interfaces of this element (which is typically a component). null if it has no editable interfaces.
     */
    public synchronized ArrayList<RemoteFrameworkElement> getEditableInterfaces() {

        if (!editableInterfacesChecked) {
            for (int i = 0; i < getChildCount(); i++) {
                RemoteFrameworkElement child = (RemoteFrameworkElement)this.getChildAt(i);
                if (child.isEditableInterface()) {
                    if (editableInterfaces == null) {
                        editableInterfaces = new ArrayList<RemoteFrameworkElement>();
                    }
                    child.classifyInterface();
                    if (isCompositeComponent()) {
                        child.flags |= FrameworkElementFlags.PROXY_INTERFACE;
                    }
                    editableInterfaces.add(child);
                }
            }
            editableInterfacesChecked = true;
        }

        return editableInterfaces;
    }

    /**
     * @return Editable interfaces object for this element (blocks until they have been received from remote runtime)
     * @throws Exception if receiving interfaces failed
     */
    public RemoteEditableInterfaces getEditableInterfacesObject() throws Exception {
        RemoteRuntime runtime = RemoteRuntime.find(this);
        if (runtime == null) {
            throw new Exception("No runtime found for this element");
        }
        RemoteEditableInterfaces result = new RemoteEditableInterfaces();
        result.deserialize(runtime.getAdminInterface().getAnnotation(getRemoteHandle(), RemoteEditableInterfaces.TYPE_NAME, runtime));
        return result;
    }

    /**
     * Checks whether interface is classified.
     * If not, set flags as to whether this is e.g. input or output interface.
     */
    private void classifyInterface() {
        int ALL_FLAGS = FrameworkElementFlags.INTERFACE_FOR_OUTPUTS | FrameworkElementFlags.INTERFACE_FOR_INPUTS | FrameworkElementFlags.INTERFACE_FOR_RPC_PORTS | FrameworkElementFlags.INTERFACE_FOR_DATA_PORTS;
        if (isInterface()) {
            if ((flags & ALL_FLAGS) == 0) {
                // currently classification is determined by-name. However, this should not be based on heuristics in future finroc versions.
                if (getName().contains("Output") || getName().contains("output")) {
                    flags |= FrameworkElementFlags.INTERFACE_FOR_OUTPUTS | FrameworkElementFlags.INTERFACE_FOR_DATA_PORTS | FrameworkElementFlags.FINAL_INTERFACE_CLASSIFICATION;
                }
                if (getName().contains("Input") || getName().contains("input") || getName().equals("Parameters")) {
                    flags |= FrameworkElementFlags.INTERFACE_FOR_INPUTS | FrameworkElementFlags.INTERFACE_FOR_DATA_PORTS | FrameworkElementFlags.FINAL_INTERFACE_CLASSIFICATION;
                }
                if (getName().equals("Services")) {
                    flags |= FrameworkElementFlags.ACCEPTS_DATA | FrameworkElementFlags.EMITS_DATA | FrameworkElementFlags.INTERFACE_FOR_RPC_PORTS | FrameworkElementFlags.FINAL_INTERFACE_CLASSIFICATION;
                }
                if (getName().equals("Blackboards")) {
                    flags |= FrameworkElementFlags.ACCEPTS_DATA | FrameworkElementFlags.EMITS_DATA | FrameworkElementFlags.INTERFACE_FOR_RPC_PORTS | FrameworkElementFlags.INTERFACE_FOR_DATA_PORTS  | FrameworkElementFlags.FINAL_INTERFACE_CLASSIFICATION;
                }
                if (getParent() instanceof RemoteFrameworkElement && ((RemoteFrameworkElement)getParent()).isCompositeComponent()) {
                    flags |= FrameworkElementFlags.PROXY_INTERFACE;
                }
                if (getName().contains("Sensor") || getName().contains("sensor")) {
                    flags |= FrameworkElementFlags.SENSOR_DATA;
                }
                if (getName().contains("Control") || getName().contains("control")) {
                    flags |= FrameworkElementFlags.CONTROLLER_DATA;
                }
            }
            if ((flags & FrameworkElementFlags.FINAL_INTERFACE_CLASSIFICATION) == 0) {
                for (int i = 0; i < getChildCount(); i++) {
                    if (getChildAt(i) instanceof RemotePort) {
                        RemotePort remotePort = (RemotePort)getChildAt(i);
                        flags |= (remotePort.getFlags() & FrameworkElementFlags.IS_OUTPUT_PORT) != 0 ? FrameworkElementFlags.INTERFACE_FOR_OUTPUTS : FrameworkElementFlags.INTERFACE_FOR_INPUTS;
                        if ((remotePort.getDataType().getTypeTraits() & DataTypeBase.IS_RPC_TYPE) != 0) {
                            flags |= FrameworkElementFlags.INTERFACE_FOR_RPC_PORTS;
                        } else if ((remotePort.getDataType().getTypeTraits() & DataTypeBase.IS_DATA_TYPE) != 0) {
                            flags |= FrameworkElementFlags.INTERFACE_FOR_DATA_PORTS;
                        }
                    }
                }
                if ((flags & ALL_FLAGS) == ALL_FLAGS) {
                    flags |= FrameworkElementFlags.FINAL_INTERFACE_CLASSIFICATION;
                }
            }
        }
    }

    @Override
    public boolean isHidden(boolean checkAncestors) {
        // Hide empty interfaces
        if (isInterface() && getChildCount() == 0) {
            return true;
        }
        return super.isHidden(checkAncestors);
    }
}
