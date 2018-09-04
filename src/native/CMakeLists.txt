################################################################
# A minimal CMake file that is compatible with sbt-jni         #
#                                                              #
# All settings required by sbt-jni have been marked so, please #
# add/modify/remove settings to build your specific library.   #
################################################################

cmake_minimum_required(VERSION 3.12)

# Define project and related variables
# (required by sbt-jni) please use semantic versioning
#
project (l2normfast)
set(PROJECT_VERSION_MAJOR 1)
set(PROJECT_VERSION_MINOR 0)
set(PROJECT_VERSION_PATCH 0)
set(CMAKE_CXX_STANDARD 20)

add_subdirectory(src)
add_subdirectory(test)

# Include directories
