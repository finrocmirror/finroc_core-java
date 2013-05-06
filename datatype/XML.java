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
package org.finroc.core.datatype;

import java.io.StringReader;

import org.rrlib.finroc_core_utils.rtti.DataType;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;
import org.rrlib.finroc_core_utils.xml.XML2WrapperException;
import org.rrlib.finroc_core_utils.xml.XMLDocument;
import org.rrlib.finroc_core_utils.xml.XMLNode;
import org.xml.sax.InputSource;

/**
 * @author Max Reichardt
 *
 * Buffer containing XML Data
 */
public class XML extends CoreString {

    /** UID */
    private static final long serialVersionUID = -239392323342057L;

    /** Data Type */
    public final static DataTypeBase TYPE = new DataType<XML>(XML.class, "XML", false);

    public XML() {}

    @Override
    public void serialize(XMLNode node) throws Exception {
        if (getBuffer().length() > 0) {
            XMLDocument doc = new XMLDocument(new InputSource(new StringReader(toString())), false);
            node.addChildNode(doc.getRootNode(), true);
        }
    }

    @Override
    public void deserialize(XMLNode node) throws Exception {
        set(node.getChildrenBegin().get().getXMLDump(true));
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof XML) {
            try {
                XMLDocument d1 = new XMLDocument(new InputSource(new StringReader(toString())), false);
                XMLDocument d2 = new XMLDocument(new InputSource(new StringReader(other.toString())), false);
                return d1.getRootNode().nodeEquals(d2.getRootNode());
            } catch (XML2WrapperException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}
