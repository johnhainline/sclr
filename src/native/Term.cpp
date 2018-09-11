#include "Term.h"

Term::Term(unique_ptr<dynamic_bitset<>> bitset, long long i1, long long i2) {
    this->bitset = std::move(bitset);
    this->i1 = i1;
    this->i2 = i2;
}
