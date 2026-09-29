cmake_minimum_required(VERSION 3.18.1)
project("hud_internal")

add_library(
        hud_internal
        SHARED
        main.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        hud_internal
        ${log-lib}
        android
)
