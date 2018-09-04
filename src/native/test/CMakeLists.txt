
include_directories(..)
include_directories(../include)

add_executable(tests tests.cpp)

target_link_libraries(tests gtest_main l2normfast1)
