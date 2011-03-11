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
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.serialization.DataType;
import org.finroc.serialization.InputStreamBuffer;
import org.finroc.serialization.OutputStreamBuffer;
import org.finroc.serialization.StringInputStream;
import org.finroc.serialization.StringOutputStream;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * Annotates ports that are a parameter
 * and provides respective functionality
 */
public class ParameterInfo extends FinrocAnnotation {

    /** Data Type */
    public final static DataType<ParameterInfo> TYPE = new DataType<ParameterInfo>(ParameterInfo.class);

    /** Place in Configuration tree, this parameter is configured from (nodes are separated with dots) */
    private String configEntry;

    /** Log domain */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(edgeLog, \"parameter\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("parameter");

    @Override
    public void serialize(OutputStreamBuffer os) {
        os.writeString(configEntry);
    }

    @Override
    public void deserialize(InputStreamBuffer is) {
        setConfigEntry(is.readString());
    }

    @Override
    public void serialize(StringOutputStream os) {
        os.append(configEntry);
    }

    @Override
    public void deserialize(StringInputStream is) throws Exception {
        setConfigEntry(is.readAll());
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
        if (!this.configEntry.equals(configEntry)) {
            this.configEntry = configEntry;
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
                            c.setRefCounter(1);
                            c.releaseLock();
                        }
                    } else if (FinrocTypeInfo.isStdType(ann.getDataType())) {
                        PortBase port = (PortBase)ann;
                        PortDataManager pd = port.getUnusedBufferRaw();
                        try {
                            pd.getObject().deserialize(node);
                            port.browserPublish(pd);
                        } catch (Exception e) {
                            pd.getCurrentRefCounter().setOrAddLock();
                            pd.releaseLock();
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
}
