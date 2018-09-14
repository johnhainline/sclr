#include <memory>
#include <stdio.h>
#include <vector>

#include "boost/dynamic_bitset/dynamic_bitset.hpp"
#include "sclr_core_strategy_L2NormFastWrapper.h"
#include "L2NormSetCover.h"

// See http://normanmaurer.me/blog/2014/07/25/JNI-Performance-The-Saga-continues/

jclass datasetClass;
jmethodID dataset_xLength;
jmethodID dataset_yLength;
jmethodID dataset_data;

jclass xyzClass;
jmethodID xyz_id;
jmethodID xyz_x;
jmethodID xyz_y;
jmethodID xyz_z;

jclass workClass;
jmethodID work_Index;
jmethodID work_Dimensions;
jmethodID work_Rows;

jclass scalaOptionClass;
jmethodID scalaOption_Empty;
jmethodID scalaOption_Apply;

jclass doubleClass;
jmethodID double_Constructor;

jclass resultClass;
jmethodID result_Apply;

jclass workloadClass;
jmethodID workload_dnfSize;
jmethodID workload_mu;

// Is automatically called once the native code is loaded via System.loadLibary(...);
jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    } else {
        jclass localDatasetClass = env->FindClass("sclr/core/database/Dataset");
        datasetClass             = (jclass) env->NewGlobalRef(localDatasetClass);
        dataset_xLength          = env->GetMethodID(datasetClass, "xLength", "()I");
        dataset_yLength          = env->GetMethodID(datasetClass, "yLength", "()I");
        dataset_data             = env->GetMethodID(datasetClass, "data", "()[Lsclr/core/database/XYZ;");

        jclass localXyzClass = env->FindClass("sclr/core/database/XYZ");
        xyzClass             = (jclass) env->NewGlobalRef(localXyzClass);
        xyz_id               = env->GetMethodID(xyzClass, "id", "()I");
        xyz_x                = env->GetMethodID(xyzClass, "x", "()[Z");
        xyz_y                = env->GetMethodID(xyzClass, "y", "()[D");
        xyz_z                = env->GetMethodID(xyzClass, "z", "()D");

        jclass localWorkClass = env->FindClass("sclr/core/Messages$Work");
        workClass             = (jclass) env->NewGlobalRef(localWorkClass);
        work_Index            = env->GetMethodID(workClass, "index", "()I");
        work_Dimensions       = env->GetMethodID(workClass, "selectedDimensions", "()[I");
        work_Rows             = env->GetMethodID(workClass, "selectedRows", "()[I");

        jclass localScalaOptionClass = env->FindClass("scala/Option");
        scalaOptionClass             = (jclass) env->NewGlobalRef(localScalaOptionClass);
        scalaOption_Empty            = env->GetStaticMethodID(scalaOptionClass, "empty", "()Lscala/Option;");
        scalaOption_Apply            = env->GetStaticMethodID(scalaOptionClass, "apply", "(Ljava/lang/Object;)Lscala/Option;");

        jclass localDoubleClass = env->FindClass("java/lang/Double");
        doubleClass             = (jclass) env->NewGlobalRef(localDoubleClass);
        double_Constructor      = env->GetMethodID(doubleClass, "<init>", "(D)V");

        jclass localResultClass = env->FindClass("sclr/core/database/Result");
        resultClass             = (jclass) env->NewGlobalRef(localResultClass);
        result_Apply            = env->GetStaticMethodID(resultClass, "apply", "(I[I[I[DLscala/Option;Lscala/Option;)Lsclr/core/database/Result;");

        jclass localWorkloadClass = env->FindClass("sclr/core/Messages$Workload");
        workloadClass             = (jclass) env->NewGlobalRef(localWorkloadClass);
        workload_dnfSize          = env->GetMethodID(workloadClass, "dnfSize", "()I");
        workload_mu               = env->GetMethodID(workloadClass, "mu", "()D");
    }
    return JNI_VERSION_1_8;
}

