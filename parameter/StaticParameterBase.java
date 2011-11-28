/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2010-2011 Max Reichardt,
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

import org.finroc.core.CoreFlags;
import org.finroc.core.FrameworkElement;
import org.finroc.core.RuntimeEnvironment;
import org.finroc.core.finstructable.FinstructableGroup;
import org.finroc.core.portdatabase.SerializationHelper;
import org.rrlib.finroc_core_utils.jc.HasDestructor;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.ConstMethod;
import org.rrlib.finroc_core_utils.jc.annotation.CppDefault;
import org.rrlib.finroc_core_utils.jc.annotation.Friend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.Managed;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.SizeT;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;
import org.rrlib.finroc_core_utils.serialization.GenericObject;
import org.rrlib.finroc_core_utils.serialization.InputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;
import org.rrlib.finroc_core_utils.serialization.Serialization;
import org.rrlib.finroc_core_utils.serialization.StringInputStream;
import org.rrlib.finroc_core_utils.serialization.TypedObject;
import org.rrlib.finroc_core_utils.xml.XMLNode;

/**
 * @author max
 *
 * Static Parameter class
 * (Generic base class without template type)
 */
@Ptr @Friend(StaticParameterList.class)
public class StaticParameterBase implements HasDestructor {

    /** Name of parameter */
    private String name;

    /** DataType of parameter */
    private DataTypeBase type;

    /** Current parameter value (in CreateModuleAction-prototypes this is null) */
    private @Ptr GenericObject value;

    /** Last parameter value (to detect whether value has changed) */
    private @Ptr GenericObject lastValue;

    /** Is current value enforced (typically hard-coded)? In this case, any config file entries or command line parameters are ignored */
    private boolean enforceCurrentValue;

    /**
     * StaticParameterBase whose value buffer to use.
     * Typically, this is set to this.
     * However, it is possible to attach this parameter to another (outer) parameter.
     * In this case they share the same buffer: This parameter uses useValueOf.valPointer(), too.
     */
    private @Ptr StaticParameterBase useValueOf = this;

    /** Index in parameter list */
    protected int listIndex;

    /** If this is a remote parameter: value as string */
    @JavaOnly
    private String remoteValue;

    /** Is this a remote parameter? */
    @JavaOnly
    private final boolean remote;

    /**
     * Command line option to set this parameter
     * (set by finstructable group containing module with this parameter)
     */
    private String commandLineOption = "";

    /**
     * Name of outer parameter if parameter is configured by static parameter of finstructable group
     * (usually set by finstructable group containing module with this parameter)
     */
    private String outerParameterAttachment = "";

    /** Create outer parameter if it does not exist yet? (Otherwise an error message is displayed. Only true, when edited with finstruct.) */
    private boolean createOuterParameter;

    /**
     * Place in Configuration tree, this parameter is configured from (nodes are separated with '/')
     * (usually set by finstructable group containing module with this parameter)
     * (starting with '/' => absolute link - otherwise relative)
     */
    private String configEntry = "";

    /** Was configEntry set by finstruct? */
    private boolean configEntrySetByFinstruct;

    /** Is this a proxy for other static parameters? (as used in finstructable groups) */
    private boolean structureParameterProxy;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"parameters\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("parameters");

    /** Constructor for remote parameters */
    @JavaOnly
    public StaticParameterBase() {
        remote = true;
    }

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     * @param constructorPrototype Is this a CreteModuleActionPrototype (no buffer will be allocated)
     */
    public StaticParameterBase(String name, DataTypeBase type, boolean constructorPrototype) {
        this(name, type, constructorPrototype, false);
    }

    /**
     * @param name Name of parameter
     * @param type DataType of parameter
     * @param constructorPrototype Is this a CreteModuleActionPrototype (no buffer will be allocated)
     */
    public StaticParameterBase(String name, DataTypeBase type, boolean constructorPrototype, @CppDefault("false") boolean structureParameterProxy) {
        this.name = name;
        this.type = type;
        this.structureParameterProxy = structureParameterProxy;

        if ((!constructorPrototype) && type.getInfo() != null) {
            createBuffer(type);
        }

        //JavaOnlyBlock
        remote = false;
    }

    public void serialize(OutputStreamBuffer os) {
        os.writeString(name);
        os.writeType(type);
        os.writeString(commandLineOption);
        os.writeString(outerParameterAttachment);
        os.writeBoolean(createOuterParameter);
        os.writeString(configEntry);
        os.writeBoolean(configEntrySetByFinstruct);
        os.writeBoolean(enforceCurrentValue);
        TypedObject val = valPointer();

        //JavaOnlyBlock
        if (remoteValue != null) {
            os.writeBoolean(true);
            os.writeString(remoteValue);
            return;
        }

        os.writeBoolean(val != null);
        if (val != null) {
            os.writeString(SerializationHelper.typedStringSerialize(type, val));
        }
    }

