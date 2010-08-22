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
package org.finroc.core.setting;

import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortCreationInfo;
import org.finroc.core.port.PortFlags;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortListener;
import org.finroc.core.port.cc.NumberPort;
import org.finroc.jc.annotation.CppDelegate;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NoCpp;

/**
 * @author max
 *
 * Boolean setting
 */
@Inline @NoCpp @JavaOnly @CppDelegate(Setting.class)
public class BoolSetting extends Setting implements CCPortListener<CoreNumber> {

    /** Current value */
    private boolean currentValue;

    public BoolSetting(String description, boolean defaultVal, boolean publishAsPort) {
        super(description, publishAsPort);
        currentValue = defaultVal;
    }

    public boolean get() {
        return currentValue;
    }

    @Override
    public AbstractPort createPort(FrameworkElement parent) {
        NumberPort p = new NumberPort(new PortCreationInfo(description, parent, PortFlags.INPUT_PORT));
        p.setDefault(currentValue ? 1 : 0);
        p.addPortListener(this);
        return p;
    }

    @Override
    public void portChanged(CCPortBase origin, CoreNumber value) {
        currentValue = value.intValue() != 0;
    }

    public NumberPort getPort() {
        return (NumberPort)super.getPort();
    }
}