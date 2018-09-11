#include "Work.h"

Work::Work(long long index, vector<long long> dimensions, vector<long long> rows) {
    this->index = index;
    this->dimensions = std::move(dimensions);
    this->rows = std::move(rows);
}
