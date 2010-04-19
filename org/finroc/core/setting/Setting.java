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
import org.finroc.core.port.AbstractPort;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.DefaultType;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.annotation.PostInclude;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.Virtual;

/**
 * @author max
 *
 * This is the abstract base for a simple setting
 */
@Ptr
@PostInclude("NumberSetting.h")
@ForwardDecl( {FrameworkElement.class, AbstractPort.class})
@CppInclude("FrameworkElement.h")
//@HAppend("#include \"libraries/core/settings/NumberSetting.h\"")
@DefaultType("AbstractPort")
@Friend(Settings.class)
public abstract class Setting {

    /** description of setting */
    protected final String description;

    /** Publish setting as port? */
    private final boolean publishAsAPort;

    /** Port - in case setting is published as port */
    @Ptr protected AbstractPort port;

    /**
     * @param description description of setting
     * @param Publish setting as port?
     */
    protected Setting(@Const @Ref String description, boolean publish) {
        this.description = description;
        publishAsAPort = publish;
    }

    /**
     * @return description of setting
     */
    @ConstMethod public String getDescription() {
        return description;
    }

    /**
     * @return Publish setting as port?
     */
    @ConstMethod public boolean publishAsPort() {
        return publishAsAPort;
    }

    /**
     * Creates port for setting as child of specified framework element
     *
     * @param parent Framework element
     */
    @InCppFile @Virtual
    public abstract @Ptr AbstractPort createPort(@Ptr FrameworkElement parent);

    /**
     * @return Port - in case setting is published as port
     */
    @NonVirtual public AbstractPort getPort() {
        return port;
    }
}
