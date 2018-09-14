//
// Created by Hai Son Le on 9/3/18.
//

#ifndef SETCOVER_SETCOVER_H
#define SETCOVER_SETCOVER_H

#include "boost/dynamic_bitset/dynamic_bitset.hpp"
#include <memory>
#include "Dataset.h"
#include "Work.h"
#include "Result.h"
#include "Term.h"
#include <vector>
#include <tuple>
#include <unordered_map>
#include <map>

using namespace std;
using namespace boost;

class L2NormSetCover {

private:
    // Computed
    vector<unique_ptr<Term>> allTerms;

    unordered_map<long long, double> idToRedness;
    unordered_map<const Term *, double> termToAverageRedness;
    vector<std::pair<const Term *, double>> sortedTermAndAverageRedness;

    // Given
    unique_ptr<Dataset> dataset;
    double mu;

    // Helpers (L2NormSetCover constructor)
    void createAllTerms(unique_ptr<Dataset> &dataset, long long n, long long k);
    void createTermsForIndices(const Dataset *dataset, vector<unique_ptr<Term>> &allTerms, long long i1, long long i2);

    // Helpers (L2NormSetCover run)
    static pair<double, double> calculateCoefficients(unique_ptr<Dataset> &dataset, Work &work);
    static bool coefficientsValid(double a1, double a2);
    void deriveRednessMaps(vector<long long> &yDimensions, double a1, double a2);
    dynamic_bitset<> bitsetUnion(vector<const Term *> &terms);
    double rednessOfTerm(const Term *term);
    double rednessOfTerms(vector<const Term *> &terms);
    double errorRate(vector<const Term *> &terms);
public:
    L2NormSetCover(unique_ptr<Dataset> dataset, int dnfSize, double mu);
    Result run(Work &work);

};

#endif //SETCOVER_SETCOVER_H
