cmake_minimum_required(VERSION 3.20)
project(example CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)

file(GLOB EXAMPLE_SOURCES "src/*.cpp" "src/*.cc")

add_library(example SHARED ${EXAMPLE_SOURCES})
target_include_directories(example PRIVATE ${JNI_INCLUDE_DIRS} include)
target_link_libraries(example PRIVATE ${JNI_LIBRARIES})
