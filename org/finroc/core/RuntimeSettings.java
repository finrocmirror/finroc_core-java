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
package org.finroc.core;

import java.io.File;
import java.nio.ByteOrder;

import org.finroc.jc.AutoDeleter;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.PassByValue;

import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.cc.CCPortListener;
import org.finroc.core.port.net.UpdateTimeChangeListener;
import org.finroc.core.portdatabase.DataType;
import org.finroc.core.setting.BoolSetting;
import org.finroc.core.setting.IntSetting;
import org.finroc.core.setting.LongSetting;
import org.finroc.core.setting.Settings;
import org.finroc.core.util.Files;

/**
 * @author max
 *
 * Contains global settings of runtime environment.
 *
 * For variable settings it contains ports inside a module.
 *
 * staticInit() should be called after runtime and data types have been initialized.
 */
@ForwardDecl(CCPortBase.class)
@CppInclude( {"port/cc/NumberPort.h"})
public class RuntimeSettings extends Settings implements CCPortListener<CoreNumber> {

    /** Singleton Instance */
    private static RuntimeSettings inst = getInstance();

    /**
     * This is turned to #ifdefs in C++
     * Perform checks on buffer usage and report unsafeties.
     * If no such unsafeties are reported when running an application,
     * this flag may be turned off in release mode to gain performance.
     */
    //public static final boolean CORE_DEBUG_CHECKS = false; // Replaced by asserts

    /**
     * There's a Java and C++ version of this framework. Java modules can
     * be used in both. This flag indicates, whether the Java modules are
     * used together with a C++ core.
     */
    public static final boolean CPP_CORE = false;

    /** Display warning, if loop times of CoreLoopThreads are exceeded? */
    public static final BoolSetting WARN_ON_CYCLE_TIME_EXCEED = inst.add("WARN_ON_CYCLE_TIME_EXCEED", true, true);

    /** Default cycle time of CoreLoopThreads in ms*/
    public static final LongSetting DEFAULT_CYCLE_TIME = inst.add("DEFAULT_CYCLE_TIME", 50L, true);

    /** Maximum number of threads */
    //public static final IntSetting MAX_THREADS = inst.add("MAX_THREADS", 256, false); // only required during initialization

    /** Default number of loop threads */
    public static final IntSetting NUM_OF_LOOP_THREADS = inst.add("NUM_OF_LOOP_THREADS", 8, false);

    /** Default number of self updating loop threads */
    //public static final IntSetting SELF_UPDATING_LOOP_THREADS = inst.add("SELF_UPDATING_LOOP_THREADS", 8, false);

    /** Default number of event threads */
    public static final IntSetting NUM_OF_EVENT_THREADS = inst.add("NUM_OF_EVENT_THREADS", 2, false);

    /** Maximum queue size for reference queues in ports */
    //public static final int MAX_PORT_REFERENCE_QUEUE_SIZE = getInt("MAX_PORT_REFERENCE_QUEUE_SIZE", Integer.MAX_VALUE);

    /** Default minimum network update time (ms) */
    public static final IntSetting DEFAULT_MINIMUM_NETWORK_UPDATE_TIME = inst.add("DEFAULT_MINIMUM_NETWORK_UPDATE_TIME", 40, true);

    public static final int EDGE_LIST_DEFAULT_SIZE = 0;
    public static final int EDGE_LIST_SIZE_INCREASE_FACTOR = 2;

    /** Default size of PortDataContainerPool for different kinds of ports */
    /*public static final int DEFAULT_SEND_BUFFER_SIZE_MAIN_EXPRESS = getInt("DEFAULT_SEND_BUFFER_SIZE_MAIN_EXPRESS", 4);
    public static final int DEFAULT_SEND_BUFFER_SIZE_ALT_EXPRESS = getInt("DEFAULT_SEND_BUFFER_SIZE_ALT_EXPRESS", 2);
    public static final int DEFAULT_SEND_BUFFER_SIZE_MAIN_BULK = getInt("DEFAULT_SEND_BUFFER_SIZE_MAIN_BULK", 1);
    public static final int DEFAULT_SEND_BUFFER_SIZE_ALT_BULK = getInt("DEFAULT_SEND_BUFFER_SIZE_ALT_BULK", 0);
    public static final int DEFAULT_SEND_BUFFER_SIZE_INPUT_PORT_MAIN = getInt("DEFAULT_SEND_BUFFER_SIZE_INPUT_PORT_MAIN", 0);
    public static final int DEFAULT_SEND_BUFFER_SIZE_INPUT_PORT_ALT = getInt("DEFAULT_SEND_BUFFER_SIZE_INPUT_PORT_ALT", 1);  // for browser etc.*/

    /** Absolute Root Directory of Runtime (location of finroc_core.jar) */
    @JavaOnly private final static File rootDir = new File(Files.getRootDir(RuntimeSettings.class)); //new File(new File(Files.getRootDir(RuntimeSettings.class)).getParentFile().getAbsolutePath());

    /** Is runtime executed as .class files (usually in Debug mode) or in .jar file */
    @JavaOnly private final static Boolean debugging = new File(Files.getRootDir(RuntimeSettings.class)).getName().equals("bin");

