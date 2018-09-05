#include <unordered_map>
#include <iterator>
#include <cmath>
#include <climits>
#include "Combinations.h"

Combinations *Combinations::s_instance = nullptr;

Combinations::Combinations() {}

Combinations &Combinations::instance() {
    if (!s_instance)
        s_instance = new Combinations();
    return *s_instance;
}

vector<long long> Combinations::first(long long n, long long k) {
    std::vector<long long> first(k);
    for (long long i = 0; i < k; i++) {
        first[i] = i;
    }
    return first;
}

vector<long long> Combinations::last(long long n, long long k) {
    std::vector<long long> last(k);
    for (long long j = 0, i = n-k; i < n; j++, i++) {
        last[j] = i;
    }
    return last;
}

long long Combinations::choose(long long n, long long k) {
    if (n < k) return 0;
    long long lowK = n - k < k ? n-k : k;
    return chooseIterative(n, lowK);
}

// Declare our map type
typedef tuple<long long, long long> map_key_t;
struct key_hash : public std::unary_function<map_key_t, std::size_t> {
    std::size_t operator()(const map_key_t& k) const {
        return (size_t)(std::get<0>(k) * 41) ^ 2 + (std::get<1>(k) * 41);
    }
};
struct key_equal_fn {
    bool operator()(const map_key_t& k1, const map_key_t& k2) const {
        return std::get<0>(k1) == std::get<0>(k2) && std::get<1>(k1) == std::get<1>(k2);
    }
};
typedef std::unordered_map<const map_key_t, long long, key_hash, key_equal_fn> map_t;

// this can be accessed by all instances but only one is every created.
static map_t & createCache() {
    static map_t cache;
    return cache;
}
static map_t & getCache() {
    static map_t & cache = createCache(); // this is only called once!
    return cache; // subsequent calls just return the single instance.
}

long long Combinations::chooseIterative(long long n, long long k) {
    std::tuple<long long, long long> key(n, k);
    map_t & cache = getCache();
    auto value = cache.find(key);
    if (value != cache.end()) {
        return value->second;
    } else {
        long double accum = 1.0;
        for (long long i = 0; i < k; i++) {
            accum = accum * (n-i) / (k-i);
        }
        auto finalValue = (long long)accum;
        cache.insert({key, finalValue});
        return finalValue;
    }
}

long long Combinations::rank(vector<long long> indexes) {
    long long accum = 0;
    for (int index = 0; index < indexes.size(); index++) {
        long long bit = indexes[index];
        accum = accum + choose(bit, index + 1);
    }
    return accum;
}

vector<long long> Combinations::unrank(long long k, long long index) {
    long long m = index;
    std::vector<long long> buffer {};
    for (long long i = k; i >= 1; i--) {
        auto l = findLargestN(i, m, i-1, LLONG_MAX);
        buffer.insert(buffer.begin(), l);
        m -= choose(l, i);
    }
    return buffer;
}

vector<long long> Combinations::next(vector<long long> combination) {
    std::vector<long long> result {};
    long long index = findIndex(combination);
    for (long long i = 0; i < combination.size(); i++) {
        if (i < index) {
            result.push_back(i);
        } else if (i == index) {
            result.push_back(combination[i] + 1);
        } else {
            result.push_back(combination[i]);
        }
    }
    return result;

}

/*  We know that for
 *   1 <= k <= n:
 *   (n/k)^k <= (n k) <= (ne/k)^k
 *   However our index "a" can be anything in the range k-1 to (n k).
 *   so given (n k) = a, our max possible n is k/e * a^(1/k)
 *   whereas our min possible n is k-1.
 */
tuple<long long, long long> Combinations::boundsOfNGivenIndex(long long k, long long a) {
    static long double e = std::exp(1.0);
    long double high = k * nthRoot(k, a) * e;
    std::tuple<long long, long long> result(k - 1, (long long)std::ceil(high) + k);
    return result;
}

// Start at n=k-1
// Find largest n, such that Combinations.choose(n, k) <= index, then return n-1
// We assume that smallest n is between [low, high].
long long Combinations::findLargestN(long long k, long long index, long long low, long long high) {
    if (low == high) {
        return low;
    } else {
        auto middle = (long long)std::ceil(((long double)(high + low)) / 2.0);
        bool upper = choose(middle, k) <= index;
        long long newLow = upper ? middle : low;
        long long newHigh = upper ? high : middle-1;
        findLargestN(k, index, newLow, newHigh);
    }
}

long double Combinations::nthRoot(long long n, long double y) {
    if (n > 10) {
        return nthRoot_bsearch(n, y);
    } else {
        return nthRoot_newton(n, y);
    }
}

long double Combinations::newton_loop(long long n, long double y, long double x0) {
    long double x1 = 1.0/n * ((n-1) * x0 + y/std::pow(x0, n-1));
    if (x0 <= x1) {
        return x0;
    } else {
        return newton_loop(n, y, x1);
    }
}

long double Combinations::nthRoot_newton(long long n, long double y) {
    if (y == 0) {
        return 0.0;
    } else {
        newton_loop(n, y, y/2.0);
    }
}

long double Combinations::nthRoot_bsearch(long long n, long double x) {
    long double low = 0.0;
    long double high = x;
    long double middle = (high + low) / 2.0;
    long double result = std::pow(middle, n);

    while (result != x) {
        middle = (high + low) / 2.0;
        result = std::pow(middle, n);
        if (middle == low || middle == high) {
            return middle;
        }
        if (result > x) {
            high = middle;
        } else {
            low = middle;
        }
    }
    return middle;
}

long long Combinations::findIndex(vector<long long> combination) {
    long long index = 0;
    if (combination.size() > 1) {
        bool foundIndex = false;
        while (!foundIndex && index < combination.size() - 1) {
            long long current = combination[index];
            long long next = combination[index+1];
            if (current + 1 < next) {
                foundIndex = true;
            } else {
                index++;
            }
        }
        if (!foundIndex) {
            index = (long long)combination.size() - 1;
        }
    }
    return index;
}
