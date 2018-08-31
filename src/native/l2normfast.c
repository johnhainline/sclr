#include <stdio.h>
#include <zconf.h>
#include "sclr_core_strategy_L2NormFastWrapper.h"

// See: https://www.cakesolutions.net/teamblogs/accessing-scala-objects-via-jni
// See: https://stackoverflow.com/questions/29043872/android-jni-return-multiple-variables

JNIEXPORT jint JNICALL Java_sclr_core_strategy_L2NormFastWrapper_prepare
(JNIEnv *env, jobject javaThis, jobject dataset, jstring name, jint dnfSize, jdouble mu, jdouble epsilon, jint subset, jint seed) {

    return 1;
}

JNIEXPORT jobject JNICALL Java_sclr_core_strategy_L2NormFastWrapper_run
(JNIEnv *env, jobject javaThis, jobject work) {

    // Get a reference to scala's Option class.
    jclass scalaOptionClass = (*env)->FindClass(env, "scala/Option");
    // Get the static "empty" and "apply" methods from the class, so we can create an option.None or option.Some(...)
    jmethodID scalaOption_Empty = (*env)->GetStaticMethodID(env, scalaOptionClass, "empty", "()Lscala/Option;");
    jmethodID scalaOption_Apply = (*env)->GetStaticMethodID(env, scalaOptionClass, "apply", "(Ljava/lang/Object;)Lscala/Option;");

    jclass doubleClass = (*env)->FindClass(env, "java/lang/Double");
    jmethodID double_Constructor = (*env)->GetMethodID(env, doubleClass, "<init>", "(D)V");
    jobject myDouble = (*env)->NewObject(env, doubleClass, double_Constructor, 0.01);
    jstring myString = (*env)->NewStringUTF(env, "blah");

    jobject none = (*env)->CallObjectMethod(env, scalaOptionClass, scalaOption_Empty);
    jobject someDouble = (*env)->CallObjectMethod(env, scalaOptionClass, scalaOption_Apply, myDouble);
    jobject someString = (*env)->CallObjectMethod(env, scalaOptionClass, scalaOption_Apply, myString);

    jclass resultClass = (*env)->FindClass(env, "sclr/core/database/Result");
    jmethodID result_Apply = (*env)->GetStaticMethodID(env, resultClass, "apply", "(I[I[I[DLscala/Option;Lscala/Option;)Lsclr/core/database/Result;");

    jclass workClass = (*env)->FindClass(env, "sclr/core/Messages$Work");
    jmethodID work_Index      = (*env)->GetMethodID(env, workClass, "index", "()I");
    jmethodID work_Dimensions = (*env)->GetMethodID(env, workClass, "selectedDimensions", "()[I");
    jmethodID work_Rows       = (*env)->GetMethodID(env, workClass, "selectedRows", "()[I");


    jint      index      = (*env)->CallIntMethod(env, work, work_Index);
    jintArray dimensions = (jintArray)(*env)->CallObjectMethod(env, work, work_Dimensions);
    jintArray rows       = (jintArray)(*env)->CallObjectMethod(env, work, work_Rows);

    double coeffs[] = {1.23, 4.32};
    jsize len = sizeof(coeffs)/sizeof(*coeffs);
    jdoubleArray coefficients = (*env)->NewDoubleArray(env, len);
    (*env)->SetIntArrayRegion(env, coefficients, 0, len, coeffs);

    jobject result = (*env)->CallObjectMethod(env, resultClass, result_Apply, index, dimensions, rows, coefficients, someDouble, someString);
    return result;
}
