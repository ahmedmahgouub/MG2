#include <jni.h>
#include <string>
#include <vector>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>
#include "Offsets.h"

// هيكل الإحداثيات المتطابق تماماً مع كود الكوتلن
struct Vector3 {
    float x, y, z;
};

// دالة قراءة الذاكرة فائقة السرعة والمستقرة عبر المعالج بالروت (PID)
template <typename T>
T Read(int pid, uintptr_t address) {
    T buffer;
    struct iovec local_io, remote_io;
    local_io.iov_base = &buffer;
    local_io.iov_len = sizeof(T);
    remote_io.iov_base = reinterpret_cast<void*>(address);
    remote_io.iov_len = sizeof(T);
    process_vm_readv(pid, &local_io, 1, &remote_io, 1, 0);
    return buffer;
}

// دالة جلب عنوان اللعبة الأساسي من خرائط نظام الأندرويد الحية
uintptr_t get_module_base(int pid, const char* module_name) {
    uintptr_t addr = 0;
    char maps_path[256];
    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", pid);
    FILE* fp = fopen(maps_path, "r");
    if (fp) {
        char line[512];
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, module_name) && strstr(line, "r-xp")) {
                addr = strtoull(line, nullptr, 16);
                break;
            }
        }
        fclose(fp);
    }
    return addr;
}

// دالة الـ JNI المحترفة المتوافقة مع حزمة com.muhgoub.hud لإرسال البيانات للرادار
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_muhgoub_hud_MemoryUtils_getPlayersLocations(JNIEnv *env, jobject thiz, jint pid) {
    if (pid <= 0) return nullptr;

    uintptr_t base_address = get_module_base(pid, "libUE4.so");
    if (!base_address) return nullptr;

    // القراءة العميقة للذاكرة باستخدام لستة الأوفستات التي اعتمدتها في الملف
    uintptr_t gworld = Read<uintptr_t>(pid, base_address + Offsets::GWorld);
    if (!gworld) return nullptr;

    uintptr_t persistent_level = Read<uintptr_t>(pid, gworld + Offsets::PersistentLevel);
    if (!persistent_level) return nullptr;

    // تم ضبط أبعاد المصفوفة والعداد على الـ 64 بت (0xA0 و 0xA8) لضمان القراءة الصحيحة وتخطي الـ 0
    uintptr_t actor_array = Read<uintptr_t>(pid, persistent_level + 0xA0);
    int actor_count = Read<int>(pid, persistent_level + 0xA8);

    std::vector<Vector3> temp_players;
    int max_actors = (actor_count > 800) ? 800 : actor_count;

    for (int i = 0; i < max_actors; i++) {
        uintptr_t actor = Read<uintptr_t>(pid, actor_array + (i * 8));
        if (!actor) continue;

        uintptr_t root_component = Read<uintptr_t>(pid, actor + Offsets::RootComponent);
        if (!root_component) continue;

        Vector3 location = Read<Vector3>(pid, root_component + Offsets::RelativeLocation);
        if (location.x != 0.0f && location.y != 0.0f) {
            temp_players.push_back(location);
        }
    }

    // إنشاء وتجهيز المصفوفة لإرسالها مباشرة لكود الكوتلن في الواجهة
    jclass vectorClass = env->FindClass("com/muhgoub/hud/MemoryUtils$Vector3");
    if (!vectorClass) return nullptr;
    
    jmethodID constructor = env->GetMethodID(vectorClass, "<init>", "(FFF)V");
    jobjectArray result = env->NewObjectArray(temp_players.size(), vectorClass, nullptr);
    
    for (size_t i = 0; i < temp_players.size(); i++) {
        jobject vecObj = env->NewObject(vectorClass, constructor, temp_players[i].x, temp_players[i].y, temp_players[i].z);
        env->SetObjectArrayElement(result, i, vecObj);
        env->DeleteLocalRef(vecObj);
    }
    
    return result;
}
