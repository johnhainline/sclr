#ifndef L2NORMFAST_DATASET_H
#define L2NORMFAST_DATASET_H

#include <utility>
#include <vector>

using namespace std;

class XYZ {
public:
    XYZ(int id, vector<bool> *x, vector<double> *y, double z);
    int id;
    vector<bool> *x;
    vector<double> *y;
    double z;
};


class Dataset {
public:
    Dataset(vector<XYZ *> *data, int xLength, int yLength);
    vector<XYZ *> *data;
    int xLength;
    int yLength;
};

#endif //L2NORMFAST_DATASET_H
