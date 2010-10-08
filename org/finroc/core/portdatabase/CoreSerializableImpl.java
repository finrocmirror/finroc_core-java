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
package org.finroc.core.portdatabase;

import org.finroc.core.buffer.CoreInput;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.jc.annotation.CppName;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Superclass;
import org.finroc.xml.XMLNode;

/**
 * @author max
 *
 * Default implementation of CoreSerializable
 */
@CppName("CoreSerializable")
@Superclass( {})
public abstract class CoreSerializableImpl implements CoreSerializable {

    /**
     * @param os Stream to serialize object to
     */
    @Override
    public abstract void serialize(CoreOutput os);

    //Cpp virtual ~CoreSerializable() {}

    /**
     * Deserialize object. Object has to already exists.
     * Should be suited for reusing old objects.
     *
     * @param readView Stream to deserialize from
     */
    @Override
    public abstract void deserialize(CoreInput is);

    /**
     * @return Object serialized as string (e.g. for xml output)
     */
    @Override @InCppFile
    public String serialize() {
        return SerializationHelper.serializeToHexString(this);
    }

    /**
     * Deserialize object. Object has to already exists.
     * Should be suited for reusing old objects.
     *
     * Parsing errors should throw an Exception - and set object to
     * sensible (default?) value
     *
     * @param s String to deserialize from
     */
    @Override @InCppFile
    public void deserialize(String s) throws Exception {
        SerializationHelper.deserializeFromHexString(this, s);
    }

    /**
     * Serialize object to XML
     *
     * @param node Empty XML node (name shouldn't be changed)
     */
    @Override @InCppFile
    public void serialize(XMLNode node) throws Exception {
        node.setTextContent(serialize());
    }

    /**
     * Deserialize from XML Node
     *
     * @param node Node to deserialize from
     */
    @Override @InCppFile
    public void deserialize(XMLNode node) throws Exception {
        deserialize(node.getTextContent());
    }
}
