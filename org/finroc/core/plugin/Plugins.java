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
package org.finroc.core.plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.finroc.core.RuntimeSettings;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.finstructable.FinstructableGroup;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.container.SimpleList;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;

import org.finroc.core.util.Files;

/**
 * @author max
 *
 * This class is used for managing the Runtime's plugins
 */
@CppInclude("RuntimeEnvironment.h")
@CppPrepend( {
    "Plugins::DLCloser::~DLCloser() {",
    "    RuntimeEnvironment::shutdown();",
    "    for (size_t i = 0; i < loaded.size(); i++) {",
    "        _dlclose(loaded.get(i));",
    "    }",
    "}"
})
public class Plugins { /*implements HTTPResource*/


    /** Plugins singleton instance */
    @JavaOnly
    private static @SharedPtr Plugins instance;

    /** All Plugins that are currently available */
    private final SimpleList<Plugin> plugins = new SimpleList<Plugin>();

    /** List with actions to create external connections */
    private final SimpleList<CreateExternalConnectionAction> externalConnections = new SimpleList<CreateExternalConnectionAction>();

    /** List with actions to create modules */
    @JavaOnly
    private final SimpleList<CreateFrameworkElementAction> moduleTypes = new SimpleList<CreateFrameworkElementAction>();

//    /** Plugin manager instance */
//    private final PluginManager pluginManager = new PluginManager();

    /** Plugin loader implementation */
    @JavaOnly @SharedPtr private PluginLoader pluginLoader;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"plugins\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("plugins");

    /**
     * Loads plugins
     */
    public static void staticInit() {
        @Ptr Plugins p = getInstance();
        p.findAndLoadPlugins();
    }

    /**
     * @return Plugins singleton instance
     */
    @InCpp( {"static Plugins instance;", "return &instance;"})
    public static @Ptr Plugins getInstance() {
        if (instance == null) {
            instance = new Plugins();
        }
        return instance;
    }

    private void findAndLoadPlugins() {
        //TODO do properly

        // JavaOnlyBlock
        if (!(RuntimeSettings.isDebugging()) || RuntimeSettings.isRunningInApplet()) {
            pluginLoader = new JavaReleasePluginLoader();
        } else {
            pluginLoader = new JavaDebugPluginLoader();
        }
        loadAllDataTypesInPackage(CoreNumber.class);
        loadAllDataTypesInPackage(FinstructableGroup.class);
        SimpleList<Plugin> plugins = pluginLoader.findPlugins(/*Plugins.class*/);
        for (Plugin plugin : plugins.getBackend()) {
            addPlugin(plugin);
        }
        //JavaPlugins.loadAllDataTypesInPackage(BehaviourInfo.class);

        ////Cpp plugins.add(new MCA2Plugin());
    }

    /**
     * (possibly manually) add plugin
     *
     * @param p Plugin to add
     */
    public void addPlugin(@Ptr @Managed Plugin p) {
        plugins.add(p);
        p.init(/*pluginManager*/);
    }

    /**
     * @return List with modules for external connections
     */
    public @Ptr SimpleList<CreateExternalConnectionAction> getExternalConnections() {
        return externalConnections;
    }

    /**
     * Register module that can be used as external connection (e.g. in GUI)
     *
     * @param action Action to be registered
     * @return Action to be registered (same as above)
     */
    public CreateExternalConnectionAction registerExternalConnection(CreateExternalConnectionAction action) {
        externalConnections.add(action);
        getModuleTypes().add(action);
        return action;
    }

    /**
     * @return List with plugins (do not modify!)
     */
    public @Ptr SimpleList<Plugin> getPlugins() {
        return plugins;
    }

    /**
     * Load all data types in a certain package
     *
     * @param classInPackage Class in this package
     */
    @JavaOnly
    public static void loadAllDataTypesInPackage(Class<?> classInPackage) {
        try {
            //System.out.println("loadAllDataTypesInPackage: " + classInPackage.toString());
            for (Class<?> c : Files.getPackageClasses(classInPackage, "", getInstance().pluginLoader == null ? null : getInstance().pluginLoader.getClassLoader())) {
                //c.newInstance();
                //System.out.println(c.toString());
                for (Field f : c.getFields()) {
                    if (Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        @SuppressWarnings("unused")
                        Object o = f.get(null);
                    }
                }
            }
        } catch (Exception e) {
            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), e);
        }
    }

    @CppType("char*") @Const
    private static String getLogDescription() {
        return "Plugins";
    }

    /**
     * @return ClassLoader used for loading plugins
     */
    @JavaOnly
    public ClassLoader getPluginClassLoader() {
        return pluginLoader.getClassLoader();
    }

    /**
     * @return List with modules that can be instantiated in this runtime using the standard mechanism
     */
    public @Ref SimpleList<CreateFrameworkElementAction> getModuleTypes() {
        //Cpp static util::SimpleList<CreateFrameworkElementAction*> moduleTypes;
        return moduleTypes;
    }

    /**
     * Add Module Type
     * (objects won't be deleted by this class)
     *
     * @param cma CreateFrameworkElementAction to add
     */
    public void addModuleType(CreateFrameworkElementAction cma) {
        logDomain.log(LogLevel.LL_DEBUG_VERBOSE_1, getLogDescription(), "Adding module type: " + cma.getName() + " (" + cma.getModuleGroup() + ")");
        getModuleTypes().add(cma);
    }

    @JavaOnly
    /**
     * @param c Class to find jar file of
     * @return Returns jar file that class is in - or class will be in, when compiled
     */
    public String getContainingJarFile(Class<?> c) {
        return pluginLoader.getContainingJarFile(c);
    }

    /*Cpp
    // closes dlopen-ed libraries
    class DLCloser {
    public:
        util::SimpleList<void*> loaded;

        DLCloser() : loaded() {}
        ~DLCloser();
    };
     */

    /**
     * Returns/loads CreateFrameworkElementAction with specified name and specified .so file.
     * (doesn't do any dynamic loading, if .so is already present)
     *
     * @param group Group (.jar or .so)
     * @param name Module type name
     * @return CreateFrameworkElementAction - null if it could not be found
     */
    public CreateFrameworkElementAction loadModuleType(@Const @Ref String group, @Const @Ref String name) {
        // dynamically loaded .so files
        //Cpp static util::SimpleList<util::String> loaded;
        //Cpp static DLCloser dlcloser;

        // try to find module among existing modules
        @Const @Ref SimpleList<CreateFrameworkElementAction> modules = getModuleTypes();
        for (@SizeT int i = 0; i < modules.size(); i++) {
            CreateFrameworkElementAction cma = modules.get(i);
            if (cma.getModuleGroup().equals(group) && cma.getName().equals(name)) {
                return cma;
            }
        }

        /*Cpp
        // hmm... we didn't find it - have we already tried to load .so?
        bool alreadyLoaded = false;
        for (size_t i = 0; i < loaded.size(); i++) {
            if (loaded.get(i).equals(group)) {
                alreadyLoaded = true;
                break;
            }
        }

        if (!alreadyLoaded) {
            loaded.add(group);
            void* handle = _dlopen(group.getCString(), _RTLD_NOW | _RTLD_GLOBAL);
            if (handle) {
                dlcloser.loaded.add(handle);
                return loadModuleType(group, name);
            } else {
                _FINROC_LOG_MESSAGE(rrlib::logging::eLL_ERROR, logDomain, "Error from dlopen: %s", _dlerror());
            }
        }
        */

        logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Could not find/load module " + name + " in " + group);
        return null;
    }

}