    public void deserialize(InputStreamBuffer is, FrameworkElement parameterized) {
        if (remoteValue()) {

            //JavaOnlyBlock
            name = is.readString();
            type = is.readType();

            //Cpp assert(false && "not supported");
        } else {
            is.readString();
            is.readType();
        }

        String commandLineOptionTmp = is.readString();
        outerParameterAttachment = is.readString();
        createOuterParameter = is.readBoolean();
        String configEntryTmp = is.readString();
        configEntrySetByFinstruct = is.readBoolean();
        enforceCurrentValue = is.readBoolean();
        updateOuterParameterAttachment(parameterized);
        updateAndPossiblyLoad(commandLineOptionTmp, configEntryTmp, parameterized);

        if (is.readBoolean()) {
            try {
                set(is.readString());
            } catch (Exception e) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), e);
            }
        }
    }

    /**
     * Set commandLineOption and configEntry.
     * Check if they changed and possibly load value.
     */
    private void updateAndPossiblyLoad(String commandLineOptionTmp, String configEntryTmp, FrameworkElement parameterized) {
        boolean cmdlineChanged = !commandLineOption.equals(commandLineOptionTmp);
        boolean configEntryChanged = !configEntry.equals(configEntryTmp);
        commandLineOption = commandLineOptionTmp;
        configEntry = configEntryTmp;

        if (useValueOf == this && (cmdlineChanged || configEntryChanged)) {
            loadValue(parameterized);
        }
    }

    /**
     * Check whether change to outerParameterAttachment occured and perform any
     * changes required.
     *
     * @param parameterized Parameterized framework element.
     */
    private void updateOuterParameterAttachment(FrameworkElement parameterized) {
        if (parameterized == null) {
            return;
        }
        if (outerParameterAttachment.length() == 0) {
            if (useValueOf != this) {
                attachTo(this);
            }
        } else {
            StaticParameterBase sp = getParameterWithBuffer();
            if (!sp.getName().equals(outerParameterAttachment)) {

                // find parameter to attach to
                FrameworkElement fg = parameterized.getParentWithFlags(CoreFlags.FINSTRUCTABLE_GROUP);
                if (fg == null) {
                    logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "No parent finstructable group. Ignoring...");
                    return;
                }

                StaticParameterList spl = StaticParameterList.getOrCreate(fg);
                for (@SizeT int i = 0; i < spl.size(); i++) {
                    sp = spl.get(i);
                    if (sp.getName().equals(outerParameterAttachment)) {
                        attachTo(sp);
                        return;
                    }
                }

                if (createOuterParameter) {
                    sp = new StaticParameterBase(outerParameterAttachment, type, false, true);
                    attachTo(sp);
                    spl.add(sp);
                    logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Creating proxy parameter '" + outerParameterAttachment + "' in '" + fg.getQualifiedName() + "'.");
                } else {
                    logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "No parameter named '" + outerParameterAttachment + "' found in parent group.");
                }
            }
        }
    }

    /**
     * @return Log description
     */
    @JavaOnly
    public String getLogDescription() {
        return name;
    }

    @Override
    public void delete() {
    }

    /**
     * @return Value serialized as string (reverse operation to set)
     */
    public String serializeValue() {
        return SerializationHelper.typedStringSerialize(type, valPointer());
    }

    /**
     * (Internal helper function to make expressions shorter)
     *
     * @return value or ccValue, depending on data type
     */
    @ConstMethod
    public GenericObject valPointer() {
        return getParameterWithBuffer().value;
    }

    /**
     * @param s serialized as string
     */
    public void set(@Const @Ref String s) throws Exception {
        if (remoteValue()) {

            //JavaOnlyBlock
            // try parsing value - if the type is available locally
            if (type != null) {
                DataTypeBase dt = SerializationHelper.getTypedStringDataType(type, s);
                TypedObject val = valPointer();
                if (val == null || val.getType() != dt) {
                    createBuffer(dt);
                    val = valPointer();
                }
                val.deserialize(new StringInputStream(s));
                remoteValue = null;
            } else {
                remoteValue = s;
                value = null;
            }

        } else {
            assert(type != null);
            DataTypeBase dt = SerializationHelper.getTypedStringDataType(type, s);
            TypedObject val = valPointer();
            if (val.getType() != dt) {
                createBuffer(dt);
                val = valPointer();
            }

            StringInputStream sis = new StringInputStream(s);
            val.deserialize(sis);
        }
    }

    /**
     * Create buffer of specified type
     * (and delete old buffer)
     *
     * @param type Type
     */
    private void createBuffer(DataTypeBase type) {
        StaticParameterBase sp = getParameterWithBuffer();

        //Cpp delete sp->value;
        sp.value = type.createInstanceGeneric(null);
        assert(sp.value != null);
    }

    /**
     * @return Is this a remote parameter?
     */
    @InCpp("return false;")
    private boolean remoteValue() {
        return remote;
    }

    /*Cpp
    const char* getLogDescription() {
        return name.getCString();
    }
     */

    /**
     * @return Name of parameter
     */
    public String getName() {
        return name;
    }

    /**
     * @return DataType of parameter
     */
    public DataTypeBase getType() {
        return type;
    }

    /**
     * @return Remote value as string if data type is null
     */
    @JavaOnly
    public Object getRemoteValue() {
        return remoteValue;
    }

    public void serialize(XMLNode node, boolean finstructContext) throws Exception {
        assert(!(node.hasAttribute("type") || node.hasAttribute("cmdline") || node.hasAttribute("config") || node.hasAttribute("attachouter")));
        TypedObject val = valPointer();
        if (val.getType() != type || structureParameterProxy) {
            node.setAttribute("type", val.getType().getName());
        }
        if (enforceCurrentValue) {
            node.setAttribute("enforcevalue", true);
        }
        val.serialize(node);

        if (commandLineOption.length() > 0) {
            node.setAttribute("cmdline", commandLineOption);
        }
        if (outerParameterAttachment.length() > 0) {
            node.setAttribute("attachouter", outerParameterAttachment);
        }
        if (configEntry.length() > 0 && (configEntrySetByFinstruct || (!finstructContext))) {
            node.setAttribute("config", configEntry);
        }
    }

    public void deserialize(XMLNode node, boolean finstructContext, FrameworkElement parameterized) throws Exception {
        DataTypeBase dt = type;
        if (node.hasAttribute("type")) {
            dt = DataTypeBase.findType(node.getStringAttribute("type"));
        }
        enforceCurrentValue = node.hasAttribute("enforcevalue") && node.getBoolAttribute("enforcevalue");
        TypedObject val = valPointer();
        if (val == null || val.getType() != dt) {
            createBuffer(dt);
            val = valPointer();
        }
        val.deserialize(node);

        String commandLineOptionTmp;
        if (node.hasAttribute("cmdline")) {
            commandLineOptionTmp = node.getStringAttribute("cmdline");
        } else {
            commandLineOptionTmp = "";
        }
        if (node.hasAttribute("attachouter")) {
            outerParameterAttachment = node.getStringAttribute("attachouter");
            updateOuterParameterAttachment(parameterized);
        } else {
            outerParameterAttachment = "";
            updateOuterParameterAttachment(parameterized);
        }
        String configEntryTmp;
        if (node.hasAttribute("config")) {
            configEntryTmp = node.getStringAttribute("config");
            configEntrySetByFinstruct = finstructContext;
        } else {
            configEntryTmp = "";
        }

        updateAndPossiblyLoad(commandLineOptionTmp, configEntryTmp, parameterized);
    }

    /**
     * (should be overridden by subclasses)
     * @return Deep copy of parameter (without value)
     */
    public @Managed StaticParameterBase deepCopy() {
        throw new RuntimeException("Unsupported");
    }

    /**
     * @return Command line option to set this parameter
     * (set by finstructable group containing module with this parameter)
     */
    @JavaOnly
    public String getCommandLineOption() {
        return commandLineOption;
    }

    /**
     * @param commandLineOption Command line option to set this parameter
     * (set by finstructable group containing module with this parameter)
     */
    @JavaOnly
    public void setCommandLineOption(String commandLineOption) {
        this.commandLineOption = commandLineOption;
    }

    /**
     * @return Name of outer parameter if parameter is configured by static parameter of finstructable group
     * (set by finstructable group containing module with this parameter)
     */
    @JavaOnly
    public String getOuterParameterAttachment() {
        return outerParameterAttachment;
    }

    /**
     * @param outerParameterAttachment Name of outer parameter of finstructable group to configure parameter with.
     * (set by finstructable group containing module with this parameter)
     * @param createOuter Create outer parameter if it does not exist yet?
     */
    @JavaOnly
    public void setOuterParameterAttachment(String outerParameterAttachment, @CppDefault("false") boolean createOuter) {
        this.outerParameterAttachment = outerParameterAttachment;
        createOuterParameter = createOuter;
    }

    /**
     * @return Place in Configuration tree, this parameter is configured from
     * (set by finstructable group containing module with this parameter)
     */
    @JavaOnly
    public String getConfigEntry() {
        return configEntry;
    }

    /**
     * @return Was config entry set by finstruct?
     */
    @JavaOnly
    public boolean isConfigEntrySetByFinstruct() {
        return configEntrySetByFinstruct;
    }

    /**
     * @param configEntry Place in Configuration tree, this parameter is configured from
     * (set by finstructable group containing module with this parameter)
     * @param finstructSet Is outer parameter attachment set by finstruct?
     */
    @JavaOnly
    public void setConfigEntry(String configEntry, @CppDefault("false") boolean finstructSet) {
        this.configEntry = configEntry;
        configEntrySetByFinstruct = finstructSet;
    }

    /**
     * @return Is this a proxy for other static parameters? (only used in finstructable groups)
     */
    public boolean isStaticParameterProxy() {
        return structureParameterProxy;
    }

    /**
     * Internal helper method to get parameter containing buffer we are using/sharing.
     *
     * @return Parameter containing buffer we are using/sharing.
     */
    private StaticParameterBase getParameterWithBuffer() {
        if (useValueOf == this) {
            return this;
        }
        return useValueOf.getParameterWithBuffer();
    }

    /**
     * Attach this static parameter to another one.
     * They will share the same value/buffer.
     *
     * @param other Other parameter to attach this one to. Use null or this to detach.
     */
    public void attachTo(StaticParameterBase other) {
        useValueOf = other == null ? this : other;
        StaticParameterBase sp = getParameterWithBuffer();
        if (sp.type.getInfo() == null) {
            sp.type = type;
        }
        if (sp.value == null) {
            createBuffer(sp.type);

            if (sp != this) {
                // Swap buffers to have something sensible in it
                GenericObject tmp = sp.value;
                sp.value = value;
                value = tmp;
            }
        }
    }

    /**
     * Reset "changed flag".
     * The current value will now be the one any new value is compared with when
     * checking whether value has changed.
     */
    public void resetChanged() {
        StaticParameterBase sp = getParameterWithBuffer();

        if (sp.lastValue == null || sp.lastValue.getType() != sp.value.getType()) {
            sp.lastValue = sp.value.getType().createInstanceGeneric(null);
            assert(sp.lastValue != null);
        }

        Serialization.deepCopy(sp.value, sp.lastValue, null);
    }

    /**
     * @return Has parameter changed since last call to "resetChanged" (or creation).
     */
    public boolean hasChanged() {
        StaticParameterBase sp = getParameterWithBuffer();
        return !Serialization.equals(sp.value, sp.lastValue);
    }

    /**
     * Load value (from any config file entries or command line or whereever)
     *
     * @param parent Parent framework element
     */
    public void loadValue(FrameworkElement parent) {
        if (!enforceCurrentValue) {

            // command line
            FrameworkElement fg = parent.getParentWithFlags(CoreFlags.FINSTRUCTABLE_GROUP);
            if (commandLineOption.length() > 0 && (fg == null || fg.getParent() == RuntimeEnvironment.getInstance())) { // outermost group?
                String arg = RuntimeEnvironment.getInstance().getCommandLineArgument(commandLineOption);
                if (arg.length() > 0) {
                    try {
                        set(arg);
                        return;
                    } catch (Exception e) {
                        logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Failed to load parameter '" + getName() + "' from command line argument '" + arg + "': ", e);
                    }
                }
            }

            // config entry
            if (configEntry.length() > 0) {
                if (configEntrySetByFinstruct) {
                    if (fg == null || (!((FinstructableGroup)fg).isResponsibleForConfigFileConnections(parent))) {
                        return;
                    }
                }
                ConfigFile cf = ConfigFile.find(parent);
                String fullConfigEntry = ConfigNode.getFullConfigEntry(parent, configEntry);
                if (cf != null) {
                    if (cf.hasEntry(fullConfigEntry)) {
                        @Ref XMLNode node = cf.getEntry(fullConfigEntry, false);
                        try {
                            value.deserialize(node);
                        } catch (Exception e) {
                            logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Failed to load parameter '" + getName() + "' from config entry '" + fullConfigEntry + "': ", e);
                        }
                    }
                }
            }
        }
    }
}
