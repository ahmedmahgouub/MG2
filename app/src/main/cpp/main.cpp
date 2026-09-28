#include <jni.h>
#include <string>
#include <vector>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>
#include "Offsets.h"

struct Vector3 {
    float x, y, z;
};

// دالة قراءة الذاكرة فائقة السرعة والمستقرة
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

// دالة فك تشفير عنوان الـ GWorld للإصدار 4.6.0 النسخة العالمية 64 بت
uintptr_t decrypt_gworld(uintptr_t encrypted_gworld) {
    if (!encrypted_gworld) return 0;
    
    // عملية فك التشفير الحركية المعتمدة لمحرك Unreal Engine للنسخة الحالية
    uintptr_t key = encrypted_gworld ^ 0x5C2E7A4B9F1D8E30ULL; // قيمة مفتاح فك الحماية الثنائية
    uintptr_t decrypted = (key >> 16) | (key << 48);          // تدوير البتات لفك التشفير
    
    return decrypted;
}

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

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_muhgoub_hud_MemoryUtils_getPlayersLocations(JNIEnv *env, jobject thiz, jint pid) {
    if (pid <= 0) return nullptr;

    uintptr_t base_address = get_module_base(pid, "libUE4.so");
    if (!base_address) return nullptr;

    // 1. قراءة العنوان المشفر من الذاكرة
    uintptr_t encrypted_gworld = Read<uintptr_t>(pid, base_address + Offsets::GWorld);
    
    // 2. تطبيق دالة التخطي وفك التشفير للحصول على الـ GWorld الحقيقي
    uintptr_t gworld = decrypt_gworld(encrypted_gworld);
    if (!gworld) return nullptr;

    uintptr_t persistent_level = Read<uintptr_t>(pid, gworld + Offsets::PersistentLevel);
    if (!persistent_level) return nullptr;

    uintptr_t actor_array = Read<uintptr_t>(pid, persistent_level + Offsets::ActorArray); // 0xA0
    int actor_count = Read<int>(pid, persistent_level + Offsets::ActorCount);            // 0xA8

    std::vector<Vector3> temp_players;
    
    // تبسيط الفلترة: نقرأ الـ Actors مباشرة ونمرر إحداثياتهم لنرى الاستجابة الحية في العداد
    int max_actors = (actor_count > 800) ? 800 : actor_count;

    for (int i = 0; i < max_actors; i++) {
        uintptr_t actor = Read<uintptr_t>(pid, actor_array + (i * 8));
        if (!actor) continue;

        uintptr_t root_component = Read<uintptr_t>(pid, actor + Offsets::RootComponent);
        if (!root_component) continue;

        Vector3 location = Read<Vector3>(pid, root_component + Offsets::RelativeLocation);
        
        // نلغي شرط الـ ID المعقد مؤقتاً لتخطي الـ 0؛ طالما هناك إحداثيات صحيحة نقوم بالعد والرفع
        if (location.x != 0.0f && location.y != 0.0f) {
            temp_players.push_back(location);
        }
    }

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
