//
// Created by Hai Son Le on 9/3/18.
//

#ifndef SETCOVER_SETCOVER_H
#define SETCOVER_SETCOVER_H

#include "boost/dynamic_bitset/dynamic_bitset.hpp"
#include <vector>
#include <tuple>
#include <unordered_map>
#include <map>
#include <set>

using namespace std;
using namespace boost;

class SetCover {
private:
    struct comp {
        bool operator()(const  std::pair<const dynamic_bitset<>, double>& l, const std::pair<const dynamic_bitset<>, double>& r) const {
            if (l.second != r.second)
                return l.second < r.second;
            else
                return l.first.to_ulong() < r.first.to_ulong();
        }
    };
    vector<dynamic_bitset<> *> *allTerms;
    map<int, double> *idToRedness;
    double mu;
    int beta;
    unsigned long idCount;
    set<pair<dynamic_bitset<> *, double>, comp> sortedCost;
public:
    SetCover(vector<dynamic_bitset<> *> *allTerms, map<int, double> *idToRedness, double mu, int beta);
    tuple<vector<dynamic_bitset<> >, double> lowDegPartial2();

private:
    unordered_map<dynamic_bitset<> *, double> *calculateDnfCostsMap(vector<dynamic_bitset<> *> *dnfs);
    double degree(int r_i, vector<dynamic_bitset<> *> *sets);
    double rednessOfSet(dynamic_bitset<> *term);
    double rednessOfSets(vector<dynamic_bitset<> *> *sets);
    double errorRate(vector<dynamic_bitset<> *>* kDNF);
    vector<dynamic_bitset<> *> *simpleGreedy();
    dynamic_bitset<> bitsetUnion(vector<dynamic_bitset<> *> *sets);
    double minRedness();
    double totalRedness();
};

#endif //SETCOVER_SETCOVER_H
