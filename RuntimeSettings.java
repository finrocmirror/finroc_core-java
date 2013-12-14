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
package org.finroc.core;

import java.io.File;
import java.nio.ByteOrder;

import org.rrlib.serialization.rtti.DataTypeBase;

import org.finroc.core.datatype.Bounds;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.parameter.ParameterBool;
import org.finroc.core.parameter.ParameterNumeric;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.PortListener;

import org.finroc.core.port.net.UpdateTimeChangeListener;
import org.finroc.core.util.Files;

/**
 * @author Max Reichardt
 *
 * Contains global settings of runtime environment.
 *
 * For variable settings it contains ports inside a module.
 *
 * staticInit() should be called after runtime and data types have been initialized.
 */
public class RuntimeSettings extends FrameworkElement implements PortListener<CoreNumber> {

    /** Singleton Instance */
    private static RuntimeSettings inst = null;

    /**
     * There's a Java and C++ version of this framework. Java modules can
     * be used in both. This flag indicates, whether the Java modules are
     * used together with a C++ core.
     */
    public static final boolean CPP_CORE = false;

    /** Display warning, if loop times of CoreLoopThreads are exceeded? */
    public static ParameterBool WARN_ON_CYCLE_TIME_EXCEED;

    /** Default cycle time of CoreLoopThreads in ms*/
    public static ParameterNumeric<Long> DEFAULT_CYCLE_TIME;

    /** Default number of event threads */
    //public static final IntSetting NUM_OF_EVENT_THREADS = inst.add("NUM_OF_EVENT_THREADS", 2, false);

    /** Default minimum network update time (ms) */
    public static ParameterNumeric<Integer> DEFAULT_MINIMUM_NETWORK_UPDATE_TIME;

    public static final int EDGE_LIST_DEFAULT_SIZE = 0;
    public static final int EDGE_LIST_SIZE_INCREASE_FACTOR = 2;

    /** Is Finroc running on Android platform? */
    public final static boolean ANDROID_PLATFORM = System.getProperty("java.vm.vendor").equals("The Android Project");

    /** Absolute Root Directory of Runtime (location of finroc_core.jar) */
    private final static File rootDir = new File(Files.getRootDir(RuntimeSettings.class)); //new File(new File(Files.getRootDir(RuntimeSettings.class)).getParentFile().getAbsolutePath());

    /** Is runtime executed as .class files (usually in Debug mode) or in .jar file */
    private static Boolean debugging;
    // new File(Files.getRootDir(RuntimeSettings.class)).getName().equals("bin") || new File(Files.getRootDir(RuntimeSettings.class) + "/org/finroc/core/RuntimeSettings.java").exists();

    /** Loop time for buffer tracker (in ms) */
    //public static final IntSetting BUFFER_TRACKER_LOOP_TIME = inst.add("BUFFER_TRACKER_LOOP_TIME", 140, true);

    /** Cycle time for stream thread */
    public static ParameterNumeric<Integer> STREAM_THREAD_CYCLE_TIME;

    /** Is runtime instantiated in Java Applet */
    private static boolean runningInApplet;

    /**
     * Period in ms after which garbage collector will delete objects... any threads
     * still working on objects while creating deletion task should be finished by then
     */
    public static ParameterNumeric<Integer> GARBAGE_COLLECTOR_SAFETY_PERIOD;

    /** ByteOrder of host that runtime is running on */
    //@JavaOnly public static ByteOrder byteOrder = processByteOrderString(ConfigFile.getInstance().getString("BYTE_ORDER", "native"));
    public static ByteOrder byteOrder = ByteOrder.nativeOrder();

    /** Collect edge statistics ? */
    public static final boolean COLLECT_EDGE_STATISTICS = false;

    /**
     * Should cc ports be used in backend?
     * (This is mainly an optimization. In non-data-intensive/non-runtime-critical applications (such as tooling) this may be disabled to reduce memory footprint.)
     */
    private static boolean useCCPorts = true;

    /**
     * Determines maximum number of ports in CoreRegister (2 ^ maxCoreRegisterIndexBits)
     */
    private static int maxCoreRegisterIndexBits = 16;

    /**
     * Is creation of framework elements with the same qualified names allowed?
     * (by default it is not, because this causes undefined behaviour with port connections by-string
     *  when ports have the same names (e.g. in fingui and in finstructable groups)
     */
    private static boolean duplicateQualifiedNamesAllowed;

    /** List with listeners for update times */
    private final UpdateTimeChangeListener.Manager updateTimeListener = new UpdateTimeChangeListener.Manager();


    /**
     * @return Absolute Root Directory of Runtime (location of finroc_core.jar)
     */
    public static File getRootDir() {
        //logDomain.log(LogLevel.LL_DEBUG, getLogDescription(), "Root dir is " + rootDir);
        return rootDir;
    }

    /**
     * @param s String from config file
     * @return Byte Order
     */
    private static ByteOrder processByteOrderString(String s) {
        String x = s.toLowerCase().trim();
        if (x.contains("little")) {
            return ByteOrder.LITTLE_ENDIAN;
        }
        if (x.contains("big")) {
            return ByteOrder.BIG_ENDIAN;
        }
        return ByteOrder.nativeOrder();
    }

