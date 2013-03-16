/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package edu.tufts.osidimpl.testing.repository;

import junit.framework.TestCase;

public class SubAssetMetadataTest extends TestCase
{
	/*
	 Assumes an Asset (OSID object and Test element are in hand.   Index is added for more informative messages
	 */
	public SubAssetMetadataTest(org.osid.repository.Asset nextAsset, org.w3c.dom.Element assetElement, String index)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		String expected = Utilities.expectedValue(assetElement,OsidTester.SUBASSET_DISPLAY_NAME_TAG);
		if (expected != null) {
			if (Utilities.isVerbose()) System.out.println(expected);
			assertEquals("seeking display name " + expected,expected,nextAsset.getDisplayName());
			System.out.println("PASSED: Asset's Display Name " + index + " " + expected);
		}
		
		expected = Utilities.expectedValue(assetElement,OsidTester.SUBASSET_DESCRIPTION_TAG);
		if (expected != null) {
			assertEquals("seeking description " + expected,expected,nextAsset.getDescription());
			System.out.println("PASSED: Asset's Description " + index + " " + expected);
		}
		
		expected = Utilities.expectedValue(assetElement,OsidTester.SUBASSET_ID_TAG);
		if (expected != null) {
			org.osid.shared.Id id = nextAsset.getId();
			try {
				String idString = id.getIdString();
				if (Utilities.isVerbose()) System.out.println("asset's id is " + idString);
				assertEquals("seeking identifier " + expected,expected,idString);
				System.out.println("PASSED: Asset's Id " + index + " " + expected);
			} catch (org.osid.shared.SharedException iex) {
				// ignore since this means something is amiss with Id
			}
		}
		
		expected = Utilities.expectedValue(assetElement,OsidTester.SUBASSET_TYPE_TAG);
		if (expected != null) {
			assertEquals("seeking type " + expected,expected,Utilities.typeToString(nextAsset.getAssetType()));
			System.out.println("PASSED: Asset's Type " + index + " " + expected);
		}
		
		expected = Utilities.expectedValue(assetElement,OsidTester.SUBASSET_REPOSITORY_ID_TAG);
		if (expected != null) {
			org.osid.shared.Id id = nextAsset.getRepository();
			try {
				String idString = id.getIdString();
				assertEquals(expected,idString);
				System.out.println("PASSED: Asset's Repository Id " + index + " " + expected);
			} catch (org.osid.shared.SharedException iex) {
				// ignore since this means something is amiss with Id
			}
		}
		
		// now look for parts to match (finds records, record, parts, part)
		org.w3c.dom.NodeList partNodeList = assetElement.getElementsByTagName(OsidTester.SUBASSET_PART_TAG);
		int numParts = partNodeList.getLength();
		if (numParts > 0) {
			// TODO: Deepen flexibility for different records
			org.osid.repository.RecordIterator recordIterator = nextAsset.getRecords();
			java.util.Vector typeVector = new java.util.Vector();
			java.util.Vector partVector = new java.util.Vector();
			
			// store all the type / value part pairs for comparison
			if (!(recordIterator.hasNextRecord())) {
				fail("No Records");
			}
			while (recordIterator.hasNextRecord()) {
				org.osid.repository.PartIterator partIterator = recordIterator.nextRecord().getParts();
				while (partIterator.hasNextPart()) {
					org.osid.repository.Part nextPart = partIterator.nextPart();
					String partStructureTypeString = Utilities.typeToString(nextPart.getPartStructure().getType());
					java.io.Serializable ser = nextPart.getValue();
					String partValue = null;
					if (ser instanceof String) {
						partValue = (String)ser;
					} else if (ser instanceof Integer) {
						partValue = ((Integer)ser).toString();
					} else if (ser instanceof Double) {
						partValue = ((Double)ser).toString();
					} else if (ser instanceof Boolean) {
						partValue = ((Boolean)ser).toString();
					} else if (ser instanceof String[]) {
						// create a string that is the concatenation of
						// string values in the array; use semicolon as delimiter
						String s[] = (String[])ser;
						StringBuffer sb = new StringBuffer();
						int len = s.length-1;
						for (int x=0; x < len; x++) {
							sb.append(s[x]);
							sb.append(";");
						}
						if (len >= 0) sb.append(s[len]);
						partValue = sb.toString();
					}
					typeVector.addElement(partStructureTypeString);
					partVector.addElement(partValue);
					if (Utilities.isVerbose()) System.out.println("Discovered Part Type " + partStructureTypeString + " with value " + partValue);
				}
			}
			
			// test the next part returned by the OSID against ANY part in the test
			for (int p=0; p < numParts; p++) {
				org.w3c.dom.Element partElement = (org.w3c.dom.Element)partNodeList.item(p);
				String expectedType = Utilities.expectedValue(partElement,OsidTester.SUBASSET_PART_TYPE_TAG);
				if (expectedType != null) {
					// test there is at least one type match
					if (typeVector.contains(expectedType)) {
						System.out.println("PASSED: Metadata for Asset " + index + " Part Type " + p + " " + expectedType);
					} else {
						fail("No match for Type " + expectedType + " in profile");
					}
				}
				
				expected = Utilities.expectedValue(partElement,OsidTester.SUBASSET_VALUE_TAG);
				if (expected != null) {
					// test there is a part for this type
					boolean found = false;
					for (int v=0, size = typeVector.size(); v < size; v++) {
						String s = (String)typeVector.elementAt(v);
						if (s.equals(expectedType)) {
							s = (String)partVector.elementAt(v);
							if (s.equals(expected)) {
								System.out.println("PASSED: Metadata for Asset " + index + " Part Value " + p + " " + expected);
								found = true;
							}
						}
					}
					if (!found) {
						fail("No match for Type(" + expectedType + ") and Value(" + expected + ") in profile");
					}
				}
			}
		}
	}
}