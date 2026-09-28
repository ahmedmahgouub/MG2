cmake_minimum_required(VERSION 3.10.2)

# اسم المكتبة الحركية للمشروع المتوافق مع الربط
project("hud_internal")

# تجميع الأكواد التي قمنا بإنشائها معاً كمكتبة مشتركة
add_library(
    hud_internal
    SHARED
    main.cpp
)

# البحث عن مكتبة الـ Log الخاصة بالأندرويد لطباعة البيانات
find_library(
    log-lib
    log
)

# ربط المكتبات ببعضها لضمان البناء المستقر بنجاح
target_link_libraries(
    hud_internal
    ${log-lib}
)
