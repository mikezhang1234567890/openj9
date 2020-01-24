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

import java.io.FileOutputStream;
import java.io.IOException;

import org.objectweb.asm.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class GcpClassGenerator implements Opcodes {
	static int classVersion;
	static {
		String versionStr = System.getProperty("java.version");
		if (versionStr.startsWith("1.")) {
			versionStr = versionStr.substring(2,3);
		} else {
			int dot = versionStr.indexOf(".");
			versionStr = versionStr.substring(0, Character.isDigit(versionStr.charAt(1)) ? 2 : 1);
		}
		classVersion = Integer.parseInt(versionStr) + 44; //version opcodes start at 46 for Java 2
	}

	public static byte[] generateClass(String name) {
		System.out.println("Classfile version: " + classVersion);
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classWriter.visit(classVersion, ACC_PUBLIC | ACC_SUPER, name, null, "java/lang/Object", null);
		classWriter.visitSource(name + ".java", null);	

		/* Generate base constructor which just calls super.<init>() */
		MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn(ALOAD, 0);
		methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(1, 1);
		methodVisitor.visitEnd();

		Integer const_int = 4000;
		Long const_long = 40000L;
		Float const_float = 4000.1f;
		Double const_double = 40000.1d;
		
		classWriter.newConst(const_int);
		classWriter.newConst(const_long);
		classWriter.newConst(const_float);
		classWriter.newConst(const_double);

		classWriter.newUTF8("const_utf8");

		Class const_class = Object.class;
		classWriter.newClass("gcp/test/classname");

		int const_handle_index = classWriter.newHandle(H_INVOKEINTERFACE, name, "methodhandle_interfacemethodref", "(LJava/Lang/Object;)V", true);

		int method_descriptor_utf8_index = classWriter.newUTF8("(Ljava/lang/Object;)V");
		int field_descriptor_utf8_index = classWriter.newUTF8("Llava/lang/Object;");


		classWriter.newInvokeDynamic( "call_list_size", "(Ljava/util/List;)I",
			new Handle(
				H_INVOKESTATIC, 
				"com/ibm/j9/jsr292/indyn/BootstrapMethods", 
				"bootstrap_call_list_size",
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
			)
		);

		if (classVersion >= V11) {
			classWriter.newConstantDynamic( "constant_string", "Ljava/lang/String;",
				new Handle(
					H_INVOKESTATIC, 
					"org/openj9/test/condy/BootstrapMethods", 
					"bootstrap_constant_string",
					"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;"
				),
				"world"
			);
		}
		

		classWriter.visitEnd();
		return classWriter.toByteArray();
	}

	public static byte[] generateModule() {
		if (classVersion < V11) {
			return null;
		}
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classWriter.visit(classVersion, ACC_MODULE, "module-info", null, null, null);

		classWriter.newModule("java/base");
		classWriter.newPackage("java/lang");

		classWriter.visitEnd();
		return classWriter.toByteArray();
	}

	public static void dumpToFile(String name, byte[] bytes) {
		String simpleName = name.substring(name.lastIndexOf('/') + 1);
		try (FileOutputStream fos = new FileOutputStream(simpleName + ".dump")) {
			fos.write(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
