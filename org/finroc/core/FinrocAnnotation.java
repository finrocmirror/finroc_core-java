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

import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.core.portdatabase.TypedObjectImpl;
import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.Ptr;

/**
 * @author max
 *
 * Base class for all finroc element annotations
 */
@Friend(FrameworkElement.class)
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
        type = DataTypeRegister.getInstance().lookupDataType(this);
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
}
