cmake_minimum_required(VERSION 3.10.2)

# اسم المكتبة الداخلية للمشروع
project("hud_internal")

# ربط ملفات الكود مباشرة بدون علامات اقتباس معقدة مسببة للخطأ
add_library(
    hud_internal
    SHARED
    main.cpp
)

# البحث عن مكتبة الـ Log الخاصة بالأندرويد
find_library(
    log-lib
    log
)

# دمج المكتبات معاً لضمان البناء المستقر بنجاح
target_link_libraries(
    hud_internal
    ${log-lib}
)
