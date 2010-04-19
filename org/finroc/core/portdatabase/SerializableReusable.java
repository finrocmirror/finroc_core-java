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
package org.finroc.core.portdatabase;

import org.finroc.jc.annotation.NoCpp;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.container.Reusable;

/**
 * @author max
 *
 * This is the base class for some classes that are both Serializable and Reusable
 */
@NoCpp
public abstract class SerializableReusable extends Reusable implements CoreSerializable {

//  /** Id of Thread responsible of recycling this object - responsibility may me transferred */
//  public volatile long responsibleThread;

//  /** Is current thread responsible of recycling object? */
//  public boolean isResponsible() {
//      return ThreadUtil.getCurrentThreadId() == responsibleThread;
//  }
//
//  public void recycle() {
//      responsibleThread = -1;
//      super.recycle();
//  }

    /**
     * Recycle call object - after calling this method, object is available in ReusablesPool it originated from
     *
     * (may be overridden by subclasses to perform custom cleanup)
     */
    @Virtual public void genericRecycle() {
        //responsibleThread = -1;
        super.recycle();
    }

}
