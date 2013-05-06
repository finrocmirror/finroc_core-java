//
// You received this file as part of Finroc
// A Framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//----------------------------------------------------------------------
package org.finroc.core.plugin;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.finroc.core.RuntimeSettings;
import org.finroc.core.util.Files;
import org.rrlib.finroc_core_utils.jc.log.LogUser;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Max Reichardt
 *
 */
public class JavaDebugPluginLoader extends LogUser implements PluginLoader, FilenameFilter {

    /** Finroc repository root */
    private File finrocRepRoot;

    /**
     * Tries to find finroc repository and stores result in finrocRepRoot
     */
    private void findFinrocRepository() {
        if (finrocRepRoot != null) {
            return;
        }

        try {
            finrocRepRoot = RuntimeSettings.getRootDir();
            while (!new File(finrocRepRoot.getAbsolutePath() + File.separator + "sources/java/org/finroc").exists()) {
                finrocRepRoot = finrocRepRoot.getParentFile();
            }
        } catch (NullPointerException e) {

            try {
                finrocRepRoot = new File(".").getAbsoluteFile().getParentFile();
                while (!new File(finrocRepRoot.getAbsolutePath() + File.separator + "sources/java/org/finroc").exists()) {
                    finrocRepRoot = finrocRepRoot.getParentFile();
                }
                return;
            } catch (NullPointerException ne) {
            }

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

                String fh = System.getenv("FINROC_HOME");
                if (fh == null) {
                    System.out.println("Cannot find FINROC_HOME. Please set environment variable or change working directory.");
                    System.exit(0);
                }
                finrocRepRoot = new File(fh);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.finroc.core.plugin.PluginLoader#findPlugins()
     */
    @Override
    public ArrayList<Plugin> findPlugins() {

        ArrayList<Plugin> result = new ArrayList<Plugin>();

        // idea/implementation: search for plugin-classes in make.xml files
        findFinrocRepository();

        // now find all make.xml files
        try {
            List<File> files = Files.getAllFiles(new File(finrocRepRoot + "/sources/java"), this, false, false);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbuilder = factory.newDocumentBuilder();
            for (File file : files) {
                Document doc = dbuilder.parse(file);

                // find FinrocJavaPlugin tags
                NodeList nl = doc.getElementsByTagName("finrocplugin");
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

    @Override
    public String getContainingJarFile(Class<?> c) {
        findFinrocRepository();

        try {
            final String file = c.getName().replace('.', '/') + ".java";
            File found = null;

            // find file in finroc repository
            List<File> files = Files.getAllFiles(new File(finrocRepRoot + "/sources/java"), new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (new File(dir.getPath() + "/" + name).isDirectory()) {
                        return true;
                    }
                    if ((dir.getAbsoluteFile() + "/" + name).endsWith(file)) {
                        return true;
                    }
                    return false;
                }
            }, false, false);

            if (files.size() == 0) {
                log(LogLevel.LL_ERROR, Plugins.logDomain, "Cannot determine jar file name for " + file + ": Not found in finroc repository");
            } else if (files.size() > 1) {
                log(LogLevel.LL_WARNING, Plugins.logDomain, "Problem determining jar file name for " + file + ": Found in finroc repository multiple times (!). Taking first.");
            }
            found = files.get(0);
            String dir = found.getAbsoluteFile().getParent();

            // find parent directory containing make.xml
            while (!new File(dir + "/make.xml").exists()) {
                dir = new File(dir).getParent();
            }

            // parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbuilder = factory.newDocumentBuilder();
            Document doc = dbuilder.parse(dir + "/make.xml");

            // this should be the first target
            NodeList nl = doc.getElementsByTagName("finrocplugin");
            String prefix = "finroc_plugin_";
            if (nl.getLength() == 0) {
                nl = doc.getElementsByTagName("finroclibrary");
                prefix = "finroc_";
            }
            if (nl.getLength() == 0) {
                nl = doc.getElementsByTagName("rrlib");
                prefix = "rrlib_";
            }
            if (nl.getLength() == 0) {
                log(LogLevel.LL_ERROR, Plugins.logDomain, "Can't find suitable target in " + dir + "/make.xml");
                return "unknown binary";
            }
            return prefix + ((Element)nl.item(0)).getAttribute("name") + ".jar";

        } catch (Exception e) {
            log(LogLevel.LL_ERROR, Plugins.logDomain, "Cannot determine jar file name", e);
            return null;
        }
    }
}
