#include "Dataset.h"

XYZ::XYZ(long long id, unique_ptr<vector<bool>> x, unique_ptr<vector<double>> y, double z) {
    this->id = id;
    this->x = move(x);
    this->y = move(y);
    this->z = z;
}

Dataset::Dataset(unique_ptr<vector<unique_ptr<XYZ>>> data, int xLength, int yLength) {
    this->data = move(data);
    this->xLength = xLength;
    this->yLength = yLength;
}
