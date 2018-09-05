#include "Dataset.h"

XYZ::XYZ(int id, vector<bool> *x, vector<double> *y, double z) {
    this->id = id;
    this->x = x;
    this->y = y;
    this->z = z;
}

Dataset::Dataset(vector<XYZ *> *data, int xLength, int yLength) {
    this->data = data;
    this->xLength = xLength;
    this->yLength = yLength;
}
