#include <vector>
#include "gtest/gtest.h"
#include "Combinations.h"

using namespace std;

TEST(sample_test_case, sample_test) {
    EXPECT_EQ(1, 1);
}

TEST(Combinations, first) {
    auto expected = vector<long long> {0, 1};

    auto result = Combinations::instance().first(10, 2);

    EXPECT_EQ(result, expected);
}

TEST(Combinations, last) {
    auto expected = vector<long long> {8, 9};

    auto result = Combinations::instance().last(10, 2);

    EXPECT_EQ(result, expected);
}

TEST(Combinations, choose) {
    long long expected = 45;

    auto result = Combinations::instance().choose(10, 2);

    EXPECT_EQ(result, expected);
}

TEST(Combinations, next) {
    // 6 choose 3
    auto expected = vector<vector<long long>> { { 0, 1, 2},
                                                { 0, 1, 3},
                                                { 0, 2, 3},
                                                { 1, 2, 3},
                                                { 0, 1, 4},
                                                { 0, 2, 4},
                                                { 1, 2, 4},
                                                { 0, 3, 4},
                                                { 1, 3, 4},
                                                { 2, 3, 4},
                                                { 0, 1, 5},
                                                { 0, 2, 5},
                                                { 1, 2, 5},
                                                { 0, 3, 5},
                                                { 1, 3, 5},
                                                { 2, 3, 5},
                                                { 0, 4, 5},
                                                { 1, 4, 5},
                                                { 2, 4, 5},
                                                { 3, 4, 5} };

    auto first  = Combinations::instance().first(6, 3);
    auto choose = Combinations::instance().choose(6, 3);
    auto last   = Combinations::instance().last(6, 3);

    EXPECT_EQ(first, expected[0]);
    EXPECT_EQ(choose, expected.size());
    EXPECT_EQ(last, expected[expected.size()-1]);

    auto current = first;
    for (const auto &i : expected) {
        EXPECT_EQ(current, i);
        current = Combinations::instance().next(current);
    }
}
