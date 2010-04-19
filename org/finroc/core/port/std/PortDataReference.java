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
package org.finroc.core.port.std;

import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Superclass2;

/**
 * @author max
 *
 * Reference to port data.
 *
 * In Java, several of these are created to compensate
 * that reuse counters cannot be encoded in pointer of data as in C++.
 * "portratio" contains an explanation.
 *
 * In C++, this is a pointer that points slightly above the port data.
 * That means, the class is empty and no objects of this class actually exist.
 *
 * Is immutable
 */
@Ptr @Superclass2( {"CombinedPointer<PortData>"}) @Inline @NoCpp
@Include("CombinedPointer.h")
public class PortDataReference {

    /** Port data that is referenced */
    @JavaOnly private final PortData portData;

    /** Reference counter associated with this reference */
    @JavaOnly private final PortDataManager.RefCounter refCounter;

    /**
     * @param portData Port data that is referenced
     */
    @JavaOnly PortDataReference(PortData portData, PortDataManager.RefCounter refCounter) {
        this.portData = portData;
        this.refCounter = refCounter;
    }

    /**
     * @return Referenced port data
     */
    @InCpp("return getPointer();")
    @ConstMethod public PortData getData() {
        return portData;
    }

    /**
     * @return Reference counter associated with this reference
     */
    @InCpp("return &(getManager()->refCounters[getInfo()]);")
    @ConstMethod public PortDataManager.RefCounter getRefCounter() {
        return refCounter;
    }

    /**
     * @return Container of referenced data
     */
    @ConstMethod public PortDataManager getManager() {
        return getData().getManager();
    }

    /**
     * @return Is data currently locked?
     */
    @ConstMethod public boolean isLocked() {
        return getRefCounter().isLocked();
    }
}
