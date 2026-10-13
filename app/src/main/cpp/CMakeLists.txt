cmake_minimum_required(VERSION 3.22.1)
project(zenith_arranger_native LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

add_library(zenith_arranger_native SHARED
    AudioEngine.cpp
    JniBridge.cpp
)

target_include_directories(zenith_arranger_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(zenith_arranger_native PRIVATE
    oboe::oboe
    log
    android
)
