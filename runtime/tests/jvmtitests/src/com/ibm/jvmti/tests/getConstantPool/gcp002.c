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

#include <string.h>

#include "ibmjvmti.h"
#include "jvmti_test.h"

static agentEnv * env;

jint JNICALL
gcp002(agentEnv * agent_env, char * args)
{
	JVMTI_ACCESS_FROM_AGENT(agent_env);
	jvmtiCapabilities capabilities;
	jvmtiError err;

    env = agent_env;

    memset(&capabilities, 0, sizeof(jvmtiCapabilities));
    capabilities.can_get_constant_pool = 1;
    err = (*jvmti_env)->AddCapabilities(jvmti_env, &capabilities);
	if (err != JVMTI_ERROR_NONE) {
		error(agent_env, err, "Failed to add capabilities");
		return JNI_ERR;
	}

	return JNI_OK;
}

jbyteArray JNICALL
Java_com_ibm_jvmti_tests_getConstantPool_gcp002_constantPoolBytesWrapper(JNIEnv *jni_env, jclass clazz, jclass cls)
{
    JVMTI_ACCESS_FROM_AGENT(env);
    jvmtiError err;
    jint cp_count = 0;
    jint cp_bytecount = 0;
    unsigned char *cp_bytes = NULL;

    err = (*jvmti_env)->GetConstantPool(jvmti_env, cls, &cp_count, &cp_bytecount, &cp_bytes);
    if (err != JVMTI_ERROR_NONE) {
        char * error;
        if ((*jvmti_env)->GetErrorName(jvmti_env, err, &error) != JVMTI_ERROR_NONE) {
            error = "could not get constant pool from jvmti";
        }
        jclass clazz = (*jni_env)->FindClass(jni_env, "java/lang/Error");
	    if (clazz != NULL) {
		    (*jni_env)->ThrowNew(jni_env, clazz, error);
	    }
    }
    jbyteArray ret = (*jni_env)->NewByteArray(jni_env, cp_bytecount);
    if (ret == NULL) {
        jclass clazz = (*jni_env)->FindClass(jni_env, "java/lang/Error");
	    if (clazz != NULL) {
		    (*jni_env)->ThrowNew(jni_env, clazz, "could not copy constant pool bytes into array");
	    }
    }
    (*jni_env)->SetByteArrayRegion(jni_env, ret, 0, cp_bytecount, (jbyte *)cp_bytes);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_ibm_jvmti_tests_getConstantPool_gcp002_constantPoolCountWrapper(JNIEnv *jni_env, jclass clazz, jclass cls)
{
    JVMTI_ACCESS_FROM_AGENT(env);
    jvmtiError err;
    jint cp_count = 0;
    jint cp_bytecount = 0;
    unsigned char *cp_bytes = NULL;

    err = (*jvmti_env)->GetConstantPool(jvmti_env, cls, &cp_count, &cp_bytecount, &cp_bytes);
    if (err != JVMTI_ERROR_NONE) {
        char * error;
        if ((*jvmti_env)->GetErrorName(jvmti_env, err, &error) != JVMTI_ERROR_NONE) {
            error = "could not get constant pool from jvmti";
        }
        jclass clazz = (*jni_env)->FindClass(jni_env, "java/lang/Error");
	    if (clazz != NULL) {
		    (*jni_env)->ThrowNew(jni_env, clazz, error);
	    }
    }
    return cp_count;
}

