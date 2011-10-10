/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010 Max Reichardt,
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
package org.finroc.core.parameter;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCPortDataManager;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataManagerTL;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortDataManager;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.jc.annotation.HAppend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.PostInclude;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.serialization.DataType;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;
import org.rrlib.finroc_core_utils.serialization.StringOutputStream;
import org.rrlib.finroc_core_utils.xml.XMLNode;

/**
 * @author max
 *
 * Annotates ports that are a parameter
 * and provides respective functionality
 */
@PostInclude("rrlib/serialization/DataType.h")
@HAppend( {"extern template class ::rrlib::serialization::DataType<finroc::core::ParameterInfo>;"})
public class ParameterInfo extends FinrocAnnotation {

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<ParameterInfo>(ParameterInfo.class);

    /** Place in Configuration tree, this parameter is configured from (nodes are separated with dots) */
    private String configEntry;

    /** Is this info on remote parameter? */
    @JavaOnly
    private boolean remote = false;

    /** Log domain */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(edgeLog, \"parameter\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("parameter");

    /** Was config entry set from finstruct? */
    private boolean entrySetFromFinstruct;

    public ParameterInfo() {}

    @JavaOnly
    public ParameterInfo(boolean remote) {
        this.remote = remote;
    }

    @Override
    public void serialize(OutputStreamBuffer os) {
        os.writeBoolean(entrySetFromFinstruct);
        os.writeString(configEntry);
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        entrySetFromFinstruct = is.readBoolean();

        //JavaOnlyBlock
        if (remote) {
            configEntry = is.readString();
            return;
        }

        setConfigEntry(is.readString(), entrySetFromFinstruct);
    }

    @Override
    public void serialize(StringOutputStream os) {
        os.append(entrySetFromFinstruct ? '+' : ' ');
        os.append(configEntry);
    }

    @Override
    public void deserialize(StringInputStream is) throws Exception {
        entrySetFromFinstruct = (is.read() == '+');

        //JavaOnlyBlock
        if (remote) {
            configEntry = is.readAll();
            return;
        }

        setConfigEntry(is.readAll(), entrySetFromFinstruct);
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
                ConfigFile cf = ConfigFile.find(ann);
                if (cf == null) {
                    return;
                }
                if (cf.hasEntry(configEntry)) {
                    @Ref XMLNode node = cf.getEntry(configEntry, false);
                    if (FinrocTypeInfo.isCCType(ann.getDataType())) {
                        CCPortBase port = (CCPortBase)ann;
                        CCPortDataManagerTL c = ThreadLocalCache.get().getUnusedBuffer(port.getDataType());
                        try {
                            c.getObject().deserialize(node);
                            port.browserPublishRaw(c);
                        } catch (Exception e) {
                            c.recycleUnused();
                        }
                    } else if (FinrocTypeInfo.isStdType(ann.getDataType())) {
                        PortBase port = (PortBase)ann;
                        PortDataManager pd = port.getUnusedBufferRaw();
                        try {
                            pd.getObject().deserialize(node);
                            port.browserPublish(pd);
                        } catch (Exception e) {
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
                @Ref XMLNode node = cf.getEntry(configEntry, true);
                CCPortDataManager c = port.getInInterThreadContainer();
                c.getObject().serialize(node);
                c.recycle2();
            }
        } else if (FinrocTypeInfo.isStdType(ann.getDataType())) {
            PortBase port = (PortBase)ann;
            if (hasEntry || (!port.containsDefaultValue())) {
                @Ref XMLNode node = cf.getEntry(configEntry, true);
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
}
