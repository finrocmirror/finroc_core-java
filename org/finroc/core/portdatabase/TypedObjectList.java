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
package org.finroc.core.portdatabase;

import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.std.PortData;
import org.finroc.core.port.std.PortDataImpl;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.container.SimpleList;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * List of typed objects
 *
 * Thread-safety: As with other port data, modifying list is not thread-safe.
 */
public abstract class TypedObjectList extends PortDataImpl implements HasDestructor {

    /** Wrapped List backend */
    private SimpleList<TypedObject> list;

    /** Default type of list elements */
    private final DataType elementType;

    /** Size of list - capacity is size of 'list' */
    private @SizeT int size;

    /**
     * @param elementType Default type of list elements
     */
    public TypedObjectList(DataType elementType) {
        this.elementType = elementType;
    }

    public void delete() {
        setCapacity(0, null);
    }

    /**
     * @return Type of list elements
     */
    @ConstMethod public DataType getElementType() {
        return elementType;
    }

    @Override
    public void serialize(CoreOutput os) {
        os.writeInt(list.size());
        for (@SizeT int i = 0; i < list.size(); i++) {
            TypedObject to = list.get(i);
            os.writeShort(to.getType().getUid());
            to.serialize(os);
        }
    }

    @Override
    public void deserialize(CoreInput is) {
        @SizeT int size = is.readInt();
        if (size > list.size()) {
            setCapacity(size, null);
        }
        for (@SizeT int i = 0; i < size; i++) {
            DataType dt = is.readType();
            if (dt == null) {
                throw new RuntimeException("Datatype is not available here");
            }
            TypedObject to = list.get(i);
            if (to.getType() != dt) { // Correct buffer type? (if not, replace)
                discardBuffer(to);
                assert(validListType(dt));
                to = createBuffer(dt);
                list.set(i, to);
            }
            to.deserialize(is);
        }
        setSize(size);
    }

    @Override
    public void serialize(XMLNode node) throws Exception {
        for (@SizeT int i = 0; i < list.size(); i++) {
            TypedObject to = list.get(i);
            XMLNode n = node.addChildNode("element");
            if (to.getType() != elementType) {
                n.setAttribute("type", to.getType().getName());
            }
            to.serialize(n);
        }
    }

    @Override
    public void deserialize(XMLNode node) throws Exception {
        list.clear();
        @PassByValue SimpleList<XMLNode> children = new SimpleList<XMLNode>();
        children.addAll(node.getChildren());
        if (children.size() > list.size()) {
            setCapacity(size, null);
        }
        for (@SizeT int i = 0; i < children.size(); i++) {
            XMLNode n = children.get(i);
            DataType dt = elementType;
            if (n.hasAttribute("type")) {
                dt = DataTypeRegister.getInstance().getDataType(n.getStringAttribute("type"));
            }
            if (dt == null) {
                throw new RuntimeException("Datatype is not available here");
            }
            TypedObject to = list.get(i);
            if (to.getType() != dt) { // Correct buffer type? (if not, replace)
                discardBuffer(to);
                assert(validListType(dt));
                to = createBuffer(dt);
                list.set(i, to);
            }
            to.deserialize(n);
        }
        setSize(size);
    }

    /**
     * @param newSize New list size (does not change capacity - except of capacity being to small)
     * (fills list with standard element type)
     */
    public void setSize(@SizeT int newSize) {
        if (newSize > getCapacity()) {
            setCapacity(newSize, elementType);
        }
        size = newSize;
    }

    /**
     * @param newSize New list size (does not change capacity - except of capacity being to small)
     * @param dt Data type of buffers to fill new entries with if size increases
     */
    public void setSize(@SizeT int newSize, DataType dt) {
        if (newSize > getCapacity()) {
            setCapacity(newSize, dt);
        }
        size = newSize;
    }

    /**
     * Ensure that buffer has at least the specified capacity
     *
     * @param capacity Capacity
     */
    public void ensureCapacity(@SizeT int capacity) {
        ensureCapacity(capacity, elementType);
    }

