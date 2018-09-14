//
// Created by Hai Son Le on 9/3/18.
//

#include "L2NormSetCover.h"
#include "Combinations.h"

#include <numeric>
#include <algorithm>
#include <functional>
#include <limits>
#include <cmath>


// PRIVATE

// Helpers (L2NormSetCover constructor)

void L2NormSetCover::createTermsForIndices(const Dataset *dataset, vector<unique_ptr<Term>> &allTerms, long long i1, long long i2) {
    i1++;
    i2++;
    vector<vector<long long>> allForIndices = {{i1, i2}, {i1, -i2}, {-i1, i2}, {-i1, -i2}};
    auto &data = dataset->data;
    for (auto indices : allForIndices) {
        auto bitset = std::make_unique<boost::dynamic_bitset<>>(data->size());
        for (int i = 0; i < data->size(); i++) {
            auto first = indices[0];
            auto second = indices[1];
            const auto &x = (*data)[i]->x;
            (*bitset)[i] = (*x)[abs(first)-1] == first > 0 && (*x)[abs(second)-1] == second > 0;
        }
        auto term = std::make_unique<Term>(std::move(bitset), i1, i2);
        allTerms.push_back(std::move(term));
    }
}

void L2NormSetCover::createAllTerms(unique_ptr<Dataset> &dataset, long long n, long long k) {
    auto termCount = (unsigned long)Combinations::instance().choose(n, k) * 4;

    // Our list of terms (bitsets). There should be (xLength choose dnfSize) * 4 of these.
    this->allTerms = vector<unique_ptr<Term>>();

    vector<long long> first   = Combinations::instance().first(n, k);
    vector<long long> last    = Combinations::instance().last(n, k);
    vector<long long> current = first;
    bool didLast = false;
    while (!didLast) {
        createTermsForIndices(dataset.get(), allTerms, current[0], current[1]);

        didLast = current == last;
        if (!didLast) {
            current = Combinations::instance().next(current);
        }
    }
}

// PRIVATE STATIC

pair<double, double> L2NormSetCover::calculateCoefficients(unique_ptr<Dataset> &dataset, Work &work) {
    auto & yDimensions = work.dimensions;
    auto & rows = work.rows;
    unique_ptr<XYZ> & xyz1 = (*dataset->data)[rows[0]];
    unique_ptr<XYZ> & xyz2 = (*dataset->data)[rows[1]];

    double x1 = (*xyz1->y)[yDimensions[0]];
    double y1 = (*xyz1->y)[yDimensions[1]];
    double z1 = xyz1->z;

    double x2 = (*xyz2->y)[yDimensions[0]];
    double y2 = (*xyz2->y)[yDimensions[1]];
    double z2 = xyz2->z;
    double a1 = (z1 * y2 - z2 * y1) / (x1 * y2 - x2 * y1);
    double a2 = (x1 * z2 - x2 * z1) / (x1 * y2 - x2 * y1);
    return make_pair(a1, a2);
}

bool L2NormSetCover::coefficientsValid(double a1, double a2) {
    return isnormal(a1) && isnormal(a2);
}

bool comparePairs(const std::pair<const Term *, double>& l, const std::pair<const Term *, double>& r) {
    return l.second < r.second;
}

