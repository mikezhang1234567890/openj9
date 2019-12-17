/*******************************************************************************
 * Copyright (c) 2019, 2019 IBM Corp. and others
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

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class gcp_testClass2 {
    public interface gcp_interface {
        public int anInterfaceMethod();
    }
    public class gcp_implements implements gcp_interface {
        public int anInterfaceMethod() {
            return 1;
        }
    }

    public static class gcp_staticClass {
        public static gcp_staticClass instance = null;
        public static gcp_staticClass getInstance() {
            if (instance == null) {
                instance = new gcp_staticClass();
            }
            return instance;
        }
    }

    public String str1 = "str1";
    public static final gcp_staticClass constant1 = gcp_staticClass.getInstance();


    private void aMethod(int i) throws Throwable {
        float f1 = 1000.0f;
        double d1 = 1000.00d;
        int i1 = 0xabcdefff;
        long l1 = 0xccddeeff;
        gcp_interface lambda = () -> 2;
        int lambdaResult = lambda.anInterfaceMethod();
        final gcp_staticClass constant2 = (gcp_staticClass) ConstantBootstraps.getStaticFinal(MethodHandles.lookup(), "constant1", gcp_staticClass.class, gcp_testClass2.class);



    }

}
