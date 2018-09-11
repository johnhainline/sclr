#ifndef L2NORMFAST_WORK_H
#define L2NORMFAST_WORK_H

#include <optional>
#include <vector>

using namespace std;


class Work {
public:
    Work(long long index, vector<long long> dimensions, vector<long long> rows);

    long long index;
    vector<long long> dimensions;
    vector<long long> rows;
};


#endif //L2NORMFAST_WORK_H
