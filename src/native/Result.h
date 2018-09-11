#ifndef L2NORMFAST_RESULT_H
#define L2NORMFAST_RESULT_H

#include <optional>
#include <vector>

using namespace std;

class Result {
public:
    Result(long long index, vector<long long> dimensions, vector<long long> rows, vector<long double> coefficients, optional<long double> someError, optional<string> someKDNF);

    long long index;
    vector<long long> dimensions;
    vector<long long> rows;
    vector<long double> coefficients;
    optional<long double> someError;
    optional<string> someKDNF;
};


#endif //L2NORMFAST_RESULT_H
