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
package org.finroc.core.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.finroc.core.admin.AdminClient;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.remote.RemoteTypeConversion.SupportedTypeFilter;
import org.finroc.core.remote.RemoteUriConnector.Status;
import org.finroc.core.util.ConcurrentLookupTable;
import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.container.SafeConcurrentlyIterableList;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;
import org.rrlib.serialization.BinaryInputStream;
import org.rrlib.serialization.PublishedRegisters;
import org.rrlib.serialization.Register;
import org.rrlib.serialization.SerializationInfo;
import org.rrlib.serialization.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Represents remote runtime environments
 * (only meant for use on local system)
 */
public class RemoteRuntime extends RemoteFrameworkElement {

    /** Remote Runtime's UUID */
    public final String uuid;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RemoteRuntime(String name, String uuid, AdminClient adminInterface, BinaryInputStream stream, int handleStampWidth) {
        super(0, name);
        this.handleStampWidth = handleStampWidth;
        int handleBitWidth = 32 - handleStampWidth;
        int chunkSizeBits = handleBitWidth / 2;
        int chunkCountBits = handleBitWidth - chunkSizeBits;
        int chunkSize = 1 << chunkSizeBits;
        int chunkCount = 1 << chunkCountBits;
        elementLookup = new ConcurrentLookupTable<>(chunkCount, chunkSize);
        connectorTable = new ConcurrentLookupTable<>(chunkCount, chunkSize);
        remoteTypes = (Register<RemoteType>)(Register)PublishedRegisters.getRemoteRegister(stream, Definitions.RegisterUIDs.TYPE.ordinal());
        remoteStaticCasts = (Register<RemoteStaticCast>)(Register)PublishedRegisters.getRemoteRegister(stream, Definitions.RegisterUIDs.STATIC_CAST.ordinal());
        remoteTypeConversions = (Register<RemoteTypeConversion>)(Register)PublishedRegisters.getRemoteRegister(stream, Definitions.RegisterUIDs.CONVERSION_OPERATION.ordinal());
        remoteSchemeHandlers = (Register<RemoteUriSchemeHandler>)(Register)PublishedRegisters.getRemoteRegister(stream, Definitions.RegisterUIDs.SCHEME_HANDLER.ordinal());
        remoteCreateActions = (Register<RemoteCreateAction>)(Register)PublishedRegisters.getRemoteRegister(stream, Definitions.RegisterUIDs.CREATE_ACTION.ordinal());
        if (remoteStaticCasts == null) {
            remoteStaticCasts = new Register<>(4, 4, 1);
        }
        if (remoteTypeConversions == null) {
            remoteTypeConversions = new Register<>(4, 4, 1);
        }
        if (remoteSchemeHandlers == null) {
            remoteSchemeHandlers = new Register<>(4, 4, 1);
        }
        if (remoteCreateActions == null) {
            remoteCreateActions = new Register<>(128, 128, 4);
        }
        this.inputStream = stream;
        this.uuid = uuid;
        this.adminInterface = adminInterface;
        elementLookup.set(0, this); // Runtime lookup
        this.serializationInfo = stream.getSourceInfo();
    }

    /**
     * Adds connector to runtime
     *
     * @param connector Connector to add
     */
    public void addConnector(RemoteConnector connector) {
        connector.ownerRuntime = this;
        if (connector instanceof RemoteUriConnector) {
            uriConnectors.add((RemoteUriConnector)connector, false);
        } else {
            for (int i = 0; i < 2; i++) {
                int index = (int)(((i == 0 ? connector.getSourceHandle() : connector.getDestinationHandle()) & 0xFFFFFFFFL) >> handleStampWidth);
                AbstractPort.EdgeList<RemoteConnector> list = connectorTable.get(index);
                if (list == null) {
                    list = new AbstractPort.EdgeList<RemoteConnector>();
                    connectorTable.set(index, list);
                }

                // Check whether edge has already been added
                ArrayWrapper<RemoteConnector> iterable = list.getIterable();
                for (int j = 0, n = iterable.size(); j < n; j++) {
                    RemoteConnector existingConnector = iterable.get(j);
                    if (existingConnector.getSourceHandle() == connector.getSourceHandle() && existingConnector.getDestinationHandle() == connector.getDestinationHandle()) {
                        Log.log(LogLevel.WARNING, "The same connector was added again. Discarding new connector.");
                        return;
                    }
                }


                list.add(connector, false);
            }
        }
    }