// Is automatically called once the Classloader is destroyed
void JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        // Something is wrong but nothing we can do about this :(
        return;
    } else {
        // delete global references so the GC can collect them
        if (datasetClass != nullptr) {
            env->DeleteGlobalRef(datasetClass);
        }
        if (xyzClass != nullptr) {
            env->DeleteGlobalRef(xyzClass);
        }
        if (workClass != nullptr) {
            env->DeleteGlobalRef(workClass);
        }
        if (scalaOptionClass != nullptr) {
            env->DeleteGlobalRef(scalaOptionClass);
        }
        if (doubleClass != nullptr) {
            env->DeleteGlobalRef(doubleClass);
        }
        if (resultClass != nullptr) {
            env->DeleteGlobalRef(resultClass);
        }
        if (workloadClass != nullptr) {
            env->DeleteGlobalRef(workloadClass);
        }
    }
}


// See: https://www.cakesolutions.net/teamblogs/accessing-scala-objects-via-jni
// See: https://stackoverflow.com/questions/29043872/android-jni-return-multiple-variables

jintArray cToJavaIntArray(JNIEnv *env, vector<long long> oldArray) {
    unsigned long length = oldArray.size();
    jintArray newArray = env->NewIntArray((jsize)length);
    jint *narr = env->GetIntArrayElements(newArray, nullptr);
    for (int i = 0; i < length; i++) {
        narr[i] = (jint)oldArray[i];
    }
    env->ReleaseIntArrayElements(newArray, narr, 0);
    return newArray;
}

jdoubleArray cToJavaDoubleArray(JNIEnv *env, vector<double> oldArray) {
    unsigned long length = oldArray.size();
    jdoubleArray newArray = env->NewDoubleArray((jsize)length);
    jdouble *narr = env->GetDoubleArrayElements(newArray, nullptr);
    for (int i = 0; i < length; i++) {
        narr[i] = (jdouble)oldArray[i];
    }
    env->ReleaseDoubleArrayElements(newArray, narr, 0);
    return newArray;
}

unique_ptr<Dataset> convertDatasetToNativeType(JNIEnv *env, jobject javaThis, jobject dataset) {

    auto data = (jobjectArray)env->CallObjectMethod(dataset, dataset_data);
    auto dataLength = (unsigned long)env->GetArrayLength(data);
    auto newData = std::make_unique<vector<unique_ptr<XYZ>>>(dataLength);
    for(int i = 0; i < dataLength; i++) {
        auto xyz = (jobject)env->GetObjectArrayElement(data, i);

        auto x = (jbooleanArray)env->CallObjectMethod(xyz, xyz_x);
        auto xLength = (unsigned long)env->GetArrayLength(x);
        auto newX = std::make_unique<vector<bool>>(xLength);
        jboolean *xBody = env->GetBooleanArrayElements(x, nullptr);
        for (int i_x = 0; i_x < xLength; i_x++) {
            (*newX)[i_x] = (bool)(xBody[i_x] != JNI_FALSE);
        }
        env->ReleaseBooleanArrayElements(x, xBody, JNI_ABORT);

        auto y = (jdoubleArray)env->CallObjectMethod(xyz, xyz_y);
        auto yLength = (unsigned long)env->GetArrayLength(y);
        auto newY = std::make_unique<vector<double>>(yLength);
        jdouble *yBody = env->GetDoubleArrayElements(y, nullptr);
        for (int i_y = 0; i_y < yLength; i_y++) {
            (*newY)[i_y] = (double)yBody[i_y];
        }
        env->ReleaseDoubleArrayElements(y, yBody, JNI_ABORT);

        auto id = (int)env->CallIntMethod(xyz, xyz_id);
        auto z = (double)env->CallIntMethod(xyz, xyz_z);
        auto newXYZ = std::make_unique<XYZ>(id, std::move(newX), std::move(newY), z);
        (*newData)[i] = std::move(newXYZ);
    }

    auto xLength = (int)env->CallIntMethod(dataset, dataset_xLength);
    auto yLength = (int)env->CallIntMethod(dataset, dataset_yLength);
    return std::make_unique<Dataset>(std::move(newData), xLength, yLength);
}

