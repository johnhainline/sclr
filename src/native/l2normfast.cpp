#include <memory>
#include <stdio.h>
#include <vector>

#include "boost/dynamic_bitset/dynamic_bitset.hpp"
#include "sclr_core_strategy_L2NormFastWrapper.h"
#include "L2NormSetCover.h"


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

jdoubleArray cToJavaDoubleArray(JNIEnv *env, vector<long double> oldArray) {
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
    // Setup access of our dataset object
    jclass datasetClass        = env->FindClass("sclr/core/database/Dataset");
    jmethodID dataset_xLength  = env->GetMethodID(datasetClass, "xLength", "()I");
    jmethodID dataset_yLength  = env->GetMethodID(datasetClass, "yLength", "()I");
    jmethodID dataset_data     = env->GetMethodID(datasetClass, "data", "()[Lsclr/core/database/XYZ;");

    jclass xyzClass  = env->FindClass("sclr/core/database/XYZ");
    jmethodID xyz_id = env->GetMethodID(xyzClass, "id", "()I");
    jmethodID xyz_x  = env->GetMethodID(xyzClass, "x", "()[Z");
    jmethodID xyz_y  = env->GetMethodID(xyzClass, "y", "()[D");
    jmethodID xyz_z  = env->GetMethodID(xyzClass, "z", "()D");

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
        env->ReleaseBooleanArrayElements(x, xBody, 0);

        auto y = (jdoubleArray)env->CallObjectMethod(xyz, xyz_y);
        auto yLength = (unsigned long)env->GetArrayLength(y);
        auto newY = std::make_unique<vector<double>>(yLength);
        jdouble *yBody = env->GetDoubleArrayElements(y, nullptr);
        for (int i_y = 0; i_y < yLength; i_y++) {
            (*newY)[i_y] = (double)yBody[i_y];
        }
        env->ReleaseDoubleArrayElements(y, yBody, 0);

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
    jclass workClass = env->FindClass("sclr/core/Messages$Work");
    jmethodID work_Index      = env->GetMethodID(workClass, "index", "()I");
    jmethodID work_Dimensions = env->GetMethodID(workClass, "selectedDimensions", "()[I");
    jmethodID work_Rows       = env->GetMethodID(workClass, "selectedRows", "()[I");

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
    // Get a reference to scala's Option class.
    static jclass scalaOptionClass = env->FindClass("scala/Option");
    // Get the static "empty" and "apply" methods from the class, so we can create an option.None or option.Some(...)
    static jmethodID scalaOption_Empty = env->GetStaticMethodID(scalaOptionClass, "empty", "()Lscala/Option;");
    static jmethodID scalaOption_Apply = env->GetStaticMethodID(scalaOptionClass, "apply", "(Ljava/lang/Object;)Lscala/Option;");

    static jclass doubleClass = env->FindClass("java/lang/Double");
    static jmethodID double_Constructor = env->GetMethodID(doubleClass, "<init>", "(D)V");
    static jobject optionNone = env->CallObjectMethod(scalaOptionClass, scalaOption_Empty);

    static jclass resultClass = env->FindClass("sclr/core/database/Result");
    static jmethodID result_Apply = env->GetStaticMethodID(resultClass, "apply", "(I[I[I[DLscala/Option;Lscala/Option;)Lsclr/core/database/Result;");

    jobject optionError = optionNone;
    if (result.someError.has_value()) {
        jobject myDouble = env->NewObject(doubleClass, double_Constructor, result.someError.value());
        optionError = env->CallObjectMethod(scalaOptionClass, scalaOption_Apply, myDouble);
        env->DeleteLocalRef(myDouble);
    }
    jobject optionKDNF = optionNone;
    if (result.someKDNF.has_value()) {
        jstring myString = env->NewStringUTF(result.someKDNF.value().c_str());
        optionKDNF = env->CallObjectMethod(scalaOptionClass, scalaOption_Apply, myString);
        env->DeleteLocalRef(myString);
    }

    jintArray dimensions = cToJavaIntArray(env, result.dimensions);
    jintArray rows = cToJavaIntArray(env, result.rows);
    jdoubleArray coefficients = cToJavaDoubleArray(env, result.coefficients);

    return env->CallObjectMethod(resultClass, result_Apply, result.index, dimensions, rows, coefficients, optionError, optionKDNF);
}

static unique_ptr<L2NormSetCover> l2NormSetCover = nullptr;

JNIEXPORT jint JNICALL Java_sclr_core_strategy_L2NormFastWrapper_prepare
(JNIEnv *env, jobject javaThis, jobject jdataset, jobject workload) {
    // Setup access of our dataset and workload objects
    jclass workloadClass       = env->FindClass("sclr/core/Messages$Workload");
    jmethodID workload_dnfSize = env->GetMethodID(workloadClass, "dnfSize", "()I");
    jmethodID workload_mu      = env->GetMethodID(workloadClass, "mu", "()D");
    int dnfSize = env->CallIntMethod(workload, workload_dnfSize);
    double mu   = env->CallIntMethod(workload, workload_mu);

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