    /**
     * @param port1 Port 1
     * @param port2 Port 2
     * @return Whether Port 1 and Port 2 are connected
     */
    public static boolean arePortsConnected(RemotePort port1, RemotePort port2) {
        RemoteRuntime runtime1 = RemoteRuntime.find(port1);
        RemoteRuntime runtime2 = RemoteRuntime.find(port2);

        if (runtime1 == null || runtime2 == null) {
            return false;
        }

        if (runtime1 == runtime2) {
            int index = (int)((port1.getRemoteHandle() & 0xFFFFFFFFL) >> runtime1.handleStampWidth);
            AbstractPort.EdgeList<RemoteConnector> connectors = runtime1.connectorTable.get(index);
            if (connectors != null) {
                ArrayWrapper<RemoteConnector> iterable = connectors.getIterable();
                for (int i = 0, n = iterable.size(); i < n; i++) {
                    RemoteConnector connector = iterable.get(i);
                    if (connector != null && (connector.sourceHandle == port2.getRemoteHandle() || connector.destinationHandle == port2.getRemoteHandle())) {
                        return true;
                    }
                }
            }

            ArrayWrapper<RemoteUriConnector> iterable = runtime1.uriConnectors.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                RemoteUriConnector connector = iterable.get(i);
                if (connector != null && connector.getOwnerRuntime() == runtime1 && (connector.getOwnerPortHandle() == port1.getRemoteHandle() && connector.currentPartner == port2) || (connector.getOwnerPortHandle() == port2.getRemoteHandle() && connector.currentPartner == port1)) {
                    return true;
                }
            }
        } else {
            ArrayWrapper<RemoteUriConnector> iterable = runtime1.uriConnectors.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                RemoteUriConnector connector = iterable.get(i);
                if (connector != null && (connector.getOwnerPortHandle() == port1.getRemoteHandle() && connector.currentPartner == port2)) {
                    return true;
                }
            }
            iterable = runtime2.uriConnectors.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                RemoteUriConnector connector = iterable.get(i);
                if (connector != null && (connector.getOwnerPortHandle() == port2.getRemoteHandle() && connector.currentPartner == port1)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find RemoteRuntime to which specified element belongs to
     *
     * @param remoteElement Remote Element
     * @return RemoteRuntime object - or null if none could be found
     */
    public static RemoteRuntime find(ModelNode remoteElement) {
        if (remoteElement == null) {
            return null;
        }
        do {
            if (remoteElement instanceof RemoteRuntime) {
                return (RemoteRuntime)remoteElement;
            }
            remoteElement = (ModelNode)remoteElement.getParent();
        } while (remoteElement != null);
        return null;
    }

//    /**
//     * Find other remote runtime environment with the specified UUID
//     *
//     * @param uuid uuid of other runtime environment
//     * @return Other runtime - or null if no runtime environment with specified id could be found
//     */
//    public RemoteRuntime findOther(String uuid) {
//
//        // A more sophisticated search might be necessary in the future
//        ModelNode parent = this.getParent();
//        if (parent != null) {
//            for (int i = 0; i < parent.getChildCount(); i++) {
//                ModelNode child = parent.getChildAt(i);
//                if (child instanceof RemoteRuntime && ((RemoteRuntime)child).uuid.equals(uuid)) {
//                    return (RemoteRuntime)child;
//                }
//            }
//        }
//        return null;
//    }

    /**
     * @return Admin interface for remote runtime
     */
    public AdminClient getAdminInterface() {
        return adminInterface;
    }

    /**
     * @param result List to write result (all connectors) to. Will be cleared in this call.
     * @param port Remote port whose connectors to get
     */
    public void getConnectors(ArrayList<RemoteConnector> result, RemotePort port) {
        result.clear();
        int index = (int)((port.getRemoteHandle() & 0xFFFFFFFFL) >> handleStampWidth);
        AbstractPort.EdgeList<RemoteConnector> connectors = connectorTable.get(index);
        if (connectors != null) {
            ArrayWrapper<RemoteConnector> iterable = connectors.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                RemoteConnector connector = iterable.get(i);
                if (connector != null) {
                    result.add(connector);
                }
            }
        }

        // Search for URI connectors in all remote runtimes
        ModelNode parent = this.getParent();
        if (parent != null) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                ModelNode child = parent.getChildAt(i);
                if (child instanceof RemoteRuntime) {
                    ArrayWrapper<RemoteUriConnector> iterable = ((RemoteRuntime)child).uriConnectors.getIterable();
                    for (int j = 0, n = iterable.size(); j < n; j++) {
                        RemoteUriConnector connector = iterable.get(j);
                        if (connector != null && (connector.currentPartner == port || (connector.getOwnerRuntime() == this && connector.getOwnerPortHandle() == port.getRemoteHandle()))) {
                            result.add(connector);
                        }
                    }
                }
            }
        }
    }

    /**
     * Get conversion options from source to destination type.
     *
     * @param sourceType Source type
     * @param destinationType Destination type
     * @param singleOperationsOnly Only return single operations (no sequences)
     * @return List with all available conversion options
     */
    public ArrayList<RemoteConnectOptions> getConversionOptions(RemoteType sourceType, RemoteType destinationType, boolean singleOperationsOnly) {
        ArrayList<RemoteConnectOptions> result = new ArrayList<RemoteConnectOptions>();
        if (sourceType == destinationType) {
            result.add(new RemoteConnectOptions(Definitions.TypeConversionRating.NO_CONVERSION));
            return result;
        }
        ArrayList<GetCastOperationEntry> fromSourceType = new ArrayList<GetCastOperationEntry>();
        ArrayList<GetCastOperationEntry> fromDestinationType = new ArrayList<GetCastOperationEntry>();
        short sourceUid = (short)sourceType.getHandle();
        short destinationUid = (short)destinationType.getHandle();

        // Collect all static casts
        RemoteTypeConversion staticCast = getTypeConversionOperation(RemoteTypeConversion.STATIC_CAST);
        if (sourceType.getUnderlyingType() > 0) {
            fromSourceType.add(new GetCastOperationEntry(staticCast, remoteTypes.get(sourceType.getUnderlyingType()), (sourceType.getTypeTraits() & DataTypeBase.IS_CAST_TO_UNDERLYING_TYPE_IMPLICIT) != 0));
        }
        for (int i = 0, n = remoteTypes.size(); i < n; i++) {
            RemoteType type = remoteTypes.get(i);
            if (type.getUnderlyingType() == sourceUid && (type.getTypeTraits() & DataTypeBase.IS_REINTERPRET_CAST_FROM_UNDERLYING_TYPE_VALID) != 0) {
                fromSourceType.add(new GetCastOperationEntry(staticCast, type, (sourceType.getTypeTraits() & DataTypeBase.IS_CAST_FROM_UNDERLYING_TYPE_IMPLICIT) != 0));
            }
        }
        for (int i = 0, n = remoteStaticCasts.size(); i < n; i++) {
            RemoteStaticCast cast = remoteStaticCasts.get(i);
            if (cast.getSourceType() == sourceType) {
                fromSourceType.add(new GetCastOperationEntry(staticCast, cast.getDestinationType(), cast.isImplicit()));
            }
        }
        if (destinationType.getUnderlyingType() > 0 && (destinationType.getTypeTraits() & DataTypeBase.IS_REINTERPRET_CAST_FROM_UNDERLYING_TYPE_VALID) != 0) {
            fromDestinationType.add(new GetCastOperationEntry(staticCast, remoteTypes.get(destinationType.getUnderlyingType()), (sourceType.getTypeTraits() & DataTypeBase.IS_CAST_FROM_UNDERLYING_TYPE_IMPLICIT) != 0));
        }
        for (int i = 0, n = remoteTypes.size(); i < n; i++) {
            RemoteType type = remoteTypes.get(i);
            if (type.getUnderlyingType() == destinationUid) {
                fromDestinationType.add(new GetCastOperationEntry(staticCast, type, (sourceType.getTypeTraits() & DataTypeBase.IS_CAST_TO_UNDERLYING_TYPE_IMPLICIT) != 0));
            }
        }
        for (int i = 0, n = remoteStaticCasts.size(); i < n; i++) {
            RemoteStaticCast cast = remoteStaticCasts.get(i);
            if (cast.getDestinationType() == destinationType) {
                fromDestinationType.add(new GetCastOperationEntry(staticCast, cast.getSourceType(), cast.isImplicit()));
            }
        }

        // Collect all operations from source type
        for (int i = 0, n = remoteTypeConversions.size(); i < n; i++) {
            RemoteTypeConversion conversion = remoteTypeConversions.get(i);
            if (isTypeSupported(sourceType, conversion.getSupportedSourceTypes(), conversion.getSupportedSourceType())) {
                addAllTypes(fromSourceType, conversion, conversion.getSupportedDestinationTypes(), conversion.getSupportedDestinationType());
            }
        }
        RemoteTypeConversion getListElementOperation = getTypeConversionOperation(RemoteTypeConversion.GET_LIST_ELEMENT);
        if ((sourceType.getTypeTraits() & DataTypeBase.IS_LIST_TYPE) != 0) {
            fromSourceType.add(new GetCastOperationEntry(getListElementOperation, remoteTypes.get(sourceUid - 1), false));
        }

        // Collect all operations from destination type
        for (int i = 0, n = remoteTypeConversions.size(); i < n; i++) {
            RemoteTypeConversion conversion = remoteTypeConversions.get(i);
            if (isTypeSupported(destinationType, conversion.getSupportedDestinationTypes(), conversion.getSupportedDestinationType())) {
                addAllTypes(fromDestinationType, conversion, conversion.getSupportedSourceTypes(), conversion.getSupportedSourceType());
            }
        }

        // Sort results
        Collections.sort(fromSourceType);
        Collections.sort(fromDestinationType);

        // Scan for single operation
        for (int i = 0; i < 2; i++) {
            RemoteType relevantType = i == 0 ? destinationType : sourceType;
            for (GetCastOperationEntry entry : (i == 0 ? fromSourceType : fromDestinationType)) {
                if (entry.intermediateType == relevantType) {
                    boolean exists = false;
                    for (RemoteConnectOptions existing : result) {
                        if (existing.operation1 == entry.operation) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        result.add(new RemoteConnectOptions(entry.implicitCast ? Definitions.TypeConversionRating.IMPLICIT_CAST : Definitions.TypeConversionRating.EXPLICIT_CONVERSION, entry.operation));
                    }
                }
            }
        }

        // Scan for operation sequences
        if (singleOperationsOnly) {
            return result;
        }

        int fromSourceIndex = 0, fromDestinationIndex = 0;
        while (fromSourceIndex < fromSourceType.size() && fromDestinationIndex < fromDestinationType.size()) {
            RemoteType fromSourceIntermediateType = fromSourceType.get(fromSourceIndex).intermediateType;
            RemoteType fromDestinationIntermediateType = fromDestinationType.get(fromDestinationIndex).intermediateType;
            if (fromSourceIntermediateType.getHandle() < fromDestinationIntermediateType.getHandle()) {
                fromSourceIndex++;
            } else if (fromSourceIntermediateType.getHandle() > fromDestinationIntermediateType.getHandle()) {
                fromDestinationIndex++;
            } else {
                RemoteType matchingType = fromSourceIntermediateType;
                while (fromSourceIndex < fromSourceType.size() && fromSourceType.get(fromSourceIndex).intermediateType == matchingType) {
                    int fromDestinationIndexTemp = fromDestinationIndex;
                    while (fromDestinationIndexTemp < fromDestinationType.size() && fromDestinationType.get(fromDestinationIndexTemp).intermediateType == matchingType) {
                        boolean exists = false;
                        for (RemoteConnectOptions existing : result) {
                            if (existing.operation1 == fromSourceType.get(fromSourceIndex).operation && existing.operation2 == fromDestinationType.get(fromDestinationIndexTemp).operation && existing.intermediateType == matchingType) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            int implicitConversions = (fromSourceType.get(fromSourceIndex).implicitCast ? 1 : 0) + (fromDestinationType.get(fromDestinationIndexTemp).implicitCast ? 1 : 0);
                            result.add(new RemoteConnectOptions(implicitConversions == 2 ? Definitions.TypeConversionRating.TWO_IMPLICIT_CASTS : (implicitConversions == 1 ? Definitions.TypeConversionRating.EXPLICIT_CONVERSION_AND_IMPLICIT_CAST : Definitions.TypeConversionRating.TWO_EXPLICIT_CONVERSIONS),
                                                                fromSourceType.get(fromSourceIndex).operation, matchingType, fromDestinationType.get(fromDestinationIndexTemp).operation));
                        }
                        fromDestinationIndexTemp++;
                    }
                    fromSourceIndex++;
                }
            }
        }

        // Try for-each and get elements operations
        boolean sourceIsListType = (sourceType.getTypeTraits() & DataTypeBase.IS_LIST_TYPE) != 0;
        boolean destinationIsListType = (destinationType.getTypeTraits() & DataTypeBase.IS_LIST_TYPE) != 0;
        if (sourceIsListType && destinationIsListType) {
            RemoteTypeConversion forEachOperation = getTypeConversionOperation(RemoteTypeConversion.FOR_EACH);
            RemoteType sourceElementType = remoteTypes.get(sourceType.getHandle() - 1);
            RemoteType destinationElementType = remoteTypes.get(destinationType.getHandle() - 1);
            for (RemoteConnectOptions operation : getConversionOptions(sourceElementType, destinationElementType, true)) {
                result.add(new RemoteConnectOptions(operation.conversionRating == Definitions.TypeConversionRating.IMPLICIT_CAST ? Definitions.TypeConversionRating.EXPLICIT_CONVERSION : Definitions.TypeConversionRating.TWO_EXPLICIT_CONVERSIONS, forEachOperation, sourceElementType, operation.operation1));
            }
        }

        // Mark deprecated casts
        RemoteTypeConversion toStringOperation = getTypeConversionOperation(RemoteTypeConversion.TO_STRING);
        RemoteTypeConversion stringDeserializationOperation = getTypeConversionOperation(RemoteTypeConversion.STRING_DESERIALIZATION);
        RemoteTypeConversion binarySerializationOperation = getTypeConversionOperation(RemoteTypeConversion.BINARY_SERIALIZATION);
        RemoteTypeConversion binaryDeserializationOperation = getTypeConversionOperation(RemoteTypeConversion.BINARY_DESERIALIZATION);
        for (RemoteConnectOptions operation : result) {
            if (operation.operation1 == toStringOperation && operation.operation2 == stringDeserializationOperation ||
                    operation.operation1 == binarySerializationOperation && operation.operation2 == binaryDeserializationOperation) {
                operation.conversionRating = Definitions.TypeConversionRating.DEPRECATED_CONVERSION;
            }
        }

        Collections.sort(result);

        return result;
    }

    /**
     * @return Create actions in runtime
     */
    public Register<RemoteCreateAction> getCreateActions() {
        if (serializationInfo.getRevision() == 0) {
            ArrayList<RemoteCreateAction> actions = getAdminInterface().getRemoteModuleTypes(this);
            for (int i = remoteCreateActions.size(); i < actions.size(); i++) {
                remoteCreateActions.add(actions.get(i));
            }
        } else {
            adminInterface.getRegisterUpdates(Definitions.RegisterUIDs.CREATE_ACTION.ordinal());
        }
        return remoteCreateActions;
    }

    /**
     * @return Retruns any elements in error state in this runtime. May only be accessed by thread that builds model.
     */
    public ArrayList<RemoteFrameworkElement> getElementsInErrorState() {
        return elementsInErrorState;
    }

    /**
     * @return Input stream that contains e.g. all shared registers
     */
    public BinaryInputStream getInputStream() {
        return inputStream;
    }

    /**
     * @param handle Remote framework element handle
     * @return Returns object that represents remote framework element with this remote handle
     */
    public RemoteFrameworkElement getRemoteElement(int handle) {
        int index = (int)((handle & 0xFFFFFFFFL) >> handleStampWidth);
        return elementLookup.get(index);
    }

    /**
     * @param handle Remote port handle
     * @return Returns object that represents remote port with this remote handle
     */
    public RemotePort getRemotePort(int handle) {
        RemoteFrameworkElement element = getRemoteElement(handle);
        return element != null && (element instanceof RemotePort) ? (RemotePort)element : null;
    }

    /**
     * @return List with all remote ports
     */
    public ArrayList<RemotePort> getRemotePorts() {
        ArrayList<RemoteFrameworkElement> temp = new ArrayList<>();
        elementLookup.getAll(temp);
        ArrayList<RemotePort> result = new ArrayList<>(temp.size());
        for (RemoteFrameworkElement element : temp) {
            if (element instanceof RemotePort) {
                result.add((RemotePort)element);
            }
        }
        return result;
    }

    /**
     * @param name Type name
     * @return Type with specified name in remote runtime. null if no type with specified name exists.
     */
    public RemoteType getRemoteType(String name) {
        for (int i = 0, n = remoteTypes.size(); i < n; i++) {
            if (remoteTypes.get(i).getName().equals(name)) {
                return remoteTypes.get(i);
            }
        }
        return null;
    }

    /**
     * @return Returns (thread-safe) register with remote types
     */
    public Register<RemoteType> getRemoteTypes() {
        return remoteTypes;
    }

    /**
     * @return URI scheme handlers in runtime
     */
    public Register<RemoteUriSchemeHandler> getSchemeHandlers() {
        return remoteSchemeHandlers;
    }

    /**
     * @return Serialization info from remote runtime
     */
    public SerializationInfo getSerializationInfo() {
        return serializationInfo;
    }

    /**
     * @return Static casts in runtime
     */
    public Register<RemoteStaticCast> getStaticCasts() {
        return remoteStaticCasts;
    }

    /**
     * @param name Name of remote type conversion operation
     * @return Return type conversion operation with specified name - or null if no type conversion operation with this name exists
     */
    public RemoteTypeConversion getTypeConversionOperation(String name) {
        for (int i = 0, n = remoteTypeConversions.size(); i < n; i++) {
            if (remoteTypeConversions.get(i).getName().equals(name)) {
                return remoteTypeConversions.get(i);
            }
        }
        return null;
    }

    /**
     * @param sourceType Source type
     * @param destinationType Destination type
     * @return Rating of 'best' possibility to convert source type to destination type in this runtime environment
     */
    public Definitions.TypeConversionRating getTypeConversionRating(RemoteType sourceType, RemoteType destinationType) {
        return getTypeConversionRatings(sourceType).getRating(destinationType);
    }

    /**
     * Returns type conversion ratings for specified type.
     * This method cashes results in RemoteType and updates cached results when required.
     * (Implementation note: as complete cached results are replaced atomically with the old ones remaining intact, this is thread-safe)
     *
     * @param type Type whose ratings to update
     * @param Returns type conversion ratings
     */
    public RemoteType.CachedConversionRatings getTypeConversionRatings(RemoteType type) {

        RemoteType.CachedConversionRatings currentRatings = type.cachedConversionRatings.get();
        if (currentRatings != null && currentRatings.typeCount == remoteTypes.size() && currentRatings.staticCastCount == remoteStaticCasts.size() && currentRatings.typeConversionOperationCount == remoteTypeConversions.size() && (!currentRatings.singleOperationsResultsOnly)) {
            return currentRatings;
        }

        // Init new result
        RemoteType.CachedConversionRatings singleRatings = getTypeSingleConversionRatings(type);
        RemoteType.CachedConversionRatings ratings = new RemoteType.CachedConversionRatings();
        ratings.typeCount = singleRatings.typeCount;
        ratings.staticCastCount = singleRatings.staticCastCount;
        ratings.typeConversionOperationCount = singleRatings.typeConversionOperationCount;
        ratings.singleOperationsResultsOnly = false;
        ratings.cachedConversionRatings = new byte[ratings.typeCount];
        System.arraycopy(singleRatings.cachedConversionRatings, 0, ratings.cachedConversionRatings, 0, singleRatings.cachedConversionRatings.length);
        if (getSerializationInfo().getRevision() != 0) {
            for (int i = 0; i < ratings.cachedConversionRatings.length; i++) {
                if (ratings.cachedConversionRatings[i] == 0 && (remoteTypes.get(i).getTypeTraits() & DataTypeBase.IS_DATA_TYPE) != 0) {
                    ratings.cachedConversionRatings[i] = 1; // Deprecated binary conversion works for all types
                }
            }
        }

        // Continue from all single operation entries
        for (int i = 0; i < ratings.cachedConversionRatings.length; i++) {
            byte rating = ratings.cachedConversionRatings[i];
            if (isSingleOperationRating(rating)) {
                RemoteType.CachedConversionRatings intermediateRatings = getTypeSingleConversionRatings(remoteTypes.get(i));
                for (int j = 0; j < ratings.cachedConversionRatings.length; j++) {
                    byte rating2 = intermediateRatings.cachedConversionRatings[j];
                    if (isSingleOperationRating(rating2)) {
                        int implicitCasts = (rating == Definitions.TypeConversionRating.IMPLICIT_CAST.ordinal() ? 1 : 0) + (rating2 == Definitions.TypeConversionRating.IMPLICIT_CAST.ordinal() ? 1 : 0);
                        boolean deprecatedConversion = ((rating == Definitions.TypeConversionRating.EXPLICIT_CONVERSION_FROM_GENERIC_TYPE.ordinal()) && (rating2 == Definitions.TypeConversionRating.EXPLICIT_CONVERSION_TO_GENERIC_TYPE.ordinal())) ||
                                                       ((rating2 == Definitions.TypeConversionRating.EXPLICIT_CONVERSION_FROM_GENERIC_TYPE.ordinal()) && (rating == Definitions.TypeConversionRating.EXPLICIT_CONVERSION_TO_GENERIC_TYPE.ordinal()));
                        updateConversionRating(ratings.cachedConversionRatings, j, deprecatedConversion ? Definitions.TypeConversionRating.DEPRECATED_CONVERSION : (implicitCasts == 2 ? Definitions.TypeConversionRating.TWO_IMPLICIT_CASTS : (implicitCasts == 1 ? Definitions.TypeConversionRating.EXPLICIT_CONVERSION_AND_IMPLICIT_CAST : Definitions.TypeConversionRating.TWO_EXPLICIT_CONVERSIONS)));
                    }
                }
            }
        }

        // For/each operation
        if ((type.getTypeTraits() & DataTypeBase.IS_LIST_TYPE) != 0) {
            RemoteType elementType = remoteTypes.get(type.getHandle() - 1);
            RemoteType.CachedConversionRatings intermediateRatings = getTypeSingleConversionRatings(elementType);
            for (int i = 0; i < ratings.cachedConversionRatings.length; i++) {
                byte rating = intermediateRatings.cachedConversionRatings[i];
                if (isSingleOperationRating(rating)) {
                    RemoteType destinationElementType = remoteTypes.get(i);
                    if ((destinationElementType.getTypeTraits() & DataTypeBase.IS_LIST_TYPE) == 0 && (destinationElementType.getTypeTraits() & DataTypeBase.HAS_LIST_TYPE) == 1) {
                        updateConversionRating(ratings.cachedConversionRatings, i + 1, rating == Definitions.TypeConversionRating.IMPLICIT_CAST.ordinal() ? Definitions.TypeConversionRating.EXPLICIT_CONVERSION : Definitions.TypeConversionRating.TWO_EXPLICIT_CONVERSIONS);
                    }
                }
            }
        }

        // Replace result if it has not changed
        if (type.cachedConversionRatings.compareAndSet(singleRatings, ratings)) {
            return ratings;
        }
        return getTypeConversionRatings(type);
    }


    /**
     * @return Type conversion operations in runtime
     */
    public Register<RemoteTypeConversion> getTypeConversions() {
        return remoteTypeConversions;
    }

    /**
     * @return Types in runtime
     */
    public Register<RemoteType> getTypes() {
        return remoteTypes;
    }

    /**
     * @param sourceType Source type
     * @param destinationType Destination type
     * @return Whether there is a static cast from sourceType to destinationType in remote runtime that is implicit
     */
    public boolean isStaticCastImplicit(RemoteType sourceType, RemoteType destinationType) {
        if ((sourceType.getTypeTraits() & DataTypeBase.HAS_UNDERLYING_TYPE) != 0 && sourceType.getUnderlyingType() == destinationType.getHandle() && (sourceType.getTypeTraits() & DataTypeBase.IS_CAST_TO_UNDERLYING_TYPE_IMPLICIT) != 0) {
            return true;
        }
        if ((destinationType.getTypeTraits() & DataTypeBase.HAS_UNDERLYING_TYPE) != 0 && (destinationType.getTypeTraits() & DataTypeBase.IS_REINTERPRET_CAST_FROM_UNDERLYING_TYPE_VALID) != 0 && destinationType.getUnderlyingType() == sourceType.getHandle() && (destinationType.getTypeTraits() & DataTypeBase.IS_CAST_FROM_UNDERLYING_TYPE_IMPLICIT) != 0) {
            return true;
        }
        int size = remoteStaticCasts.size();
        for (int i = 0; i < size; i++) {
            RemoteStaticCast cast = remoteStaticCasts.get(i);
            if (cast.isImplicit() && cast.getSourceType() == sourceType && cast.getDestinationType() == destinationType) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param sourceType Source type
     * @param destinationType Destination type
     * @return Whether there is a static cast from sourceType to destinationType in remote runtime
     */
    public boolean isStaticCastSupported(RemoteType sourceType, RemoteType destinationType) {
        if ((sourceType.getTypeTraits() & DataTypeBase.HAS_UNDERLYING_TYPE) != 0 && sourceType.getUnderlyingType() == destinationType.getHandle()) {
            return true;
        }
        if ((destinationType.getTypeTraits() & DataTypeBase.HAS_UNDERLYING_TYPE) != 0 && (destinationType.getTypeTraits() & DataTypeBase.IS_REINTERPRET_CAST_FROM_UNDERLYING_TYPE_VALID) != 0 && destinationType.getUnderlyingType() == sourceType.getHandle()) {
            return true;
        }
        int size = remoteStaticCasts.size();
        for (int i = 0; i < size; i++) {
            RemoteStaticCast cast = remoteStaticCasts.get(i);
            if (cast.getSourceType() == sourceType && cast.getDestinationType() == destinationType) {
                return true;
            }
        }
        return false;
    }

    /**
     * Load module library in external runtime
     *
     * @param load module library to load
     * @return Updated list of remote create module actions
     */
    public Register<RemoteCreateAction> loadModuleLibrary(String load) {
        List<RemoteCreateAction> createActions = adminInterface.loadModuleLibrary(load, this);
        if (serializationInfo.getRevision() == 0) {
            for (int i = remoteCreateActions.size(); i < createActions.size(); i++) {
                remoteCreateActions.add(createActions.get(i));
            }
        } else {
            adminInterface.getRegisterUpdates(Definitions.RegisterUIDs.CREATE_ACTION.ordinal());
        }
        return remoteCreateActions;
    }

    /**
     * Returns framework element with specified handle.
     * Creates one if it doesn't exist.
     *
     * @param handle Remote handle of framework element
     * @return Framework element.
     */
    public RemoteFrameworkElement obtainFrameworkElement(int handle) {
        int index = (int)((handle & 0xFFFFFFFFL) >> handleStampWidth);
        RemoteFrameworkElement remoteElement = elementLookup.get(index);
        if (remoteElement == null) {
            remoteElement = new RemoteFrameworkElement(handle, "(unknown)");
            setElementLookupEntry(handle, remoteElement);
        }
        return remoteElement;
    }

    /**
     * Removes connector from connector table
     *
     * @param connector Connector to remove
     */
    public void removeConnector(RemoteConnector connector) {
        if (connector instanceof RemoteUriConnector) {
            uriConnectors.remove((RemoteUriConnector)connector);
        } else {
            for (int i = 0; i < 2; i++) {
                int index = (int)(((i == 0 ? connector.getSourceHandle() : connector.getDestinationHandle()) & 0xFFFFFFFFL) >> handleStampWidth);
                AbstractPort.EdgeList<RemoteConnector> list = connectorTable.get(index);
                if (list != null) {
                    list.remove(connector);
                }
            }
        }
    }

    /**
     * Removes connector from connector table
     *
     * @param sourceHandle Handle of source port
     * @param destinationHandle Handle of destination port
     */
    public void removeConnector(int sourceHandle, int destinationHandle) {
        for (int i = 0; i < 2; i++) {
            int index = (int)(((i == 0 ? sourceHandle : destinationHandle) & 0xFFFFFFFFL) >> handleStampWidth);
            AbstractPort.EdgeList<RemoteConnector> list = connectorTable.get(index);
            if (list != null) {
                ArrayWrapper<RemoteConnector> iterable = list.getIterable();
                for (int j = 0, n = iterable.size(); j < n; j++) {
                    RemoteConnector connector = iterable.get(j);
                    if ((connector.sourceHandle == sourceHandle && connector.destinationHandle == destinationHandle) || (connector.sourceHandle == destinationHandle && connector.destinationHandle == sourceHandle)) {
                        list.remove(connector);
                    }
                }
            }
        }
    }

    /**
     * Removes all connectors owned by port with specified handle
     *
     * @param portHandle Handle of port
     */
    public void removePortConnectors(int portHandle) {
        int index = (int)((portHandle & 0xFFFFFFFFL) >> handleStampWidth);
        AbstractPort.EdgeList<RemoteConnector> list = connectorTable.get(index);
        if (list != null) {
            list.clear();
        }

        ArrayWrapper<RemoteUriConnector> iterable = uriConnectors.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            RemoteUriConnector connector = iterable.get(i);
            if (connector != null && connector.getOwnerPortHandle() == portHandle) {
                uriConnectors.remove(connector);
            }
        }
    }

    /**
     * Removes URI connector
     *
     * @param ownerHandle Handle of owner port
     * @param index Index of URI connector in owner port list
     */
    public void removeUriConnector(int ownerHandle, byte index) {
        ArrayWrapper<RemoteUriConnector> iterable = uriConnectors.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            RemoteUriConnector connector = iterable.get(i);
            if (connector != null && connector.getOwnerPortHandle() == ownerHandle && connector.getIndex() == index) {
                uriConnectors.remove(connector);
            }
        }
    }

    /**
     * Resolves local data types if this has not been done yet
     * Should be done when static cast operations are also available
     */
    public synchronized void resolveDefaultLocalTypes() {
        int typeCount = this.remoteTypes.size();
        if (resolvedTypes < typeCount) {
            for (int i = resolvedTypes; i < typeCount; i++) {
                this.remoteTypes.get(i).resolveDefaultLocalType(this);
            }
            resolvedTypes = typeCount;
        }
    }

    /**
     * Sets entry in element lookup table (thread-safe)
     *
     * @param handle Handle of element
     * @param element Element
     */
    public void setElementLookupEntry(int handle, RemoteFrameworkElement element) {
        int index = (int)((handle & 0xFFFFFFFFL) >> handleStampWidth);
        if (element != null && elementLookup.get(index) != null) {
            Log.log(LogLevel.WARNING, "Lookup entry overwritten");
        }
        elementLookup.set(index, element);
    }

    /**
     * Sets status of remote URI connector
     *
     * @param ownerHandle Handle of owner port
     * @param index Index of URI connector in owner port list
     * @param status New Status
     */
    public void setUriConnectorStatus(int ownerHandle, byte index, Status status) {
        ArrayWrapper<RemoteUriConnector> iterable = uriConnectors.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            RemoteUriConnector connector = iterable.get(i);
            if (connector != null && connector.getOwnerPortHandle() == ownerHandle && connector.getIndex() == index) {
                connector.setStatus(status);
            }
        }
    }

    /**
     * Update/resolve URI connector connection partners using current model.
     * When model changes, this needs to be computed again (suggestion: do before redraw)
     * May only be called by thread that builds remote model
     */
    public void updateUriConnectors() {
        ArrayList<RemoteRuntime> otherRuntimes = new ArrayList<RemoteRuntime>();
        elementsInErrorState.clear();
        ArrayWrapper<RemoteUriConnector> iterable = uriConnectors.getIterable();
        for (int j = 0, n = iterable.size(); j < n; j++) {
            RemoteUriConnector connector = iterable.get(j);
            if (connector == null) {
                continue;
            }
            if (connector.getStatus() != RemoteUriConnector.Status.CONNECTED) {
                connector.currentPartner = null;
                RemotePort errorPort = getRemotePort(connector.getOwnerPortHandle());
                if (errorPort != null) {
                    elementsInErrorState.add(errorPort);
                }
                continue;
            }
            if (connector.currentPartner != null && connector.currentPartner.getRoot() == this.getRoot()) {  // still connected
                continue;
            }
            connector.currentPartner = null;

            // Resolve runtimes that match scheme handler and authority (needs to be more sophisticated for supporting additional network transports)
            otherRuntimes.clear();
            if (connector.getSchemeHandler().getSchemeName().equals("")) {
                otherRuntimes.add(this);
            } else {
                ModelNode parent = this.getParent();
                if (parent != null) {
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        ModelNode child = parent.getChildAt(i);
                        if (child instanceof RemoteRuntime && connector.currentConnectionMayGoTo((RemoteRuntime)child)) {
                            otherRuntimes.add((RemoteRuntime)child);
                        }
                    }
                }
            }

            // Look for port in runtimes
            for (RemoteRuntime runtime : otherRuntimes) {
                RemotePort port = connector.getRemoteConnectedPort(runtime);
                if (port != null) {
                    connector.currentPartner = port;
                    break;
                }
            }
        }
    }

