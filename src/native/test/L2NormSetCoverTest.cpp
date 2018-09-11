#include <vector>
#include "gtest/gtest.h"
#include "L2NormSetCover.h"

using namespace std;

TEST(L2NormSetCover, simple) {

    auto xyz1x_unique = make_unique<vector<bool>>(*new vector<bool>({false, false, false}));
    auto xyz2x_unique = make_unique<vector<bool>>(*new vector<bool>({false, false, true}));
    auto xyz3x_unique = make_unique<vector<bool>>(*new vector<bool>({false, true, false}));
    auto xyz4x_unique = make_unique<vector<bool>>(*new vector<bool>({false, true, true}));
    auto xyz5x_unique = make_unique<vector<bool>>(*new vector<bool>({true, false, false}));
    auto xyz6x_unique = make_unique<vector<bool>>(*new vector<bool>({true, false, true}));
    auto xyz7x_unique = make_unique<vector<bool>>(*new vector<bool>({true, true, false}));
    auto xyz8x_unique = make_unique<vector<bool>>(*new vector<bool>({true, true, true}));

    auto xyz1y_unique = make_unique<vector<double>>(*new vector<double>({8.0, 0.0}));
    auto xyz2y_unique = make_unique<vector<double>>(*new vector<double>({8.0, 3.0}));
    auto xyz3y_unique = make_unique<vector<double>>(*new vector<double>({5.0, 0.0}));
    auto xyz4y_unique = make_unique<vector<double>>(*new vector<double>({5.0, 3.6}));
    auto xyz5y_unique = make_unique<vector<double>>(*new vector<double>({-2.0, 0.0}));
    auto xyz6y_unique = make_unique<vector<double>>(*new vector<double>({-2.0, 3.2}));
    auto xyz7y_unique = make_unique<vector<double>>(*new vector<double>({1.0, 0.0}));
    auto xyz8y_unique = make_unique<vector<double>>(*new vector<double>({1.0, 3.0}));

    auto xyz1 = make_unique<XYZ>(1, std::move(xyz1x_unique), std::move(xyz1y_unique), 0.01);
    auto xyz2 = make_unique<XYZ>(2, std::move(xyz2x_unique), std::move(xyz2y_unique), 0.10);
    auto xyz3 = make_unique<XYZ>(3, std::move(xyz3x_unique), std::move(xyz3y_unique), 0.80);
    auto xyz4 = make_unique<XYZ>(4, std::move(xyz4x_unique), std::move(xyz4y_unique), 0.12);
    auto xyz5 = make_unique<XYZ>(5, std::move(xyz5x_unique), std::move(xyz5y_unique), 0.01);
    auto xyz6 = make_unique<XYZ>(6, std::move(xyz6x_unique), std::move(xyz6y_unique), 0.20);
    auto xyz7 = make_unique<XYZ>(7, std::move(xyz7x_unique), std::move(xyz7y_unique), 2.00);
    auto xyz8 = make_unique<XYZ>(8, std::move(xyz8x_unique), std::move(xyz8y_unique), 0.15);

    auto data = make_unique<vector<unique_ptr<XYZ>>>();
    data->push_back(std::move(xyz1));
    data->push_back(std::move(xyz2));
    data->push_back(std::move(xyz3));
    data->push_back(std::move(xyz4));
    data->push_back(std::move(xyz5));
    data->push_back(std::move(xyz6));
    data->push_back(std::move(xyz7));
    data->push_back(std::move(xyz8));
    auto dataset = make_unique<Dataset>(std::move(data), 3, 2);

    auto l2norm = L2NormSetCover(std::move(dataset), 2, 0.1);
    auto work1 = Work(0, {0,1}, {0,1});


    auto result = l2norm.run(work1);


    auto expected = Result(work1.index, work1.dimensions, work1.rows, {0.0, 0.0}, nullopt, nullopt);
    EXPECT_EQ(result.index, expected.index);
}
