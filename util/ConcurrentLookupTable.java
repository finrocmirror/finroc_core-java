//
// You received this file as part of RRLib
// Robotics Research Library
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
package org.finroc.core.util;

import java.util.ArrayList;

/**
 * @author Max Reichardt
 *
 * Concurrent lookup table.
 * It allows lookups concurrently to modifications.
 */
public class ConcurrentLookupTable<Entry> {

    /**
     * @param chunkCount Number of chunks
     * @param chunkSize Size of chunks
     */
    public ConcurrentLookupTable(int chunkCount, int chunkSize) {
        this.chunkCount = chunkCount;
        this.chunkSize = chunkSize;
        chunks = new Object[chunkCount][];
    }

    /**
     * @param index Index of entry
     * @return Entry at specified index
     */
    @SuppressWarnings("unchecked")
    public Entry get(long index) {
        int chunkIndex = (int)(index / chunkSize);
        int chunkElementIndex = (int)(index % chunkSize);
        return chunks[chunkIndex] == null ? null : (Entry)chunks[chunkIndex][chunkElementIndex];
    }

    /**
     * Gets all non-null entries from this table
     *
     * @param result List to store result in (cleared before filling)
     */
    @SuppressWarnings("unchecked")
    public void getAll(ArrayList<Entry> result) {
        result.clear();
        for (Object[] chunk : chunks) {
            if (chunk != null) {
                for (Object o : chunk) {
                    if (o != null) {
                        result.add((Entry)o);
                    }
                }
            }
        }
    }

    /**
     * Sets register entry
     *
     * @param index Index of entry
     * @param entry Entry to add
     * @return Index in register
     */
    public synchronized void set(long index, Entry entry) {
        int chunkIndex = (int)(index / chunkSize);
        if (chunkIndex >= chunkCount) {
            throw new IndexOutOfBoundsException("Adding element exceeds table size (possibly increase table's chunkCount or chunkSize)");
        }
        int chunkElementIndex = (int)(index % chunkSize);
        if (chunks[chunkIndex] == null) {
            // Allocate new chunk
            chunks[chunkIndex] = new Object[chunkSize];
        }
        chunks[chunkIndex][chunkElementIndex] = entry;
    }


    /** Size and number of chunks */
    private final int chunkCount, chunkSize;

    /** Array with chunks */
    private final Object[][] chunks;

}
