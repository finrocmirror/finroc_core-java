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

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.container.SimpleList;
//import org.finroc.core.ConfigFile;
import org.finroc.core.FrameworkElement;
import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.Unit;

/**
 * @author max
 *
 * This class contains a set of parameters/settings that can be
 * conveniently managed/accessed/created.
 *
 * The parameters may be exposed to the outside as input ports.
 * The parameters can be loaded from a config file.
 *
 * The parameters can have (safe) static declarations.
 * Initialization of ports etc. will start as soon as init() is
 * called (this should not be done from static initializations)
 */
@CppInclude( { "RuntimeEnvironment.h"})
@CppPrepend( {"Settings::~Settings() {",
              "    if (isStatic) {",
              "        RuntimeEnvironment::shutdown();",
              "    }",
              "    if (portRoot != NULL && (!RuntimeEnvironment::shuttingDown())) {",
              "         portRoot->managedDelete();",
              "    }",
              "    for (size_t i = 0, n = settings.size(); i < n; i++) {",
              "        delete settings.get(i);",
              "    }",
              "    settings.clear();",
              "}"
             })
@Include("NumberSetting.h")
public class Settings {

    /** Prefix of keys in config file */
    private final String configPrefix;

    /** Settings description (port will be called like this) */
    private final String description;

    /** Framework element representing settings - will be initialized deferred in C++ */
    @Ptr
    protected FrameworkElement portRoot;

    /** All defined settings */
    private final SimpleList<Setting> settings = new SimpleList<Setting>();

    /** Have settings been initialized? */
    private boolean initialized = false;

    /** Is this a static settings instance? */
    private boolean isStatic;

    /**
     * @param description Settings description (port will be called like this)
     * @param configPrefix Prefix of keys in config file
     * @param isStatic Is this a static settings instance?
     */
    public Settings(@Const @Ref String description, @Const @Ref String configPrefix, boolean isStatic) {
        this.configPrefix = configPrefix + ".";
        this.description = description;
        this.isStatic = isStatic;
    }

    /*Cpp
    virtual ~Settings();
    */

    /**
     * Add settings node to specified FrameworkElement
     *
     * @param parent Settings instance to add these settings to
     */
    public void init(@Ptr Settings parent) {
        init(parent.portRoot);
    }

    /**
     * Add settings node to specified FrameworkElement
     *
     * @param parent FrameworkElement to add settings node to
     */
    public void init(@Ptr FrameworkElement parent) {
        if (initialized) {
            return;
        }
        portRoot = new FrameworkElement(description, parent);
        for (int i = 0, n = settings.size(); i < n; i++) {
            Setting set = settings.get(i);
            if (set.publishAsPort()) {
                set.port = set.createPort(portRoot);
            }
        }
        portRoot.init();
        initialized = true;
    }

    /**
     * @param description Description/Key of setting
     * @param defaultVal Default value
     * @param publishAsPort Publish setting as port?
     * @return Created bool setting
     */
    public BoolSetting add(@Const @Ref String description, boolean defaultVal, boolean publishAsPort) {
        BoolSetting setting = new BoolSetting(description, defaultVal, publishAsPort);
        settings.add(setting);
        return setting;
    }

    /**
     * @param description Description/Key of setting
     * @param defaultVal Default value
     * @param publishAsPort Publish setting as port?
     * @return Created int setting
     */
    @JavaOnly public IntSetting add(@Const @Ref String description, int defaultVal, boolean publishAsPort) {
        return add(description, defaultVal, publishAsPort, Unit.NO_UNIT, new Bounds());
    }


    /**
     * @param description Description/Key of setting
     * @param defaultVal Default value
     * @param publishAsPort Publish setting as port?
     * @param Unit unit
     * @param bounds Bounds for values
     * @return Created int setting
     */
    public IntSetting add(@Const @Ref String description, int defaultVal, boolean publishAsPort, @CppDefault("&Unit::NO_UNIT") Unit unit, @CppDefault("Bounds()") Bounds bounds) {
        IntSetting setting = new IntSetting(description, defaultVal, publishAsPort, unit, bounds);
        settings.add(setting);
        return setting;
    }

    /**
     * @param description Description/Key of setting
     * @param defaultVal Default value
     * @param publishAsPort Publish setting as port?
     * @return Created int setting
     */
    @JavaOnly public LongSetting add(@Const @Ref String description, long defaultVal, boolean publishAsPort) {
        return add(description, defaultVal, publishAsPort, Unit.NO_UNIT, new Bounds());
    }

    /**
     * @param description Description/Key of setting
     * @param defaultVal Default value
     * @param publishAsPort Publish setting as port?
     * @param Unit unit
     * @param bounds Bounds for values
     * @return Created int setting
     */
    public LongSetting add(@Const @Ref String description, long defaultVal, boolean publishAsPort, @CppDefault("&Unit::NO_UNIT") Unit unit, @CppDefault("Bounds()") Bounds bounds) {
        LongSetting setting = new LongSetting(description, defaultVal, publishAsPort, unit, bounds);
        settings.add(setting);
        return setting;
    }

    /**
     * @param description Description/Key of setting
     * @param defaultVal Default value
     * @param publishAsPort Publish setting as port?
     * @return Created int setting
     */
    @JavaOnly public DoubleSetting add(@Const @Ref String description, double defaultVal, boolean publishAsPort) {
        return add(description, defaultVal, publishAsPort, Unit.NO_UNIT, new Bounds());
    }

    /**
     * @param description Description/Key of setting
     * @param defaultVal Default value
     * @param publishAsPort Publish setting as port?
     * @param Unit unit
     * @param bound Bounds for values
     * @return Created int setting
     */
    public DoubleSetting add(@Const @Ref String description, double defaultVal, boolean publishAsPort, @CppDefault("&Unit::NO_UNIT") Unit unit, @CppDefault("Bounds()") Bounds bounds) {
        DoubleSetting setting = new DoubleSetting(description, defaultVal, publishAsPort, unit, bounds);
        settings.add(setting);
        return setting;
    }
}
