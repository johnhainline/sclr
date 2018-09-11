#include <utility>

#include <utility>

#include "Result.h"

Result::Result(long long index, vector<long long> dimensions, vector<long long> rows, vector<long double> coefficients, optional<long double> someError, optional<string> someKDNF) {
    this->index = index;
    this->dimensions = std::move(dimensions);
    this->rows = std::move(rows);
    this->coefficients = std::move(coefficients);
    this->someError = std::move(someError);
    this->someKDNF = std::move(someKDNF);
}
