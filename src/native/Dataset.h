#ifndef L2NORMFAST_DATASET_H
#define L2NORMFAST_DATASET_H

#include <utility>
#include <vector>
#include <memory>

using namespace std;

class XYZ {
public:
    XYZ(long long id, unique_ptr<vector<bool>> x, unique_ptr<vector<double>> y, double z);
    long long id;
    unique_ptr<vector<bool>> x;
    unique_ptr<vector<double>> y;
    double z;
};


class Dataset {
public:
    Dataset(unique_ptr<vector<unique_ptr<XYZ>>> data, int xLength, int yLength);
    unique_ptr<vector<unique_ptr<XYZ>>> data;
    int xLength;
    int yLength;
};

#endif //L2NORMFAST_DATASET_H
