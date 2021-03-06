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
package org.finroc.core.plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.finroc.core.RuntimeSettings;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.finstructable.FinstructableGroup;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;

import org.finroc.core.util.Files;

/**
 * @author Max Reichardt
 *
 * This class is used for managing the Runtime's plugins
 */
public class Plugins { /*implements HTTPResource*/


    /** Plugins singleton instance */
    private static Plugins instance;

    /** All Plugins that are currently available */
    private final ArrayList<Plugin> plugins = new ArrayList<Plugin>();

    /** List with actions to create external connections */
    private final ArrayList<CreateExternalConnectionAction> externalConnections = new ArrayList<CreateExternalConnectionAction>();

    /** List with actions to create modules */
    private final ArrayList<CreateFrameworkElementAction> moduleTypes = new ArrayList<CreateFrameworkElementAction>();

//    /** Plugin manager instance */
//    private final PluginManager pluginManager = new PluginManager();

    /** Plugin loader implementation */
    private PluginLoader pluginLoader;

    /** Prefixes of plugin jar files to load (contains "finroc_plugins_" by default) */
    private ArrayList<String> prefixesOfPluginsToLoad = new ArrayList<String>();

    /**
     * Loads plugins
     */
    public static void staticInit() {
        Plugins p = getInstance();
        p.findAndLoadPlugins();
    }

    /**
     * @return Plugins singleton instance
     */
    public static Plugins getInstance() {
        if (instance == null) {
            instance = new Plugins();
            instance.prefixesOfPluginsToLoad.add("finroc_plugins_");
        }
        return instance;
    }

    /**
     * Tools/libraries can use this to load additional plugins at program startup.
     * Needs to be called before Finroc runtime environment is instantiated in order to have an effect.
     *
     * @param additionalPrefix Additional .jar file prefix (e.g. "finroc_tools_gui_plugins_") of plugins to load on initialization
     */
    public void addPrefixForPluginsToLoad(String additionalPrefix) {
        prefixesOfPluginsToLoad.add(additionalPrefix);
    }

    private void findAndLoadPlugins() {
        if (RuntimeSettings.isRunningInApplet()) {
            // Load no additional plugins
        } else if (!(RuntimeSettings.isDebugging()) || RuntimeSettings.isRunningInApplet()) {
            pluginLoader = new JavaReleasePluginLoader();
        } else {
            pluginLoader = new JavaDebugPluginLoader();
        }
        loadAllDataTypesInPackage(CoreNumber.class);
        ArrayList<Plugin> plugins = new ArrayList<Plugin>();
        if (pluginLoader != null) {
            loadAllDataTypesInPackage(FinstructableGroup.class);
            plugins = pluginLoader.findPlugins(/*Plugins.class*/);
        }
        for (Plugin plugin : plugins) {
            addPlugin(plugin);
        }
    }

    /**
     * (possibly manually) add plugin
     *
     * @param p Plugin to add
     */
    public void addPlugin(Plugin p) {
        plugins.add(p);
        p.init(/*pluginManager*/);
    }

    /**
     * @return List with modules for external connections
     */
    public ArrayList<CreateExternalConnectionAction> getExternalConnections() {
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
    public ArrayList<Plugin> getPlugins() {
        return plugins;
    }

    /**
     * Load all data types in a certain package
     *
     * @param classInPackage Class in this package
     */
    public static void loadAllDataTypesInPackage(Class<?> classInPackage) {
        if (RuntimeSettings.ANDROID_PLATFORM) { // Does not work on Android platforms
            return;
        }
        try {
            //System.out.println("loadAllDataTypesInPackage: " + classInPackage.toString());
            for (Class<?> c : Files.getPackageClasses(classInPackage, "", getInstance().pluginLoader == null ? null : getInstance().pluginLoader.getClassLoader())) {
                //c.newInstance();
                //System.out.println(c.toString());
                for (Field f : c.getFields()) {
                    if (!RuntimeSettings.isRunningInApplet()) {
                        if (Modifier.isStatic(f.getModifiers())) {
                            f.setAccessible(true);
                            @SuppressWarnings("unused")
                            Object o = f.get(null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.log(LogLevel.WARNING, e);
        }
    }

    /**
     * @return ClassLoader used for loading plugins
     */
    public ClassLoader getPluginClassLoader() {
        return pluginLoader.getClassLoader();
    }

    /**
     * @return List with modules that can be instantiated in this runtime using the standard mechanism
     */
    public ArrayList<CreateFrameworkElementAction> getModuleTypes() {
        return moduleTypes;
    }

    /**
     * Add Module Type
     * (objects won't be deleted by this class)
     *
     * @param cma CreateFrameworkElementAction to add
     */
    public void addModuleType(CreateFrameworkElementAction cma) {
        Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Adding module type: " + cma.getName() + " (" + cma.getModuleGroup() + ")");
        getModuleTypes().add(cma);
    }

    /**
     * @param c Class to find jar file of
     * @return Returns jar file that class is in - or class will be in, when compiled
     */
    public String getContainingJarFile(Class<?> c) {
        return pluginLoader.getContainingJarFile(c);
    }

    /**
     * @return Prefixes of plugin jar files to load
     */
    ArrayList<String> getPrefixesOfPluginsToLoad() {
        return prefixesOfPluginsToLoad;
    }

    /**
     * Returns/loads CreateFrameworkElementAction with specified name and specified .so file.
     * (doesn't do any dynamic loading, if .so is already present)
     *
     * @param group Group (.jar or .so)
     * @param name Module type name
     * @return CreateFrameworkElementAction - null if it could not be found
     */
    public CreateFrameworkElementAction loadModuleType(String group, String name) {

        // try to find module among existing modules
        ArrayList<CreateFrameworkElementAction> modules = getModuleTypes();
        for (int i = 0; i < modules.size(); i++) {
            CreateFrameworkElementAction cma = modules.get(i);
            if (cma.getModuleGroup().equals(group) && cma.getName().equals(name)) {
                return cma;
            }
        }

        Log.log(LogLevel.ERROR, this, "Could not find/load module " + name + " in " + group);
        return null;
    }
}
