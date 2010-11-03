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
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortDataContainer;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortData;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.portdatabase.DataTypeRegister;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * Annotates ports that are a parameter
 * and provides respective functionality
 */
public class ParameterInfo extends FinrocAnnotation {

    /** Data Type */
    public static DataType TYPE = DataTypeRegister.getInstance().getDataType(ParameterInfo.class);

    /** Place in Configuration tree, this parameter is configured from (nodes are separated with dots) */
    private String configEntry;

    /** Log domain */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(edgeLog, \"parameter\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("parameter");

    @Override
    public void serialize(CoreOutput os) {
        os.writeString(configEntry);
    }

    @Override
    public void deserialize(CoreInput is) {
        setConfigEntry(is.readString());
    }

    @Override
    public String serialize() {
        return configEntry;
    }

    @Override
    public void deserialize(String s) throws Exception {
        setConfigEntry(s);
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
                    XMLNode node = cf.getEntry(configEntry, false);
                    if (ann.getDataType().isCCType()) {
                        CCPortBase port = (CCPortBase)ann;
                        CCPortDataContainer<?> c = ThreadLocalCache.get().getUnusedBuffer(port.getDataType());
                        c.deserialize(node);
                        port.browserPublish(c);
                    } else if (ann.getDataType().isStdType()) {
                        PortBase port = (PortBase)ann;
                        PortData pd = port.getUnusedBufferRaw();
                        pd.deserialize(node);
                        port.browserPublish(pd);
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
        if (ann.getDataType().isCCType()) {
            CCPortBase port = (CCPortBase)ann;
            if (hasEntry || (!port.containsDefaultValue())) {
                XMLNode node = cf.getEntry(configEntry, true);
                CCInterThreadContainer<?> c = port.getInInterThreadContainer();
                c.serialize(node);
                c.recycle2();
            }
        } else if (ann.getDataType().isStdType()) {
            PortBase port = (PortBase)ann;
            if (hasEntry || (!port.containsDefaultValue())) {
                XMLNode node = cf.getEntry(configEntry, true);
                @Const PortData pd = port.getLockedUnsafeRaw();
                pd.serialize(node);
                pd.getManager().releaseLock();
            }
        } else {
            throw new RuntimeException("Port Type not supported as a parameter");
        }
    }




}
