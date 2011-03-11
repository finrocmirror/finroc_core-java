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
package org.finroc.core;

import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.Virtual;
import org.finroc.serialization.DataTypeBase;
import org.finroc.serialization.InputStreamBuffer;
import org.finroc.serialization.OutputStreamBuffer;
import org.finroc.serialization.TypedObjectImpl;

/**
 * @author max
 *
 * Base class for all finroc element annotations.
 *
 * If annotation should be available over the net (e.g. in finstruct),
 * the serialization methods need to be overridden.
 */
@Friend( {FrameworkElement.class, Annotatable.class})
public abstract class FinrocAnnotation extends TypedObjectImpl implements HasDestructor {

    /** Next framework element annotation - used to build linked list - null if no more annotations */
    FinrocAnnotation nextAnnotation;

    /** Object that is annotated - null if annotation is not attached to an object yet */
    @Ptr Annotatable annotated;

    /**
     * Add another annotation to framework element
     * (added to end of linked list)
     */
    void append(FinrocAnnotation ann) {
        if (nextAnnotation == null) {
            nextAnnotation = ann;
        } else {
            nextAnnotation.append(ann);
        }
    }

    /**
     * initialize data type
     */
    public void initDataType() {
        if (type != null) {
            return; // already set
        }

        //JavaOnlyBlock
        type = DataTypeBase.findType(this.getClass());

        //Cpp type = rrlib::serialization::DataTypeBase::findTypeByRtti(_typeid(*this)._name());

        assert type != null : "Unknown Object type";
    }

    @Override
    public void delete() {
    }

    /**
     * @return Object that is annotated - null if annotation is not attached to an object yet
     */
    public @Ptr Annotatable getAnnotated() {
        return annotated;
    }

    @Override
    public void serialize(OutputStreamBuffer os) {
        throw new RuntimeException("Unsupported");
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        throw new RuntimeException("Unsupported");
    }

    /**
     * Called when annotated object is initialized
     * (supposed to be overridden)
     */
    @Virtual protected void annotatedObjectInitialized() {}

    /**
     * Called when annotated object is about to be deleted
     * (supposed to be overridden)
     */
    @Virtual protected void annotatedObjectToBeDeleted() {}

    /**
     * Searches for parent with annotation of specified type
     *
     * @param fe Framework element to start searching at
     * @param type Data Type
     * @return Annotation of first parent that has one - or null
     */
    protected static FinrocAnnotation findParentWithAnnotation(FrameworkElement fe, @Const @Ref DataTypeBase type) {
        FinrocAnnotation ann = fe.getAnnotation(type);
        if (ann != null) {
            return ann;
        }
        FrameworkElement parent = fe.getParent();
        if (parent != null) {
            return findParentWithAnnotation(parent, type);
        }
        return null;
    }
}
