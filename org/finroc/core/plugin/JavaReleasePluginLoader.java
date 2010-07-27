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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.finroc.core.RuntimeSettings;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.NonVirtual;
import org.finroc.jc.container.SimpleList;
import org.finroc.jc.log.LogUser;
import org.finroc.log.LogLevel;
import org.finroc.log.LogStream;

/**
 * @author max
 *
 * Loads plugins when code is compiled and packed in jar files.
 */
@JavaOnly
public class JavaReleasePluginLoader extends LogUser implements PluginLoader {

    /** Class loader for plugins */
    private PluginClassLoader classLoader;

    /* (non-Javadoc)
     * @see org.finroc.core.plugin.PluginLoader#findPlugins()
     */
    @Override
    public SimpleList<Plugin> findPlugins() {

        List<URL> allJars = new ArrayList<URL>();
        List<File> pluginMainJars = new ArrayList<File>();
        SimpleList<Plugin> result = new SimpleList<Plugin>();

        // collect jars
        File rootDir = RuntimeSettings.getRootDir();
        for (File file : rootDir.listFiles()) {
            if (file.getName().startsWith("finroc_plugin_") && file.getName().endsWith(".jar")) {
                pluginMainJars.add(file);
                try {
                    allJars.add(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    log(LogLevel.LL_WARNING, Plugins.logDomain, "Error finding plugin", e);
                }
            }
        }
        addToClassLoader(allJars);

        // load plugins
        for (File pluginJar : pluginMainJars) {
            try {
                //String className = new JarFile(pluginJar).getManifest().getMainAttributes().getValue("Plugin-Class");
                //System.out.println("Found plugin " + filename + "; Plugin class: " + className);
                JarFile jf = new JarFile(pluginJar);
                result.add(loadPlugin(jf.getManifest(), pluginJar.getAbsolutePath()));
                jf.close();
            } catch (Exception e) {
                log(LogLevel.LL_WARNING, Plugins.logDomain, "Error loading plugin: " + pluginJar.getName(), e);
            }
        }

        return result;
    }

    /**
     * Add Plugin to set of Plugins
     *
     * @param pluginClass Fully qualified Plugin class the implements Plugin interface
     * @param jar Jar File that contains ALL files that are necessary for the plugin (including libraries)
     */
    @SuppressWarnings("unchecked")
    public synchronized Plugin loadPlugin(Manifest mf, String jarFile) throws Exception {
        String className = mf.getMainAttributes().getValue("Plugin-Class");
        if (className == null) {
            throw new Exception("No Plugin class specified in " + jarFile);
        }
        Class <? extends Plugin > c = (Class <? extends Plugin >)classLoader.loadClass(className);

        // Is Plugin?
        if (!Plugin.class.isAssignableFrom(c)) {
            throw new Exception(className + " is not a plugin class.");
        }
        log(LogLevel.LL_DEBUG, Plugins.logDomain, "Found plugin: " + className);

        Plugin plugin = c.newInstance();
        return plugin;
    }

    /**
     * Add jar files to class loader. Init class loader if not yet done.
     *
     * @param jars Jar files to add
     */
    private synchronized void addToClassLoader(List<URL> jars) {
        if (classLoader == null) {
            try {
                classLoader = new PluginClassLoader(jars.toArray(new URL[0]));
            } catch (Exception e) {
                log(LogLevel.LL_ERROR, Plugins.logDomain, e);
            }
        } else {
            for (URL url : jars) {
                classLoader.addURL(url);
            }
        }
    }

    /**
     * @author max
     *
     * This class loader is used to load plugins.
     */
    @JavaOnly
    static public class PluginClassLoader extends URLClassLoader {

        /**
         * @param jars Jar URL
         */
        PluginClassLoader(URL[] jars) throws Exception {
            super(jars);
            LogStream ls = Plugins.logDomain.getLogStream(LogLevel.LL_DEBUG, getLogDescription());
            ls.append("Constructed PluginClassLoader: ");
            for (URL url : jars) {
                ls.append(url.toString() + " ");
            }
            ls.close();
        }

        @Override
        protected void addURL(URL url) {
            Plugins.logDomain.log(LogLevel.LL_DEBUG, getLogDescription(), "PluginClassLoader: Adding " + url.toString());
            for (URL u : getURLs()) {
                if (u.equals(url)) {
                    return;
                }
            }
            super.addURL(url);
        }

        @CppType("char*") @Const
        private static String getLogDescription() {
            return "PluginClassLoader";
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
