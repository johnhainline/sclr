# Setup Boost
set(Boost_USE_STATIC_LIBS   ON)
set(Boost_USE_RELEASE_LIBS  ON)
find_package(Boost 1.65 COMPONENTS system REQUIRED)

# Setup JNI
find_package(JNI REQUIRED)
if (JNI_FOUND)
    message (STATUS "JNI include directories: ${JNI_INCLUDE_DIRS}")
endif()


# Include directories
include_directories(.)
include_directories(include)
include_directories(${Boost_INCLUDE_DIRS})
include_directories(${JNI_INCLUDE_DIRS})

#target_link_libraries(l2normfast ${Boost_LIBRARIES})

# Sources
file(GLOB LIB_SRC
    "*.c"
    "*.cc"
    "*.cpp"
)

# Setup installation targets
# (required by sbt-jni) major version should always be appended to library name
#
set (LIB_NAME ${PROJECT_NAME}${PROJECT_VERSION_MAJOR})
add_library(${LIB_NAME} SHARED ${LIB_SRC})
install(TARGETS ${LIB_NAME} LIBRARY DESTINATION .)
