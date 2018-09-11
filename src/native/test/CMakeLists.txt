
include_directories(..)
include_directories(../include)

add_executable(tests CombinationsTest.cpp L2NormSetCoverTest.cpp)

target_link_libraries(tests gtest_main l2normfast1)
