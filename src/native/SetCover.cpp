//
// Created by Hai Son Le on 9/3/18.
//

#include "SetCover.h"

#include <numeric>
#include <algorithm>
#include <functional>
#include <limits>

SetCover::SetCover(vector<dynamic_bitset<> *> *allTerms, map<int, double> *idToRedness, double mu, int beta) {
    this->allTerms = allTerms;
    this->beta = beta;
    this->mu = mu;
    this->idToRedness = idToRedness;
    this->idCount = idToRedness->size();
    unordered_map<dynamic_bitset<>, double> costMap = calculateDnfCostsMap(allTerms);
    this->sortedCost = set<std::pair<dynamic_bitset<> ,double>, comp> (costMap.begin(), costMap.end());
}


double SetCover::degree(int r_i, vector<dynamic_bitset<> *> *sets) {
    return accumulate(sets->begin(), sets->end(), 0.0, [this,&r_i](double a, dynamic_bitset<> b) {
        return a + b.test(r_i) * (*idToRedness)[r_i];
    });
}

double SetCover::errorRate(vector<dynamic_bitset<> *> *kDNF) {
    dynamic_bitset<> setUnion = bitsetUnion(kDNF);
    if (setUnion.count() > 0)
        return rednessOfSets(kDNF)/setUnion.count();
    else
        return numeric_limits<double>::quiet_NaN();
}

double SetCover::rednessOfSets(vector<dynamic_bitset<> *> *sets) {
    return accumulate(sets->begin(), sets->end(), 0.0, [this](double a, dynamic_bitset<> b) {
       return a + rednessOfSet(b);
    });
}

double SetCover::rednessOfSet(dynamic_bitset<> *term) {
    double redness = 0.0;
    for (int i = 0; i < term->size(); i++) {
        if (term->test(i)){
            redness += (*idToRedness)[i];
        }
    }
    return redness;
}

unordered_map<dynamic_bitset<> *, double> *SetCover::calculateDnfCostsMap(vector<dynamic_bitset<> *> *dnfs) {
    auto costMap = new unordered_map<dynamic_bitset<>*, double>();
    for (const auto &dnf : *dnfs) {
        (*costMap)[dnf] = rednessOfSet(dnf)/dnf.count();
    }
    return costMap;
}

dynamic_bitset<> SetCover::bitsetUnion(vector<dynamic_bitset<> *> *sets) {
    dynamic_bitset<> u((*sets)[0]->size());
    for (const auto & set : *sets){
        u = u | set;
    }
    return u;
}





vector<dynamic_bitset<> > SetCover::simpleGreedy() {
    vector<dynamic_bitset<> *> result(idCount);

    for (pair<dynamic_bitset<> *, double> element : sortedCost) {
        result.push_back(element.first);
        if (mu * idCount - bitsetUnion(&result).count() <= 0)
            break;
    }
    return result;
}

double SetCover::minRedness() {
    pair<int, double> min = *min_element(idToRedness->begin(), idToRedness->end(), comp());
    return min.second;
}

double SetCover::totalRedness() {
    return accumulate(begin(*idToRedness), end(*idToRedness), 0.0,
                    [](const double previous, const std::pair<const int, double>& p)
                    { return previous + p.second; });
}

tuple<vector<dynamic_bitset<> >, double> SetCover::lowDegPartial2() {
    double rednessThreshold = std::max(0.01, minRedness());
    double sumRedness = totalRedness();
    double minError = std::numeric_limits<double>::max();
    vector<dynamic_bitset<> > bestKDNF;
    while (rednessThreshold < sumRedness) {
        vector<dynamic_bitset<> > kDNF = simpleGreedy();
        double newError = errorRate(kDNF);
        if (minError > newError) {
            minError = newError;
            bestKDNF = kDNF;
        }
        rednessThreshold *= 1.1;
    }

    return make_tuple(bestKDNF, minError);
}