Work convertWorkToNativeType(JNIEnv *env, jobject javaThis, jobject work) {
    auto index = (long long)env->CallIntMethod(work, work_Index);

    auto dimensions = (jintArray)env->CallObjectMethod(work, work_Dimensions);
    auto dLength = (unsigned long)env->GetArrayLength(dimensions);
    auto newD = vector<long long>(dLength);
    jint *dBody = env->GetIntArrayElements(dimensions, nullptr);
    for (int i = 0; i < dLength; i++) {
        newD[i] = (long long)dBody[i];
    }
    env->ReleaseIntArrayElements(dimensions, dBody, 0);

    auto rows = (jintArray)env->CallObjectMethod(work, work_Rows);
    auto rLength = (unsigned long)env->GetArrayLength(rows);
    auto newR = vector<long long>(rLength);
    jint *rBody = env->GetIntArrayElements(rows, nullptr);
    for (int i = 0; i < dLength; i++) {
        newR[i] = (long long)rBody[i];
    }
    env->ReleaseIntArrayElements(rows, rBody, 0);

    return Work(index, newD, newR);
}

jobject convertNativeTypeToResult(JNIEnv *env, jobject javaThis, Result result) {
    jobject optionNone = env->CallStaticObjectMethod(scalaOptionClass, scalaOption_Empty);
    jobject optionError = optionNone;

    if (result.someError.has_value()) {
        // only allow a double value between 0 and 10e14
        double value = max(0.0, min(result.someError.value(), 10e14));
        jobject myDouble = env->NewObject(doubleClass, double_Constructor, value);
        optionError = env->CallStaticObjectMethod(scalaOptionClass, scalaOption_Apply, myDouble);
        env->DeleteLocalRef(myDouble);
    }
    jobject optionKDNF = optionNone;
    if (result.someKDNF.has_value()) {
        string kdnf = result.someKDNF.value();
        jstring myString = env->NewStringUTF(kdnf.c_str());
        optionKDNF = env->CallStaticObjectMethod(scalaOptionClass, scalaOption_Apply, myString);
        env->DeleteLocalRef(myString);
    }

    jintArray dimensions = cToJavaIntArray(env, result.dimensions);
    jintArray rows = cToJavaIntArray(env, result.rows);
    jdoubleArray coefficients = cToJavaDoubleArray(env, result.coefficients);

    return env->CallStaticObjectMethod(resultClass, result_Apply, result.index, dimensions, rows, coefficients, optionError, optionKDNF);
}

static unique_ptr<L2NormSetCover> l2NormSetCover = nullptr;

JNIEXPORT jint JNICALL Java_sclr_core_strategy_L2NormFastWrapper_prepare
(JNIEnv *env, jobject javaThis, jobject jdataset, jobject workload) {
    // Setup access of our dataset and workload objects
    int dnfSize = env->CallIntMethod(workload, workload_dnfSize);
    double mu   = env->CallDoubleMethod(workload, workload_mu);

    auto dataset = convertDatasetToNativeType(env, javaThis, jdataset);

    l2NormSetCover = make_unique<L2NormSetCover>(std::move(dataset), dnfSize, mu);

    return 1;
}

JNIEXPORT jobject JNICALL Java_sclr_core_strategy_L2NormFastWrapper_run
(JNIEnv *env, jobject javaThis, jobject work) {
    Work nativeWork = convertWorkToNativeType(env, javaThis, work);

    auto result = l2NormSetCover->run(nativeWork);

    return convertNativeTypeToResult(env, javaThis, result);
}