    /** Debug Settings */
    public static final BoolSetting DISPLAY_MODULE_UPDATES = inst.add("DISPLAY_MODULE_UPDATES", false, true);
    public static final BoolSetting DISPLAY_MCA_MODULE_UPDATES = inst.add("DISPLAY_MCA_MODULE_UPDATES", false, true);
    public static final BoolSetting DISPLAY_MCA_MESSAGES = inst.add("DISPLAY_MCA_MESSAGES", true, true);
    public static final BoolSetting DISPLAY_MCA_BB_MESSAGES = inst.add("DISPLAY_MCA_BB_MESSAGES", false, true);
    public static final BoolSetting DISPLAY_LOOP_TIME = inst.add("DISPLAY_LOOP_TIME", false, true);
    public static final BoolSetting DISPLAY_DATATYPE_INIT = inst.add("DISPLAY_DATATYPE_INIT", false, true);
    public static final BoolSetting DISPLAY_BUFFER_ALLOCATION = inst.add("DISPLAY_BUFFER_ALLOCATION", false, true);
    public static final BoolSetting DISPLAY_INCOMING_PORT_INFO = inst.add("DISPLAY_INCOMING_PORT_INFO", false, true);
    public static final BoolSetting LOG_LOOP_TIMES = inst.add("LOG_LOOP_TIMES", false, true);
    public static final BoolSetting DISPLAY_CONSOLE = inst.add("DISPLAY_CONSOLE", false, true);
    public static final BoolSetting DISPLAY_CONSTRUCTION_DESTRUCTION = inst.add("DISPLAY_CONSTRUCTION_DESTRUCTION", true, true);
    public static final BoolSetting DISPLAY_EDGE_CREATION = inst.add("DISPLAY_EDGE_CREATION", false, true);

    /** Loop time for buffer tracker (in ms) */
    //public static final IntSetting BUFFER_TRACKER_LOOP_TIME = inst.add("BUFFER_TRACKER_LOOP_TIME", 140, true);

    /** Cycle time for stream thread */
    public static final IntSetting STREAM_THREAD_CYCLE_TIME = inst.add("STREAM_THREAD_CYCLE_TIME", 200, true);

    /** Create tree node for every RuntimeElement class? (needed for tree view as in GUI) */
    @JavaOnly public static final BoolSetting CREATE_TREE_NODES_FOR_RUNTIME_ELEMENTS = inst.add("CREATE_TREE_NODES_FOR_RUNTIME_ELEMENTS", false, false);

    /** > 0 if Runtime is instantiated in Java Applet - contains bit size of server CPU */
    public static final IntSetting runningInApplet = inst.add("RUNNING_IN_APPLET", 0, false);

    /**
     * Period in ms after which garbage collector will delete objects... any threads
     * still working on objects while creating deletion task should be finished by then
     */
    public static final IntSetting GARBAGE_COLLECTOR_SAFETY_PERIOD = inst.add("GARBAGE_COLLECTOR_SAFETY_PERIOD", 5000, true);

    /** ByteOrder of host that runtime is running on */
    //@JavaOnly public static ByteOrder byteOrder = processByteOrderString(ConfigFile.getInstance().getString("BYTE_ORDER", "native"));
    @JavaOnly public static ByteOrder byteOrder = ByteOrder.nativeOrder();

    /** Port with information about shared ports */
    //SharedPorts sharedPorts; // always has port index zero

    /** Collect edge statistics ? */
    public static final boolean COLLECT_EDGE_STATISTICS = false;

    /** List with listeners for update times */
    @PassByValue
    private final UpdateTimeChangeListener.Manager updateTimeListener = new UpdateTimeChangeListener.Manager();

    /**
     * @return Absolute Root Directory of Runtime (location of finroc_core.jar)
     */
    @JavaOnly public static File getRootDir() {
        System.out.println("Root dir is " + rootDir);
        return rootDir;
    }

    /**
     * @param s String from config file
     * @return Byte Order
     */
    @JavaOnly private static ByteOrder processByteOrderString(String s) {
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
    @JavaOnly public synchronized static boolean isDebugging() {
        return debugging;
    }

    protected RuntimeSettings() {
        super("Settings", "Runtime", true);

        // add shared ports port
        //addChild(sharedPorts.getPortSet());

        // init data type register
        //DataTypeRegister.init();

        // add ports with update times
        //addChild(DataTypeRegister2.getInstance());

        // Beanshell console?
        //if (DISPLAY_CONSOLE) {
        //  new DebugConsole(null);
        //}
    }

    /** Completes initialization */
    @InCppFile
    static void staticInit() {
        //inst.sharedPorts = new SharedPorts(inst.portRoot);
        inst.init(RuntimeEnvironment.getInstance());
        DEFAULT_MINIMUM_NETWORK_UPDATE_TIME.getPort().addPortListener(inst);
    }

    /** @return Singleton instance */
    public static RuntimeSettings getInstance() {
        if (inst == null) {
            inst = new RuntimeSettings();
            AutoDeleter.addStatic(inst);
        }
        return inst;
    }

//  @Override
//  protected void update() {}

    //@Override
    //public synchronized void delete() {}

//  /**
//   * @return Local information list about shared ports
//   */
//  public SharedPorts getSharedPorts() {
//      return sharedPorts;
//  }

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
    public void portChanged(CCPortBase origin, CoreNumber value) {
        updateTimeListener.notify(null, null, (short)value.intValue());
    }

    /**
     * Notify update time change listener of change
     *
     * @param dt Datatype whose default time has changed
     * @param time New time
     */
    public void notifyUpdateTimeChangeListener(DataType dt, short time) {
        updateTimeListener.notify(dt, null, time);
    }

    /**
     * @return Is application running in applet?
     */
    @JavaOnly
    public static boolean isRunningInApplet() {
        return false;
    }
}
