#include <stdio.h>
#include <vector>
#include "boost/dynamic_bitset/dynamic_bitset.hpp"
#include "sclr_core_strategy_L2NormFastWrapper.h"
#include "Combinations.h"

// See: https://www.cakesolutions.net/teamblogs/accessing-scala-objects-via-jni
// See: https://stackoverflow.com/questions/29043872/android-jni-return-multiple-variables

JNIEXPORT jint JNICALL Java_sclr_core_strategy_L2NormFastWrapper_prepare
(JNIEnv *env, jobject javaThis, jobject dataset, jobject workload) {
    // Setup access of our dataset and workload objects
    jclass datasetClass        = env->FindClass("sclr/core/database/Dataset");
    jmethodID dataset_xLength  = env->GetMethodID(datasetClass, "xLength", "()I");
    jclass workloadClass       = env->FindClass("sclr/core/Messages$Workload");
    jmethodID workload_dnfSize = env->GetMethodID(workloadClass, "dnfSize", "()I");
    jmethodID workload_mu      = env->GetMethodID(workloadClass, "mu", "()D");

    // Get Combinations class and methods
    jclass combinationsClass        = env->FindClass("combinations/Combinations");
    jmethodID combinations_Apply    = env->GetStaticMethodID(combinationsClass, "apply", "(II)Lcombinations/Combinations;");
    jmethodID combinations_Iterator = env->GetMethodID(combinationsClass, "iterator", "()Lscala/collection/Iterator;");

    // Get Iterator class and methods
    jclass iteratorClass = env->FindClass("scala/collection/Iterator");
    jmethodID iterator_hasNext = env->GetMethodID(iteratorClass, "hasNext", "()Z");
    jmethodID iterator_next = env->GetMethodID(iteratorClass, "next", "()Ljava/lang/Object;");

    // Construct our Combinations object
    int n = env->CallIntMethod(dataset, dataset_xLength);
    int k = env->CallIntMethod(workload, workload_dnfSize);

    // Our list of terms (bitsets). There should be (xLength choose dnfSize) * 4 of these.
    std::vector< boost::dynamic_bitset<> > terms;

//    std::vector<long long> first   = Combinations::instance()->first(n, k);
//    std::vector<long long> last    = Combinations::instance()->last(n, k);
//    std::vector<long long> current = first;
//    bool didLast = false;
//    while (!didLast) {
//        long long index1 = current[0] + 1;
//        long long index2 = current[1] + 1;
//
//        printf("(%lld %lld)", index1, index2);
////        val combinations = Vector((a, b), (-a, b), (a, -b), (-a, -b))
//
//        didLast = current == last;
//        if (!didLast) {
//            current = Combinations::instance()->next(current);
//        }
//    }

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