    /**
     * Ensure that buffer has at least the specified capacity
     *
     * @param capacity Capacity
     * @param dt Data type to fill entries with if size increases (null fills entries with NULL)
     */
    public void ensureCapacity(@SizeT int capacity, DataType dt) {
        if (list.size() < capacity) {
            setCapacity(capacity, dt);
        }
    }

    /**
     * @param newCapacity New Capacity (new buffers are allocated or deleted if capacity changes)
     * @param dt Data type to fill entries with if size increases (null fills entries with NULL)
     */
    public void setCapacity(@SizeT int newCapacity, DataType dt) {
        assert(validListType(dt));
        while (list.size() > newCapacity) {
            TypedObject to = list.remove(list.size() - 1);
            discardBuffer(to);
        }
        while (list.size() < newCapacity) {
            list.add(dt == null ? null : createBuffer(dt));
        }
    }

    /**
     * Create new buffer/instance of port data
     *
     * @param dt Data type of buffer to create
     * @return Returns new instance
     */
    protected @Ptr @Managed TypedObject createBuffer(DataType dt) {
        if (elementType.isStdType()) {
            PortDataManager pdm = new PortDataManager(dt, (PortData)(getSize() > 0 ? getElement(getSize() - 1) : null));
            pdm.getCurrentRefCounter().setOrAddLocks((byte)1);
            return pdm.getData();
        } else if (elementType.isCCType()) {
            assert(dt.isCCType());
            return dt.createInterThreadInstance(); // not attached to any queue
            //return ThreadLocalCache.get().getUnusedInterThreadBuffer(dt);
        } else {
            throw new RuntimeException("Invalid data type for list");
        }
    }

    /**
     * @param to Object/buffer to discard (usually it is deleted or recycled)
     */
    protected abstract void discardBuffer(TypedObject to);

    /**
     * @return size of list
     */
    @ConstMethod public @SizeT int getSize() {
        return size;
    }

    /**
     * @return capacity of list
     */
    @ConstMethod public @SizeT int getCapacity() {
        return list.size();
    }

    /**
     * @param index Index
     * @return Element at specified index in this list
     */
    @ConstMethod protected TypedObject getElement(int index) {
        return list.get(index);
    }

    /**
     * Replace buffer at specified index
     *
     * @param index Index
     * @param element (Locked!) Buffer to add to specified index (the replaced one is discarded)
     */
    protected void setElement(int index, TypedObject element) {
        assert(validListType(element.getType()));
        discardBuffer(list.get(index));
        list.set(index, element);
    }

    /**
     * @param dt Data type
     * @return May value of this type be added to list?
     */
    @Virtual @InCppFile
    public boolean validListType(DataType dt) {
        return dt.isConvertibleTo(getElementType());
    }

    /**
     * Removes element at specified index
     * (buffer is appended to end of list => capacity remains unchanged)
     *
     * @param index Index at which to remove element
     */
    public void removeElement(@SizeT int index) {
        if (index == size - 1) {
            setSize(getSize() - 1);
        } else if (index >= 0 && index < size) {
            TypedObject removed = list.remove(index);
            list.add(removed);
            setSize(getSize() - 1);
        }
    }

    /**
     * Add auto-lock to specified object
     * (calls ThreadLocalCache::get()->addAutoLock(t);
     * for classes who can't, because of cyclic dependencies)
     *
     * @param t object
     */
    @InCppFile
    protected void addAutoLock(@Const PortData t) {
        ThreadLocalCache.get().addAutoLock(t);
    }

    /**
     * Add auto-lock to specified object
     * (calls ThreadLocalCache::get()->addAutoLock(t);
     * for classes who can't, because of cyclic dependencies)
     *
     * @param t object
     */
    @InCppFile
    protected void addAutoLock(CCInterThreadContainer<?> t) {
        ThreadLocalCache.get().addAutoLock(t);
    }
}
