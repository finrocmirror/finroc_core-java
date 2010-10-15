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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.finroc.core.RuntimeSettings;
import org.finroc.core.util.Files;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.container.SimpleList;
import org.finroc.jc.log.LogUser;
import org.finroc.log.LogLevel;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author max
 *
 */
@JavaOnly
public class JavaDebugPluginLoader extends LogUser implements PluginLoader, FilenameFilter {

    /** Finroc repository root */
    private File finrocRepRoot;

    /* (non-Javadoc)
     * @see org.finroc.core.plugin.PluginLoader#findPlugins()
     */
    @Override
    public SimpleList<Plugin> findPlugins() {

        SimpleList<Plugin> result = new SimpleList<Plugin>();

        // idea/implementation: search for plugin-classes in make.xml files
        try {
            finrocRepRoot = RuntimeSettings.getRootDir();
            while (!new File(finrocRepRoot.getAbsolutePath() + File.separator + "sources/java/core" + File.separator + "make.xml").exists()) {
                finrocRepRoot = finrocRepRoot.getParentFile();
            }
        } catch (NullPointerException e) {
            // ok, we are in an external location - try reading .project file
            try {
                finrocRepRoot = RuntimeSettings.getRootDir().getParentFile();
                List<String> lines = Files.readLines(new File(finrocRepRoot.getAbsolutePath() + "/.project"));
                for (String line : lines) {
                    line = line.trim();
                    if (line.endsWith("/sources/java/core</location>")) {
                        finrocRepRoot = new File(line.substring(line.indexOf(">") + 1, line.indexOf("/sources/java/core</location>")).trim());
                        break;
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        // now find all make.xml files
        try {
            List<File> files = Files.getAllFiles(new File(finrocRepRoot + "/sources/java"), this, false, false);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbuilder = factory.newDocumentBuilder();
            for (File file : files) {
                Document doc = dbuilder.parse(file);

                // find FinrocJavaPlugin tags
                NodeList nl = doc.getElementsByTagName("finrocjavaplugin");
                for (int i = 0; i < nl.getLength(); i++) {
                    Node n = nl.item(i).getAttributes().getNamedItem("plugin-class");
                    if (n != null) {
                        try {
                            String className = n.getNodeValue();
                            Class<?> c = Class.forName(className);
                            if (!Plugin.class.isAssignableFrom(c)) {
                                throw new Exception(className + " is not a plugin class.");
                            }
                            log(LogLevel.LL_DEBUG, Plugins.logDomain, "Found plugin: " + className);
                            result.add((Plugin)c.newInstance());
                        } catch (Exception e) {
                            log(LogLevel.LL_WARNING, Plugins.logDomain, "Error loading plugin", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log(LogLevel.LL_WARNING, Plugins.logDomain, "Error loading plugins", e);
        }

        return result;
    }

    @Override
    public boolean accept(File dir, String name) {
        File f = new File(dir.getAbsolutePath() + File.separator + name);
        if (f.isDirectory()) {
            if (f.getAbsolutePath().length() > finrocRepRoot.getAbsolutePath().length()) {
                return !f.getName().startsWith(".");
            }
        }
        return name.equals("make.xml");
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }
}
