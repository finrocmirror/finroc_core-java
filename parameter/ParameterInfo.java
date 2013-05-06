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
package org.finroc.core.parameter;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;
import org.rrlib.finroc_core_utils.xml.XML2WrapperException;
import org.rrlib.finroc_core_utils.xml.XMLNode;

/**
 * @author Max Reichardt
 *
 * Annotates ports that are a parameter
 * and provides respective functionality
 */
public class ParameterInfo extends FinrocAnnotation {

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<ParameterInfo>(ParameterInfo.class);

    /**
     * Place in Configuration tree, this parameter is configured from (nodes are separated with '/')
     * (starting with '/' => absolute link - otherwise relative)
     */
    private String configEntry = "";

    /** Is this info on remote parameter? */
    private boolean remote = false;

    /** Log domain */
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("parameter");

    /** Was config entry set from finstruct? */
    private boolean entrySetFromFinstruct;

    /**
     * Command line option to set this parameter
     * (set by outer-most finstructable group)
     */
    private String commandLineOption = "";

    /**
     * Default value set in finstruct (optional)
     * (set by finstructable group responsible for connecting this parameter to attribute tree)
     */
    private String finstructDefault = "";

    public ParameterInfo() {}

    public ParameterInfo(boolean remote) {
        this.remote = remote;
    }

