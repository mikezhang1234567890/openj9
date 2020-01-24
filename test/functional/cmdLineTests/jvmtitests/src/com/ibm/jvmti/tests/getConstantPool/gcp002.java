/*******************************************************************************
 * Copyright (c) 2020, 2020 IBM Corp. and others
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] http://openjdk.java.net/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR LicenseRef-GPL-2.0 WITH Assembly-exception
 *******************************************************************************/
package com.ibm.jvmti.tests.getConstantPool;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class gcp002
{
	static final int CONSTANT_Utf8 = 1;
	static final int CONSTANT_Integer = 3;
	static final int CONSTANT_Float = 4;
	static final int CONSTANT_Long = 5;
	static final int CONSTANT_Double = 6;
	static final int CONSTANT_Class = 7;
	static final int CONSTANT_String = 8;
	static final int CONSTANT_Fieldref = 9;
	static final int CONSTANT_Methodref = 10;
	static final int CONSTANT_InterfaceMethodref = 11;
	static final int CONSTANT_NameAndType = 12;
	static final int CONSTANT_MethodHandle = 15;
	static final int CONSTANT_MethodType = 16;
	static final int CONSTANT_Dynamic = 17;
	static final int CONSTANT_InvokeDynamic = 18;
	static final int CONSTANT_Module = 19;
	static final int CONSTANT_Package = 20;

	private static native byte[] constantPoolBytesWrapper(Class c);
	private static native int constantPoolCountWrapper(Class c);

	public boolean testGetConstantPool() throws Exception {
		byte[] generatedBytes = GcpClassGenerator.generateClass("gcpTest");
		GcpClassGenerator.dumpToFile("gcpTest.class", generatedBytes);

		CustomClassLoader cl = new CustomClassLoader();
		Class clazz = cl.getClass("gcpTest", generatedBytes);
	
		byte[] cp = constantPoolBytesWrapper(clazz);
		int count = constantPoolCountWrapper(clazz);
		GcpClassGenerator.dumpToFile("gcpTest_cp", cp);
		System.out.println("generated size:" + generatedBytes.length + " class: " + clazz.getName() + " cp count: " + count + " cp bytes: " + cp.length);
		int[] cpIndices = indexConstantPool(cp, count);
		return checkParsingConstantPool(cp, cpIndices);
	}

	public String helpGetConstantPool() {
		return "testGetConstantPool verifies constant pool can be parsed";
	}

	private static boolean checkParsingConstantPool(byte[] cpBytes, int[] cpIndexOffsets) {
		int cpIndex = 1;
		while (cpIndex < cpIndexOffsets.length) {
			int byteOffset = cpIndexOffsets[cpIndex]; 
			int offset1, offset2;
			switch(cpBytes[byteOffset]) {
				case CONSTANT_Class:
					offset1 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 1)];
					if (cpBytes[offset1] != CONSTANT_Utf8) {
						printFailParsingCPEntry("class", cpIndex, byteOffset);
						return false;
					}
					break;
				case CONSTANT_Fieldref:
				case CONSTANT_Methodref:
				case CONSTANT_InterfaceMethodref:
					offset1 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 1)];
					if (cpBytes[offset1] != CONSTANT_Class) {
						printFailParsingCPEntry("field/method/interfacemethod ref", cpIndex, byteOffset);
						return false;
					}
					offset2 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 3)];
					if (cpBytes[offset2] != CONSTANT_NameAndType) {
						printFailParsingCPEntry("field/method/interfacemethod ref", cpIndex, byteOffset);
						return false;
					}
					break;
				case CONSTANT_String:
					offset1 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 1)];
					if (cpBytes[offset1] != CONSTANT_Utf8) {
						printFailParsingCPEntry("string", cpIndex, byteOffset);
						return false;
					}
					break;
				case CONSTANT_Long:
				case CONSTANT_Double:
					cpIndex++;
				case CONSTANT_Integer:
				case CONSTANT_Float:
					break;
				case CONSTANT_NameAndType:
					offset1 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 1)];
					if (cpBytes[offset1] != CONSTANT_Utf8) {
						printFailParsingCPEntry("name and type", cpIndex, byteOffset);
						return false;
					}
					offset2 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 3)];
					if (cpBytes[offset2] != CONSTANT_Utf8) {
						printFailParsingCPEntry("name and type", cpIndex, byteOffset);
						return false;
					}
					break;
				case CONSTANT_Utf8:
					int utfEndOffset = readUnsignedShort(cpBytes, byteOffset + 1);
					for (int i = byteOffset + 3; i < utfEndOffset; i++) {
						if (cpBytes[i] == 0 || (cpBytes[i] >= 0xf0 && cpBytes[i] <= 0xff)) {
							printFailParsingCPEntry("utf8", cpIndex, byteOffset);
							return false;
						}
					}
					break;
				case CONSTANT_MethodHandle:
					int kind = cpBytes[byteOffset + 1];
					if (kind >= 1 && kind <=9) {
						offset1 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 2)];
						if (kind <= 4) {
							if (cpBytes[offset1] == CONSTANT_Fieldref) {
								break;
							}
						} else if (kind == 5 || kind == 8) {
							if (cpBytes[offset1] == CONSTANT_Methodref) {
								break;
							}
						} else if (kind == 6 || kind == 7) {
							/* for classfile ver 52.0 and up only, but we compile with at least Java 8, which is 52.0 */
							if (cpBytes[offset1] == CONSTANT_Methodref || cpBytes[offset1] == CONSTANT_InterfaceMethodref) {
								break;
							}
						}
					}
					printFailParsingCPEntry("method handle", cpIndex, byteOffset);
					return false;
				case CONSTANT_MethodType:
					offset1 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 1)];
					if (cpBytes[offset1] != CONSTANT_Utf8) {
						printFailParsingCPEntry("method type", cpIndex, byteOffset);
						return false;
					}
					break;
				case CONSTANT_Dynamic:
				case CONSTANT_InvokeDynamic:
					offset1 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 1)];
					offset2 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 3)];
					if (cpBytes[offset2] != CONSTANT_NameAndType) {
						printFailParsingCPEntry("dynamic/invokedynamic", cpIndex, byteOffset);
						return false;
					}
					break;
				case CONSTANT_Module:
				case CONSTANT_Package:
					offset1 = cpIndexOffsets[readUnsignedShort(cpBytes, byteOffset + 1)];
					if (cpBytes[offset1] != CONSTANT_Utf8) {
						printFailParsingCPEntry("module/package", cpIndex, byteOffset);
						return false;
					}
					break;
				default:
					System.out.println("Unknown cp entry at index " + cpIndex + ", offset " + byteOffset + "in the cp bytes array.");
					return false;
			}
			cpIndex++;
		}
		return true;
	}

	private static int[] indexConstantPool(byte[] cpBytes, int cpCount) {
		int[] indices = new int[cpCount];
		int byteIndex = 0;
		int countIndex = 1;
		try {
			while (countIndex < cpCount) {
				indices[countIndex++] = byteIndex;
				switch (cpBytes[byteIndex]) {
					case CONSTANT_Fieldref:
					case CONSTANT_Methodref:
					case CONSTANT_InterfaceMethodref:
					case CONSTANT_Integer:
					case CONSTANT_Float:
					case CONSTANT_NameAndType:
						byteIndex += 5;
						break;
					case CONSTANT_Dynamic:
						byteIndex += 5;
						break;
					case CONSTANT_InvokeDynamic:
						byteIndex += 5;
						break;
					case CONSTANT_Long:
					case CONSTANT_Double:
						byteIndex += 9;
						countIndex++;
						break;
					case CONSTANT_Utf8:
						byteIndex += 3 + readUnsignedShort(cpBytes, byteIndex + 1);
						break;
					case CONSTANT_MethodHandle:
						byteIndex += 4;
						break;
					case CONSTANT_Class:
					case CONSTANT_String:
					case CONSTANT_MethodType:
					case CONSTANT_Package:
					case CONSTANT_Module:
						byteIndex += 3;
						break;
					default:
						throw new IllegalArgumentException("CP Index " + (countIndex - 1) + " has invalid tag at byte " + byteIndex + " with value" + cpBytes[byteIndex]);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("CP Index " + (countIndex - 1) + " " + e.getMessage());
		}
		return indices;
	}

	private static int readUnsignedShort(byte[] bytes, int offset) {
		return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
	}

	private static void printFailParsingCPEntry(String entry, int index, int offset) {
		System.out.println("Failed parsing " + entry + " cp entry at index " + index + ", offset " + offset + " in the cp bytes array.");

	}

	private static boolean isUtfFieldDescriptor(byte[] bytes, int offset) {
		int len = readUnsignedShort(bytes, offset + 1);
		int end = offset + 3 + len;
		int index = offset + 3;
		while (bytes[index] == '[') {
			index++;
		}
		if ("BCDFIJSZ".contains(bytes[index])) {
			index++;
		} else if (bytes.index == 'L') {
			index++;
			int index2 = index;
			while (index < end && bytes[index] != ';') {
				if (!Character.isisJavaIdentifierPart(bytes[index2])) {
					break;
				}
				index2++;
			}
			bytes[index2] != ';'
		}

		if (index == end) {
			return true;
		}
		return false;
	}
}