void L2NormSetCover::deriveRednessMaps(vector<long long> &yDimensions, double a1, double a2) {

    // idToRedness map
    this->idToRedness = unordered_map<long long, double>();
    for (unique_ptr<XYZ> &xyz : *dataset->data) {
        double redness = pow(xyz->z - (a1 * (*xyz->y)[yDimensions[0]]) - a2 * ((*xyz->y)[yDimensions[1]]), 2);
        idToRedness[xyz->id] = redness;
    }

    // termToAverageRedness map
    this->termToAverageRedness = unordered_map<const Term *, double>();
    for (unique_ptr<Term> & termUnique : allTerms) {
        const Term *term = termUnique.get();
        double termRedness = 0.0;
        size_t index = term->bitset->find_first();
        while(index != boost::dynamic_bitset<>::npos) {
            termRedness += idToRedness[index];
            index = term->bitset->find_next(index);
        }
        double averageRedness = termRedness / (double)term->bitset->count();
        termToAverageRedness[term] = averageRedness;
    }

    // sortedTermAndAverageRedness vector
    this->sortedTermAndAverageRedness = vector<pair<const Term *, double>>();
    for (auto pair : termToAverageRedness) {
        sortedTermAndAverageRedness.push_back(pair);
    }

    std::sort(begin(sortedTermAndAverageRedness), end(sortedTermAndAverageRedness), comparePairs);
}

dynamic_bitset<> L2NormSetCover::bitsetUnion(vector<const Term *> &terms) {
    unsigned long length = terms[0]->bitset->size();
    auto u = dynamic_bitset<>(length);
    for (const auto & term : terms) {
        u |= (*term->bitset);
    }
    return u;
}

double L2NormSetCover::rednessOfTerm(const Term *term) {
    double redness = 0.0;
    size_t index = term->bitset->find_first();
    while(index != boost::dynamic_bitset<>::npos) {
        redness += idToRedness[index];
        index = term->bitset->find_next(index);
    }
    return redness;
}

double L2NormSetCover::rednessOfTerms(vector<const Term *> &terms) {
    double redness = 0.0;
    for (auto term : terms) {
        redness += rednessOfTerm(term);
    }
    return redness;
}

double L2NormSetCover::errorRate(vector<const Term *> &terms) {
    dynamic_bitset<> setUnion = bitsetUnion(terms);
    if (setUnion.count() > 0)
        return rednessOfTerms(terms) / setUnion.count();
    else
        return numeric_limits<double>::quiet_NaN();
}

// PUBLIC

// Constructor
L2NormSetCover::L2NormSetCover(unique_ptr<Dataset> dataset, int dnfSize, double mu) {
    this->dataset = std::move(dataset);
    this->mu = mu;

    int n = this->dataset->xLength;
    int k = dnfSize;

    createAllTerms(this->dataset, n, k);
}

Result L2NormSetCover::run(Work &work) {

    auto p = calculateCoefficients(dataset, work);
    auto a1 = p.first;
    auto a2 = p.second;
    if (coefficientsValid(a1, a2)) {

        // Derive idToRedness, termToAverageRedness, sortedTermAndAverageRedness
        deriveRednessMaps(work.dimensions, a1, a2);

        auto minRedness = std::numeric_limits<double>::max();
        double totalRedness = 0.0;
        for (auto pair : idToRedness) {
            auto redness = pair.second;
            minRedness = std::min(minRedness, redness);
            totalRedness += redness;
        }

        double min = 0.01;
        double rednessThreshold = std::max(min, minRedness);
        auto minError = std::numeric_limits<double>::max();
        unsigned long idCount = idToRedness.size();
        vector<const Term *> bestTerms;
        while (rednessThreshold < totalRedness) {
            vector<const Term *> terms;
            for (auto termAndRedness : sortedTermAndAverageRedness) {
                terms.push_back(termAndRedness.first);
                auto termUnion = bitsetUnion(terms);
                if (mu * idCount - termUnion.size() <= 0)
                    break;
            }
            double newError = errorRate(terms);
            if (minError > newError) {
                minError = newError;
                bestTerms = terms;
            }
            rednessThreshold *= 1.1;
        }

        string kdnf;
        for (auto term : bestTerms) {
            kdnf += "(" + to_string(term->i1) + ", " + to_string(term->i2) + ") ";
        }
        return Result(work.index, work.dimensions, work.rows, {a1, a2}, {minError}, {kdnf});
    } else {
        return Result(work.index, work.dimensions, work.rows, {0, 0}, std::nullopt, std::nullopt);
    }
}
