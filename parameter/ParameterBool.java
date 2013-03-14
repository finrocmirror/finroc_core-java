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
package org.finroc.core.parameter;

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.CoreBoolean;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortListener;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataManagerTL;

/**
 * @author Max Reichardt
 *
 * Parameter template class for cc types
 */
public class ParameterBool extends Parameter<CoreBoolean> {

    /**
     * Caches bool value of parameter port (optimization)
     */
    static class BoolCache implements PortListener<CoreBoolean> {

        /** Cached current value (we will much more often read than it will be changed) */
        public volatile boolean currentValue;

        @Override
        public void portChanged(AbstractPort origin, CoreBoolean value) {
            currentValue = ((CoreBoolean)value).get();
        }
    }

    /** Bool cache instance used for this parameter */
    public BoolCache cache = new BoolCache();

    public ParameterBool(String name, FrameworkElement parent, boolean defaultValue, String configEntry) {
        this(name, parent, defaultValue);
        setConfigEntry(configEntry);
    }

    public ParameterBool(String name, FrameworkElement parent, boolean defaultValue) {
        super(name, parent, CoreBoolean.TYPE);
        this.addPortListener(cache);
        cache.currentValue = defaultValue;
        setDefault(CoreBoolean.getInstance(defaultValue));
    }

    /**
     * @return Current parameter value
     */
    public boolean getValue() {
        return cache.currentValue;
    }

    /**
     * @param b new value
     */
    public void set(boolean b) {
        CCPortDataManagerTL cb = ThreadLocalCache.get().getUnusedBuffer(CoreBoolean.TYPE);
        cb.getObject().<CoreBoolean>getData().set(b);
        ((CCPortBase)wrapped).browserPublishRaw(cb);
        cache.currentValue = b;
    }
}
