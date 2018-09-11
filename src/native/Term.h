#ifndef L2NORMFAST_TERM_H
#define L2NORMFAST_TERM_H

#include <memory>
#include "boost/dynamic_bitset/dynamic_bitset.hpp"

using namespace std;
using namespace boost;

class Term {
public:
    Term(unique_ptr<dynamic_bitset<>> bitset, long long i1, long long i2);
    unique_ptr<dynamic_bitset<>> bitset;
    long long i1, i2;
};


#endif //L2NORMFAST_TERM_H
