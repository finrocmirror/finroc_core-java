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
package org.finroc.core;

import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Abstract base class for classes that can be annotated with
 * an arbitrary number of FinrocAnnotations.
 */
public class Annotatable {

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
    public synchronized void addAnnotation(FinrocAnnotation ann) {
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
     * @return Annotation. Null if framework element has no annotation of this type.
     */
    public FinrocAnnotation getAnnotation(DataTypeBase dt) {
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


    public void delete() {

        // delete annotations
        FinrocAnnotation a = firstAnnotation;
        while (a != null) {
            FinrocAnnotation tmp = a;
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
