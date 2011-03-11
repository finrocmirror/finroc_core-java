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
package org.finroc.core;

import org.finroc.jc.HasDestructor;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.log.LogUser;
import org.finroc.serialization.DataTypeBase;

/**
 * @author max
 *
 * Abstract base class for classes that can be annotated with
 * an arbitrary number of FinrocAnnotations.
 */
public class Annotatable extends LogUser implements HasDestructor {

    /**
     * First element of framework element annotation linked list
     * Annotations may be changed - but not deleted.
     */
    private FinrocAnnotation firstAnnotation = null;

    /**
     * Add annotation to this framework element
     *
     * @param ann Annotation
     */
    public synchronized void addAnnotation(@Managed FinrocAnnotation ann) {
        if (ann.getType() == null) {
            ann.initDataType();
            assert(ann.getType() != null) : "Initializing data type failed";
        }
        assert(ann.annotated == null) : "Already used as annotation in other object. Not allowed (double deleteting etc.)";
        ann.annotated = this;
        if (firstAnnotation == null) {
            firstAnnotation = ann;
        } else {
            firstAnnotation.append(ann);
        }
    }

    /**
     * Get annotation of specified type
     *
     * @param type Data type of annotation we're looking for
     */
    public FinrocAnnotation getAnnotation(@Const @Ref DataTypeBase dt) {
        FinrocAnnotation ann = firstAnnotation;
        while (ann != null) {
            if (ann.getType() == dt) {
                return ann;
            }
            ann = ann.nextAnnotation;
        }
        return null;
    }

    /**
     * Get annotation of specified type
     *
     * @param c Data type of annotation we're looking for
     */
    @SuppressWarnings("unchecked")
    @JavaOnly
    public <C extends FinrocAnnotation> C getAnnotation(Class<C> c) {
        FinrocAnnotation ann = firstAnnotation;
        while (ann != null) {
            if (c.isAssignableFrom(ann.getClass())) {
                return (C)ann;
            }
            ann = ann.nextAnnotation;
        }
        return null;
    }


    @Override
    public void delete() {

        // delete annotations
        FinrocAnnotation a = firstAnnotation;
        while (a != null) {
            @Ptr FinrocAnnotation tmp = a;
            a = a.nextAnnotation;
            tmp.delete();
        }
    }

    /**
     * Notify annotations that object has been initialized
     */
    protected void notifyAnnotationsInitialized() {
        FinrocAnnotation ann = firstAnnotation;
        while (ann != null) {
            ann.annotatedObjectInitialized();
            ann = ann.nextAnnotation;
        }
    }

    /**
     * Notify annotations that object is to be deleted
     */
    protected void notifyAnnotationsDelete() {
        FinrocAnnotation ann = firstAnnotation;
        while (ann != null) {
            ann.annotatedObjectToBeDeleted();
            ann = ann.nextAnnotation;
        }
    }

}