//    /**
//     * Find RemoteRuntime to which specified port belongs to
//     *
//     * @param np remote port
//     * @return RemoteRuntime object - or null if none could be found
//     */
//    public static RemoteRuntime find(NetPort np) {
//        return find(RemotePort.get(np.getPort())[0]);
//    }


    /** Admin interface for remote runtime */
    private final AdminClient adminInterface;

    /** Remote registers */
    private final Register<RemoteType> remoteTypes;
    private Register<RemoteStaticCast> remoteStaticCasts;
    private Register<RemoteTypeConversion> remoteTypeConversions;
    private Register<RemoteUriSchemeHandler> remoteSchemeHandlers;
    private Register<RemoteCreateAction> remoteCreateActions;

    /** Input stream containing all registers */
    private final BinaryInputStream inputStream;

    /** Number of resolved remote types */
    private int resolvedTypes;

    /** Thread-safe lookup for remote framework elements: remote handle => remote framework element */
    private final ConcurrentLookupTable<RemoteFrameworkElement> elementLookup;

    /** Stamp Width of framework element handles */
    private final int handleStampWidth;

    /** Thread-safe table/lookup for remote connectors: remote handle => remote connectors */
    private final ConcurrentLookupTable<AbstractPort.EdgeList<RemoteConnector>> connectorTable;

    /** List with all URI connectors */
    private final SafeConcurrentlyIterableList<RemoteUriConnector> uriConnectors = new SafeConcurrentlyIterableList<RemoteUriConnector>(128, 4);

    /** Serialization info from remote runtime */
    private final SerializationInfo serializationInfo;

    /** Contains any elements in error state in this runtime. May only be accessed by thread that builds model. */
    private final ArrayList<RemoteFrameworkElement> elementsInErrorState = new ArrayList<>();

    /** Internal helper type for getConversionOptions() methods */
    private static class GetCastOperationEntry implements Comparable<GetCastOperationEntry> {

        final RemoteType intermediateType;
        final RemoteTypeConversion operation;
        final boolean implicitCast;

        public GetCastOperationEntry(RemoteTypeConversion operation, RemoteType intermediateType, boolean implicitCast) {
            this.operation = operation;
            this.intermediateType = intermediateType;
            this.implicitCast = implicitCast;
        }

        @Override
        public int compareTo(GetCastOperationEntry o) {
            if (intermediateType != o.intermediateType) {
                return Integer.compare(intermediateType.getHandle(), o.intermediateType.getHandle());
            }
            return operation.getName().compareTo(o.operation.getName());
        }
    }

    /**
     * Helper function for getConversionOptions()
     *
     * @param type Type to check
     * @param filter Type filter to apply
     * @param singleType Single type for type filter 'SINGLE'
     * @return Whether 'type' is supported by non-special function
     */
    private boolean isTypeSupported(RemoteType type, SupportedTypeFilter filter, RemoteType singleType) {
        return (filter == SupportedTypeFilter.ALL) ||
               (filter == SupportedTypeFilter.BINARY_SERIALIZABLE && (type.getTypeTraits() & DataTypeBase.IS_BINARY_SERIALIZABLE) != 0) ||
               (filter == SupportedTypeFilter.SINGLE && singleType == type) ||
               (filter == SupportedTypeFilter.STRING_SERIALIZABLE && (type.getTypeTraits() & DataTypeBase.IS_STRING_SERIALIZABLE) != 0);
    }

    /**
     * Helper function for getConversionOptions()
     *
     * @param result List to store result in
     * @param operation Type conversion operation in question
     * @param filter Type filter to apply
     * @param singleType Single type for type filter 'SINGLE'
     */
    private void addAllTypes(ArrayList<GetCastOperationEntry> result, RemoteTypeConversion operation, SupportedTypeFilter filter, RemoteType singleType) {
        if (filter == SupportedTypeFilter.SINGLE) {
            result.add(new GetCastOperationEntry(operation, singleType, false));
        } else if (filter == SupportedTypeFilter.ALL || filter == SupportedTypeFilter.BINARY_SERIALIZABLE || filter == SupportedTypeFilter.STRING_SERIALIZABLE) {
            int requiredFlag = filter == SupportedTypeFilter.ALL ? 0 : (filter == SupportedTypeFilter.BINARY_SERIALIZABLE ? DataTypeBase.IS_BINARY_SERIALIZABLE : DataTypeBase.IS_STRING_SERIALIZABLE);
            for (int i = 0, n = remoteTypes.size(); i < n; i++) {
                RemoteType type = remoteTypes.get(i);
                if ((type.getTypeTraits() & requiredFlag) == requiredFlag) {
                    result.add(new GetCastOperationEntry(operation, type, false));
                }
            }
        }
    }

    /**
     * Helper function for getTypeConversionRatings()
     *
     * @param result Object to store result in
     * @param operation Type conversion operation in question
     * @param filter Type filter to apply
     * @param singleType Single type for type filter 'SINGLE'
     * @param sourceFilter Whether this is the source filter
     */
    private void addAllDestinationTypes(RemoteType.CachedConversionRatings result, RemoteTypeConversion operation) {
        RemoteTypeConversion.SupportedTypeFilter filter = operation.getSupportedDestinationTypes();
        if (operation.getSupportedDestinationTypes() == SupportedTypeFilter.SINGLE) {
            Definitions.TypeConversionRating rating = (operation.getSupportedSourceTypes() == SupportedTypeFilter.ALL || operation.getSupportedSourceTypes() == SupportedTypeFilter.BINARY_SERIALIZABLE || operation.getSupportedSourceTypes() == SupportedTypeFilter.STRING_SERIALIZABLE) ? Definitions.TypeConversionRating.EXPLICIT_CONVERSION_TO_GENERIC_TYPE : Definitions.TypeConversionRating.EXPLICIT_CONVERSION;
            updateConversionRating(result.cachedConversionRatings, operation.getSupportedDestinationType().getHandle(), rating);
        } else if (filter == SupportedTypeFilter.ALL || filter == SupportedTypeFilter.BINARY_SERIALIZABLE || filter == SupportedTypeFilter.STRING_SERIALIZABLE) {
            int requiredFlag = filter == SupportedTypeFilter.ALL ? 0 : (filter == SupportedTypeFilter.BINARY_SERIALIZABLE ? DataTypeBase.IS_BINARY_SERIALIZABLE : DataTypeBase.IS_STRING_SERIALIZABLE);
            for (int i = 0; i < result.typeCount; i++) {
                RemoteType type = remoteTypes.get(i);
                if ((type.getTypeTraits() & requiredFlag) == requiredFlag) {
                    updateConversionRating(result.cachedConversionRatings, type.getHandle(), Definitions.TypeConversionRating.EXPLICIT_CONVERSION_FROM_GENERIC_TYPE);
                }
            }
        }
    }

    /**
     * Helper function for getTypeConversionRatings()
     *
     * @param ratingArray Array with cached results. Stores provided rating value if it is better than the current value.
     * @param index Index (== remote handle) of type of interest
     * @param rating Rating to possibly set array entry to
     */
    private void updateConversionRating(byte[] ratingArray, int index, Definitions.TypeConversionRating rating) {
        byte value = (byte)rating.ordinal();
        if (value > ratingArray[index]) {
            ratingArray[index] = value;
        }
    }

    /**
     * Helper function for getTypeConversionRatings()
     * Returns type conversion ratings for specified type if only a single type conversion is used.
     * Returned object may also include ratings for type conversion sequences.
     * This method cashes results in RemoteType and updates cached results when required.
     * (Implementation note: as complete cached results are replaced atomically with the old ones remaining intact, this is thread-safe)
     *
     * @param type Type whose ratings to update
     * @param Returns type conversion ratings
     */
    private RemoteType.CachedConversionRatings getTypeSingleConversionRatings(RemoteType type) {
        RemoteType.CachedConversionRatings currentRatings = type.cachedConversionRatings.get();
        if (currentRatings != null && currentRatings.typeCount == remoteTypes.size() && currentRatings.staticCastCount == remoteStaticCasts.size() && currentRatings.typeConversionOperationCount == remoteTypeConversions.size()) {
            return currentRatings;
        }

        // Init new result
        RemoteType.CachedConversionRatings ratings = new RemoteType.CachedConversionRatings();
        ratings.typeCount = remoteTypes.size();
        ratings.staticCastCount = remoteStaticCasts.size();
        ratings.typeConversionOperationCount = remoteTypeConversions.size();
        ratings.singleOperationsResultsOnly = true;
        ratings.cachedConversionRatings = new byte[ratings.typeCount];
        ratings.cachedConversionRatings[type.getHandle()] = (byte)Definitions.TypeConversionRating.NO_CONVERSION.ordinal();

        // Process static casts
        if (type.getUnderlyingType() > 0) {
            updateConversionRating(ratings.cachedConversionRatings, type.getUnderlyingType(), ((type.getTypeTraits() & DataTypeBase.IS_CAST_TO_UNDERLYING_TYPE_IMPLICIT) != 0) ? Definitions.TypeConversionRating.IMPLICIT_CAST : Definitions.TypeConversionRating.EXPLICIT_CONVERSION);
        }
        for (int i = 0; i < ratings.typeCount; i++) {
            RemoteType type2 = remoteTypes.get(i);
            if (type2.getUnderlyingType() == type.getHandle() && (type2.getTypeTraits() & DataTypeBase.IS_REINTERPRET_CAST_FROM_UNDERLYING_TYPE_VALID) != 0) {
                updateConversionRating(ratings.cachedConversionRatings, i, ((type.getTypeTraits() & DataTypeBase.IS_CAST_FROM_UNDERLYING_TYPE_IMPLICIT) != 0) ? Definitions.TypeConversionRating.IMPLICIT_CAST : Definitions.TypeConversionRating.EXPLICIT_CONVERSION);
            }
        }
        for (int i = 0; i < ratings.staticCastCount; i++) {
            RemoteStaticCast cast = remoteStaticCasts.get(i);
            if (cast.getSourceType() == type) {
                updateConversionRating(ratings.cachedConversionRatings, cast.getDestinationType().getHandle(), cast.isImplicit() ? Definitions.TypeConversionRating.IMPLICIT_CAST : Definitions.TypeConversionRating.EXPLICIT_CONVERSION);
            }
        }

        // Process type conversion operations
        for (int i = 0; i < ratings.typeConversionOperationCount; i++) {
            RemoteTypeConversion conversion = remoteTypeConversions.get(i);
            if (isTypeSupported(type, conversion.getSupportedSourceTypes(), conversion.getSupportedSourceType())) {
                addAllDestinationTypes(ratings, conversion);
            }
        }

        // Get list elements operation
        if ((type.getTypeTraits() & DataTypeBase.IS_LIST_TYPE) != 0) {
            updateConversionRating(ratings.cachedConversionRatings, type.getHandle() - 1, Definitions.TypeConversionRating.EXPLICIT_CONVERSION);
        }

        // Replace result if it has not changed
        if (type.cachedConversionRatings.compareAndSet(currentRatings, ratings)) {
            return ratings;
        }
        return getTypeSingleConversionRatings(type);
    }

    /**
     * Helper function for getTypeConversionRatings()
     *
     * @param rating Rating to check
     * @return Whether this is a rating for a single operation
     */
    private static boolean isSingleOperationRating(byte rating) {
        return rating == Definitions.TypeConversionRating.IMPLICIT_CAST.ordinal() || rating == Definitions.TypeConversionRating.EXPLICIT_CONVERSION.ordinal() || rating == Definitions.TypeConversionRating.EXPLICIT_CONVERSION_FROM_GENERIC_TYPE.ordinal() || rating == Definitions.TypeConversionRating.EXPLICIT_CONVERSION_TO_GENERIC_TYPE.ordinal();
    }
}
