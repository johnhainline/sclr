#ifndef L2NORMFAST_COMBINATIONS_H
#define L2NORMFAST_COMBINATIONS_H

#include <vector>
#include <tuple>
#include <iterator>

using namespace std;

class Combinations {
public:
    static Combinations &instance();

    vector<long long> first(long long n, long long k);
    vector<long long> last(long long n, long long k);

    long long choose(long long n, long long k);
    long long rank(vector<long long> indexes);
    vector<long long> unrank(long long k, long long index);
    vector<long long> next(vector<long long> combination);

private:
    static Combinations *s_instance;
    Combinations();
    long long chooseIterative(long long n, long long k);
    tuple<long long, long long> boundsOfNGivenIndex(long long k, long long a);
    long long findLargestN(long long k, long long index, long long low, long long high);
    long double newton_loop(long long n, long double y, long double x0);
    long double nthRoot(long long n, long double y);
    long double nthRoot_newton(long long n, long double y);
    long double nthRoot_bsearch(long long n, long double x);
    long long findIndex(vector<long long> combination);
};

#endif //L2NORMFAST_COMBINATIONS_H
