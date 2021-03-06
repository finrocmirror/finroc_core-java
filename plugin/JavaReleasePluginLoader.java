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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.finroc.core.RuntimeSettings;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.logging.LogStream;
import org.rrlib.serialization.Serialization;

/**
 * @author Max Reichardt
 *
 * Loads plugins when code is compiled and packed in jar files.
 */
public class JavaReleasePluginLoader implements PluginLoader {

    /** Class loader for plugins */
    private PluginClassLoader classLoader;

    /** Pattern to extract jar file */
    private static Pattern JAR_FILE = Pattern.compile(".*/(.*[.]jar)!/.*");

    /* (non-Javadoc)
     * @see org.finroc.core.plugin.PluginLoader#findPlugins()
     */
    @Override
    public ArrayList<Plugin> findPlugins() {

        List<URL> allJars = new ArrayList<URL>();
        List<File> pluginMainJars = new ArrayList<File>();
        ArrayList<Plugin> result = new ArrayList<Plugin>();

        // collect jars
        File rootDir = RuntimeSettings.getRootDir();
        for (File file : rootDir.listFiles()) {
            for (String validPrefix : Plugins.getInstance().getPrefixesOfPluginsToLoad()) {
                if (file.getName().startsWith(validPrefix) && file.getName().endsWith(".jar")) {
                    pluginMainJars.add(file);
                    try {
                        allJars.add(file.toURI().toURL());
                    } catch (MalformedURLException e) {
                        Log.log(LogLevel.WARNING, "Error finding plugin", e);
                    }
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
                Log.log(LogLevel.WARNING, "Error loading plugin: " + pluginJar.getName(), e);
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
        Log.log(LogLevel.DEBUG, "Found plugin: " + className);

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
                Log.log(LogLevel.ERROR, e);
            }
        } else {
            for (URL url : jars) {
                classLoader.addURL(url);
            }
        }
    }

    /**
     * @author Max Reichardt
     *
     * This class loader is used to load plugins.
     */
    static public class PluginClassLoader extends URLClassLoader {

        /**
         * @param jars Jar URL
         */
        PluginClassLoader(URL[] jars) throws Exception {
            super(jars);
            LogStream ls = Log.getLogStream(LogLevel.DEBUG, "PluginClassLoader");
            ls.append("Constructed PluginClassLoader: ");
            for (URL url : jars) {
                ls.append(url.toString() + " ");
            }
            ls.close();
            Serialization.setDeepCopyClassLoader(this);
        }

        @Override
        protected void addURL(URL url) {
            Log.log(LogLevel.DEBUG, "PluginClassLoader: Adding " + url.toString());
            for (URL u : getURLs()) {
                if (u.equals(url)) {
                    return;
                }
            }
            super.addURL(url);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public String getContainingJarFile(Class<?> c) {
        String url = "";
        try {
            url = c.getResource(c.getSimpleName() + ".class").toURI().toURL().toString();
            Matcher m = JAR_FILE.matcher(url);
            if (!m.matches()) {
                throw new Exception("Cannot extract jar file");
            }
            return m.group(1);
        } catch (Exception e) {
            Log.log(LogLevel.ERROR, "Error extracting jar file from URL " + url, e);
        }
        return null;
    }
}
