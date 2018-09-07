#include <stdio.h>
#include <vector>
#include "boost/dynamic_bitset/dynamic_bitset.hpp"
#include "sclr_core_strategy_L2NormFastWrapper.h"
#include "Combinations.h"
#include "Dataset.h"

using namespace std;

// See: https://www.cakesolutions.net/teamblogs/accessing-scala-objects-via-jni
// See: https://stackoverflow.com/questions/29043872/android-jni-return-multiple-variables

unique_ptr<Dataset> convertToNativeType(JNIEnv *env, jobject javaThis, jobject dataset) {
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
    vector<unique_ptr<XYZ>> newData = new vector<XYZ*>(dataLength);
    for(int i = 0; i < dataLength; i++) {
        auto xyz = (jobject)env->GetObjectArrayElement(data, i);

        auto x = (jbooleanArray)env->CallObjectMethod(xyz, xyz_x);
        auto xLength = (unsigned long)env->GetArrayLength(x);
        vector<bool> *newX = new vector<bool>(xLength);
        jboolean *xBody = env->GetBooleanArrayElements(x, nullptr);
        for (int i_x = 0; i_x < xLength; i_x++) {
            (*newX)[i_x] = (bool)(xBody[i_x] != JNI_FALSE);
        }
        env->ReleaseBooleanArrayElements(x, xBody, 0);

        auto y = (jdoubleArray)env->CallObjectMethod(xyz, xyz_y);
        auto yLength = (unsigned long)env->GetArrayLength(y);
        vector<double> *newY = new vector<double>(yLength);
        jdouble *yBody = env->GetDoubleArrayElements(y, nullptr);
        for (int i_y = 0; i_y < yLength; i_y++) {
            (*newY)[i_y] = (double)yBody[i_y];
        }
        env->ReleaseDoubleArrayElements(y, yBody, 0);

        auto id = (int)env->CallIntMethod(xyz, xyz_id);
        auto z = (double)env->CallIntMethod(xyz, xyz_z);
        XYZ *newXYZ = new XYZ(id, newX, newY, z);
        (*newData)[i] = newXYZ;
    }

    auto xLength = (int)env->CallIntMethod(dataset, dataset_xLength);
    auto yLength = (int)env->CallIntMethod(dataset, dataset_yLength);
    return new Dataset(newData, xLength, yLength);
}

vector<boost::dynamic_bitset<> *> createBitSetsForIndices(Dataset *dataset, long long i1, long long i2) {
    vector<vector<tuple<long long, bool>>> allForIndices = {{{i1, true}, {i2, true}}, {{i1, true}, {i2, false}}, {{i1, false}, {i2, true}}, {{i1, false}, {i2, false}}};
    vector<boost::dynamic_bitset<> *> result(allForIndices.size());
    for (int indicesIndex = 0; indicesIndex < allForIndices.size(); indicesIndex++) {
        auto indices = allForIndices[indicesIndex];
        auto data = dataset->data;
        auto size = data->size();
        boost::dynamic_bitset<> *bitset = new boost::dynamic_bitset<>(size);
        for (int i = 0; i < size; i++) {
            auto first = indices[0];
            auto second = indices[1];
            vector<bool> *x = (*data)[i]->x;
            (*bitset)[i] = (*x)[get<0>(first)] == get<1>(first) && (*x)[get<0>(second)] == get<1>(second);
        }
        result[indicesIndex] = bitset;
    }
    return result;
}

static vector<boost::dynamic_bitset<> *> *terms = nullptr;

JNIEXPORT jint JNICALL Java_sclr_core_strategy_L2NormFastWrapper_prepare
(JNIEnv *env, jobject javaThis, jobject jdataset, jobject workload) {
    // Setup access of our dataset and workload objects
    jclass workloadClass       = env->FindClass("sclr/core/Messages$Workload");
    jmethodID workload_dnfSize = env->GetMethodID(workloadClass, "dnfSize", "()I");
    jmethodID workload_mu      = env->GetMethodID(workloadClass, "mu", "()D");
    int dnfSize = env->CallIntMethod(workload, workload_dnfSize);
    double mu   = env->CallIntMethod(workload, workload_mu);

    auto dataset = convertToNativeType(env, javaThis, jdataset);
    int n = dataset->xLength;
    int k = dnfSize;

    auto termCount = (unsigned long)Combinations::instance().choose(n, k) * 4;

    // Our list of terms (bitsets). There should be (xLength choose dnfSize) * 4 of these.
    terms = new vector<boost::dynamic_bitset<> *>(termCount);

    vector<long long> first   = Combinations::instance().first(n, k);
    vector<long long> last    = Combinations::instance().last(n, k);
    vector<long long> current = first;
    bool didLast = false;
    while (!didLast) {

        auto bitsets = createBitSetsForIndices(dataset, current[0], current[1]);
        for (const auto & bitset : bitsets) {
            terms->push_back(bitset);
        }

        didLast = current == last;
        if (!didLast) {
            current = Combinations::instance().next(current);
        }
    }

    return 1;
}

JNIEXPORT jobject JNICALL Java_sclr_core_strategy_L2NormFastWrapper_run
(JNIEnv *env, jobject javaThis, jobject work) {

    // Get a reference to scala's Option class.
    jclass scalaOptionClass = env->FindClass("scala/Option");
    // Get the static "empty" and "apply" methods from the class, so we can create an option.None or option.Some(...)
    jmethodID scalaOption_Empty = env->GetStaticMethodID(scalaOptionClass, "empty", "()Lscala/Option;");
    jmethodID scalaOption_Apply = env->GetStaticMethodID(scalaOptionClass, "apply", "(Ljava/lang/Object;)Lscala/Option;");

    jclass doubleClass = env->FindClass("java/lang/Double");
    jmethodID double_Constructor = env->GetMethodID(doubleClass, "<init>", "(D)V");
    jobject myDouble = env->NewObject(doubleClass, double_Constructor, 0.01);
    jstring myString = env->NewStringUTF("blah");

    jobject none = env->CallObjectMethod(scalaOptionClass, scalaOption_Empty);
    jobject someDouble = env->CallObjectMethod(scalaOptionClass, scalaOption_Apply, myDouble);
    jobject someString = env->CallObjectMethod(scalaOptionClass, scalaOption_Apply, myString);

    jclass resultClass = env->FindClass("sclr/core/database/Result");
    jmethodID result_Apply = env->GetStaticMethodID(resultClass, "apply", "(I[I[I[DLscala/Option;Lscala/Option;)Lsclr/core/database/Result;");

    jclass workClass = env->FindClass("sclr/core/Messages$Work");
    jmethodID work_Index      = env->GetMethodID(workClass, "index", "()I");
    jmethodID work_Dimensions = env->GetMethodID(workClass, "selectedDimensions", "()[I");
    jmethodID work_Rows       = env->GetMethodID(workClass, "selectedRows", "()[I");


    jint      index      = env->CallIntMethod(work, work_Index);
    jintArray dimensions = (jintArray)env->CallObjectMethod(work, work_Dimensions);
    jintArray rows       = (jintArray)env->CallObjectMethod(work, work_Rows);

    double coeffs[] = {1.23, 4.32};
    jsize len = sizeof(coeffs)/sizeof(*coeffs);
    jdoubleArray coefficients = env->NewDoubleArray(len);
    env->SetDoubleArrayRegion(coefficients, 0, len, coeffs);

    jobject result = env->CallObjectMethod(resultClass, result_Apply, index, dimensions, rows, coefficients, someDouble, someString);
    return result;
}