    @Override
    public void serialize(OutputStreamBuffer os) {
        os.writeBoolean(entrySetFromFinstruct);
        os.writeString(configEntry);
        os.writeString(commandLineOption);
        os.writeString(finstructDefault);
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        entrySetFromFinstruct = is.readBoolean();
        String configEntryTmp = is.readString();
        String commandLineOptionTmp = is.readString();
        String finstructDefaultTmp = is.readString();
        boolean same = configEntryTmp.equals(configEntry) && commandLineOptionTmp.equals(commandLineOption) && finstructDefaultTmp.equals(finstructDefault);
        configEntry = configEntryTmp;
        commandLineOption = commandLineOptionTmp;
        finstructDefault = finstructDefaultTmp;

        //JavaOnlyBlock
        if (remote) {
            return;
        }

        if (!same) {
            try {
                loadValue();
            } catch (Exception e) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), e);
            }
        }
    }

    @Override
    public void serialize(XMLNode node) throws Exception {
        serialize(node, false, true);
    }

    public void serialize(XMLNode node, boolean finstructContext, boolean includeCommandLine) {
        assert(!(node.hasAttribute("default") || node.hasAttribute("cmdline") || node.hasAttribute("config")));
        if (configEntry.length() > 0 && (entrySetFromFinstruct || (!finstructContext))) {
            node.setAttribute("config", configEntry);
        }
        if (includeCommandLine) {
            if (commandLineOption.length() > 0) {
                node.setAttribute("cmdline", commandLineOption);
            }
        }
        if (finstructDefault.length() > 0) {
            node.setAttribute("default", finstructDefault);
        }
    }

    @Override
    public void deserialize(XMLNode node) throws Exception {
        deserialize(node, false, true);
    }

    public void deserialize(XMLNode node, boolean finstructContext, boolean includeCommandLine) throws XML2WrapperException {
        if (node.hasAttribute("config")) {
            configEntry = node.getStringAttribute("config");
            entrySetFromFinstruct = finstructContext;
        } else {
            configEntry = "";
        }
        if (includeCommandLine) {
            if (node.hasAttribute("cmdline")) {
                commandLineOption = node.getStringAttribute("cmdline");
            } else {
                commandLineOption = "";
            }
        }
        if (node.hasAttribute("default")) {
            finstructDefault = node.getStringAttribute("default");
        } else {
            finstructDefault = "";
        }
    }

    /**
     * @return Place in Configuration tree, this parameter is configured from (nodes are separated with dots)
     */
    public String getConfigEntry() {
        return configEntry;
    }

    /**
     * (loads value from configuration file, if is exists
     *
     * @param configEntry New Place in Configuration tree, this parameter is configured from (nodes are separated with dots)
     */
    public void setConfigEntry(String configEntry) {
        setConfigEntry(configEntry, false);
    }

    /**
     * (loads value from configuration file, if is exists
     *
     * @param configEntry New Place in Configuration tree, this parameter is configured from (nodes are separated with dots)
     * @param finstructSet Is config entry set from finstruct?
     */
    public void setConfigEntry(String configEntry, boolean finstructSet) {

        //JavaOnlyBlock
        if (remote) {
            this.configEntry = configEntry;
            this.entrySetFromFinstruct = finstructSet;
            return;
        }

        if (!this.configEntry.equals(configEntry)) {
            this.configEntry = configEntry;
            this.entrySetFromFinstruct = finstructSet;
            try {
                loadValue();
            } catch (Exception e) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), e);
            }
        }
    }

    /**
     * load value from configuration file
     */
    public void loadValue() throws Exception {
        loadValue(false);
    }


    /**
     * load value from configuration file
     *
     * @param ignore ready flag?
     */
    public void loadValue(boolean ignoreReady) throws Exception {
        AbstractPort ann = (AbstractPort)getAnnotated();
        synchronized (ann.getRegistryLock()) {
            if (ann != null && (ignoreReady || ann.isReady())) {

                // command line option
                if (commandLineOption.length() > 0) {
                    String arg = RuntimeEnvironment.getInstance().getCommandLineArgument(commandLineOption);
                    if (arg.length() > 0) {
                        StringInputStream sis = new StringInputStream(arg);
                        if (FinrocTypeInfo.isCCType(ann.getDataType())) {
                            CCPortBase port = (CCPortBase)ann;
                            CCPortDataManagerTL c = ThreadLocalCache.get().getUnusedBuffer(port.getDataType());
                            try {
                                c.getObject().deserialize(sis);
                                port.browserPublishRaw(c);
                                return;
                            } catch (Exception e) {
                                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Failed to load parameter '" + ann.getQualifiedName() + "' from command line argument '" + arg + "': ", e);
                                c.recycleUnused();
                            }
                        } else if (FinrocTypeInfo.isStdType(ann.getDataType())) {
                            PortBase port = (PortBase)ann;
                            PortDataManager pd = port.getUnusedBufferRaw();
                            try {
                                pd.getObject().deserialize(sis);
                                port.browserPublish(pd);
                                return;
                            } catch (Exception e) {
                                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Failed to load parameter '" + ann.getQualifiedName() + "' from command line argument '" + arg + "': ", e);
                                pd.recycleUnused();
                            }
                        } else {
                            throw new RuntimeException("Port Type not supported as a parameter");
                        }
                    }
                }

                // config file entry
                ConfigFile cf = ConfigFile.find(ann);
                if (cf != null && configEntry.length() > 0) {
                    String fullConfigEntry = ConfigNode.getFullConfigEntry(ann, configEntry);
                    if (cf.hasEntry(fullConfigEntry)) {
                        XMLNode node = cf.getEntry(fullConfigEntry, false);
                        if (FinrocTypeInfo.isCCType(ann.getDataType())) {
                            CCPortBase port = (CCPortBase)ann;
                            CCPortDataManagerTL c = ThreadLocalCache.get().getUnusedBuffer(port.getDataType());
                            try {
                                c.getObject().deserialize(node);
                                port.browserPublishRaw(c);
                                return;
                            } catch (Exception e) {
                                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Failed to load parameter '" + ann.getQualifiedName() + "' from config entry '" + fullConfigEntry + "': ", e);
                                c.recycleUnused();
                            }
                        } else if (FinrocTypeInfo.isStdType(ann.getDataType())) {
                            PortBase port = (PortBase)ann;
                            PortDataManager pd = port.getUnusedBufferRaw();
                            try {
                                pd.getObject().deserialize(node);
                                port.browserPublish(pd);
                                return;
                            } catch (Exception e) {
                                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Failed to load parameter '" + ann.getQualifiedName() + "' from config entry '" + fullConfigEntry + "': ", e);
                                pd.recycleUnused();
                            }
                        } else {
                            throw new RuntimeException("Port Type not supported as a parameter");
                        }
                    }
                }

                // finstruct default
                if (finstructDefault.length() > 0) {
                    StringInputStream sis = new StringInputStream(finstructDefault);
                    if (FinrocTypeInfo.isCCType(ann.getDataType())) {
                        CCPortBase port = (CCPortBase)ann;
                        CCPortDataManagerTL c = ThreadLocalCache.get().getUnusedBuffer(port.getDataType());
                        try {
                            c.getObject().deserialize(sis);
                            port.browserPublishRaw(c);
                            return;
                        } catch (Exception e) {
                            logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Failed to load parameter '" + ann.getQualifiedName() + "' from finstruct default '" + finstructDefault + "': ", e);
                            c.recycleUnused();
                        }
                    } else if (FinrocTypeInfo.isStdType(ann.getDataType())) {
                        PortBase port = (PortBase)ann;
                        PortDataManager pd = port.getUnusedBufferRaw();
                        try {
                            pd.getObject().deserialize(sis);
                            port.browserPublish(pd);
                            return;
                        } catch (Exception e) {
                            logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Failed to load parameter '" + ann.getQualifiedName() + "' from finstruct default '" + finstructDefault + "': ", e);
                            pd.recycleUnused();
                        }
                    } else {
                        throw new RuntimeException("Port Type not supported as a parameter");
                    }
                }
            }
        }
    }

    /**
     * save value to configuration file
     * (if value equals default value and entry does not exist, no entry is written to file)
     */
    public void saveValue() throws Exception {
        AbstractPort ann = (AbstractPort)getAnnotated();
        if (ann == null || (!ann.isReady())) {
            return;
        }
        ConfigFile cf = ConfigFile.find(ann);
        boolean hasEntry = cf.hasEntry(configEntry);
        if (FinrocTypeInfo.isCCType(ann.getDataType())) {
            CCPortBase port = (CCPortBase)ann;
            if (hasEntry || (!port.containsDefaultValue())) {
                XMLNode node = cf.getEntry(configEntry, true);
                CCPortDataManager c = port.getInInterThreadContainer();
                c.getObject().serialize(node);
                c.recycle2();
            }
        } else if (FinrocTypeInfo.isStdType(ann.getDataType())) {
            PortBase port = (PortBase)ann;
            if (hasEntry || (!port.containsDefaultValue())) {
                XMLNode node = cf.getEntry(configEntry, true);
                PortDataManager pd = port.getLockedUnsafeRaw();
                pd.getObject().serialize(node);
                pd.releaseLock();
            }
        } else {
            throw new RuntimeException("Port Type not supported as a parameter");
        }
    }

    @Override
    protected void annotatedObjectInitialized() {
        try {
            loadValue(true);
        } catch (Exception e) {
            log(LogLevel.LL_ERROR, FrameworkElement.logDomain, e);
        }
    }

    /**
     * @return Is config entry set from finstruct/xml?
     */
    public boolean isConfigEntrySetFromFinstruct() {
        return entrySetFromFinstruct;
    }

    /**
     * @return Command line option to set this parameter
     * (set by outer-most finstructable group)
     */
    public String getCommandLineOption() {
        return commandLineOption;
    }

    /**
     * @param commandLineOption Command line option to set this parameter
     * (set by outer-most finstructable group)
     */
    public void setCommandLineOption(String commandLineOption) {
        this.commandLineOption = commandLineOption;
    }

    /**
     * @return Default value set in finstruct (optional)
     * (set by finstructable group responsible for connecting this parameter to attribute tree)
     */
    public String getFinstructDefault() {
        return finstructDefault;
    }

    /**
     * @param finstructDefault Default value set in finstruct.
     * (set by finstructable group responsible for connecting this parameter to attribute tree)
     */
    public void setFinstructDefault(String finstructDefault) {
        this.finstructDefault = finstructDefault;
    }

    /**
     * @return Does parameter have any non-default info relevant for finstructed group?
     */
    public boolean hasNonDefaultFinstructInfo() {
        return (configEntry.length() > 0 && entrySetFromFinstruct) || commandLineOption.length() > 0 || finstructDefault.length() > 0;
    }
}