    /**
     * @return Returns whether runtime is executed as .class files (usually in Debug mode) or in .jar file
     */
    public synchronized static boolean isDebugging() {
        if (debugging == null) {
            try {
                debugging = (ANDROID_PLATFORM || runningInApplet) ? false : !RuntimeSettings.class.getResource("RuntimeSettings.class").toString().contains(".jar!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return debugging;
    }

    protected RuntimeSettings() {
        super(new FrameworkElement(RuntimeEnvironment.getInstance(), "Settings"), "Core");
        WARN_ON_CYCLE_TIME_EXCEED = new ParameterBool("WARN_ON_CYCLE_TIME_EXCEED", this, true);
        DEFAULT_CYCLE_TIME = new ParameterNumeric<Long>("DEFAULT_CYCLE_TIME", this, 50L, new Bounds<Long>(1, 2000));
        DEFAULT_MINIMUM_NETWORK_UPDATE_TIME = new ParameterNumeric<Integer>("DEFAULT_MINIMUM_NETWORK_UPDATE_TIME", this, 40, new Bounds<Integer>(1, 2000));
        STREAM_THREAD_CYCLE_TIME = new ParameterNumeric<Integer>("STREAM_THREAD_CYCLE_TIME", this, 200, new Bounds<Integer>(1, 2000));
        GARBAGE_COLLECTOR_SAFETY_PERIOD = new ParameterNumeric<Integer>("GARBAGE_COLLECTOR_SAFETY_PERIOD", this, 5000, new Bounds<Integer>(500, 50000));

        // add ports with update times
        //addChild(DataTypeRegister2.getInstance());

        // Beanshell console?
        //if (DISPLAY_CONSOLE) {
        //  new DebugConsole(null);
        //}
    }

    /** Completes initialization */
    static void staticInit() {
        //inst.sharedPorts = new SharedPorts(inst.portRoot);
        //inst.init(RuntimeEnvironment.getInstance());
        getInstance();
        DEFAULT_MINIMUM_NETWORK_UPDATE_TIME.addPortListener(inst);
    }

    /** @return Singleton instance */
    public static RuntimeSettings getInstance() {
        if (inst == null) {
            inst = new RuntimeSettings();
            //AutoDeleter.addStatic(inst);
        }
        return inst;
    }

    /**
     * @param listener Listener to add
     */
    public void addUpdateTimeChangeListener(UpdateTimeChangeListener listener) {
        updateTimeListener.add(listener);
    }

    /**
     * @param listener Listener to remove
     */
    public void removeUpdateTimeChangeListener(UpdateTimeChangeListener listener) {
        updateTimeListener.remove(listener);
    }

    @Override
    public void portChanged(AbstractPort origin, CoreNumber value) {
        updateTimeListener.notify(null, null, (short)value.intValue());
    }

    /*Cpp
    virtual void portChanged(AbstractPort* origin, const int& value)
    {
      updateTimeListener.notify(NULL, NULL, static_cast<int16>(value));
    }
    */

    /**
     * Notify update time change listener of change
     *
     * @param dt Datatype whose default time has changed
     * @param time New time
     */
    public void notifyUpdateTimeChangeListener(DataTypeBase dt, short time) {
        updateTimeListener.notify(dt, null, time);
    }

    /**
     * @return Is application running in applet?
     */
    public static boolean isRunningInApplet() {
        return runningInApplet;
    }

    /**
     * @return Should cc ports be used in backend?
     */
    public static boolean useCCPorts() {
        return useCCPorts;
    }

    /**
     * @param newUseCCPorts Should cc ports be used in backend?
     * (This is mainly an optimization. In non-data-intensive/non-runtime-critical applications (such as tooling) this may be disabled to reduce memory footprint.)
     * (This may only be changed before any data type has been registered and any port created)
     */
    public static void setUseCCPorts(boolean newUseCCPorts) {
        if (DataTypeBase.getTypeCount() > 0) {
            throw new RuntimeException("This is only possible before any type has been loaded.");
        }
        useCCPorts = newUseCCPorts;
    }

    /**
     * @return Determines maximum number of ports in CoreRegister (2 ^ maxCoreRegisterIndexBits)
     */
    public static int getMaxCoreRegisterIndexBits() {
        return maxCoreRegisterIndexBits;
    }

    /**
     * (will only have an effect if set before runtime environment was created)
     *
     * @param maxCoreRegisterIndexBits Determines maximum number of ports in CoreRegister (2 ^ maxCoreRegisterIndexBits)
     */
    public static void setMaxCoreRegisterIndexBits(int maxCoreRegisterIndexBits) {
        RuntimeSettings.maxCoreRegisterIndexBits = maxCoreRegisterIndexBits;
    }

    /**
     * Allow creation of framework elements with the same qualified names.
     * Activating this is somewhat dangerous. Only do this if you really have to!
     * If ports have the same qualified names, connecting to them by-string causes undefined behaviour.
     */
    public static void allowDuplicateQualifiedNames() {
        duplicateQualifiedNamesAllowed = true;
    }

    /**
     * @return Is creation of framework elements with the same qualified names allowed?
     */
    public static boolean duplicateQualifiedNamesAllowed() {
        return duplicateQualifiedNamesAllowed;
    }

    /**
     * @param runningInApplet Is runtime instantiated in Java Applet
     */
    public static void setRunningInApplet(boolean runningInApplet) {
        RuntimeSettings.runningInApplet = runningInApplet;
    }
}
