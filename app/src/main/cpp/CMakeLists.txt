cmake_minimum_required(VERSION 3.10.2)

project("hud_internal")

set(CMAKE_CURRENT_SOURCE_DIR ${CMAKE_CURRENT_LIST_DIR})

add_library(
    hud_internal
    SHARED
    "${CMAKE_CURRENT_SOURCE_DIR}/main.cpp"
)

find_library(
    log-lib
    log
)

target_link_libraries(
    hud_internal
    ${log-lib}
)
