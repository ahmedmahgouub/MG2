#include <jni.h>
#include <string>
#include <vector>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>

struct Vector3 {
    float x, y, z;
};

// الأوفست الحقيقي والصحيح للـ GEngine
#define O_GEngine 0xEDC6210

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

// دالة فك تشفير مؤشر الـ GEngine الموجه للـ 64 بت لفك حماية الذاكرة الحية
uintptr_t decrypt_gengine(uintptr_t encrypted_ptr) {
    if (!encrypted_ptr) return 0;
    
    // عملية فك التشفير القياسية: فك حظر تدوير البتات وعملية الـ XOR الحركية
    uintptr_t key = encrypted_ptr ^ 0x9D7C5B3A1E2F4D60ULL; // مفتاح الحماية الافتراضي المتوافق
    uintptr_t decrypted = (key >> 24) | (key << 40);         // تدوير الخانات لإصلاح العنوان المكسور
    
    return decrypted;
}

uintptr_t get_module_base(int pid, const char* module_name) {
    uintptr_t addr = 0;
    char maps_path; 
    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", pid);
    FILE* fp = fopen(maps_path, "r");
    if (fp) {
        char line; 
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

    // 1. قراءة مؤشر الـ GEngine المشفر من الذاكرة
    uintptr_t encrypted_gengine = Read<uintptr_t>(pid, base_address + O_GEngine);
    
    // 2. تمرير القيمة المشوهة على دالة فك التعمية لإصلاح مسار الذاكرة
    uintptr_t gengine = decrypt_gengine(encrypted_gengine);
    if (!gengine) return nullptr;

    // 3. التحرك الآمن داخل الهيكل المصلح للمحرك
    uintptr_t game_viewport = Read<uintptr_t>(pid, gengine + 0x780); // GameViewportClient = 0x780
    if (!game_viewport) return nullptr;

    uintptr_t gworld = Read<uintptr_t>(pid, game_viewport + 0x80); // WorldPtr = 0x80
    if (!gworld) return nullptr;

    uintptr_t persistent_level = Read<uintptr_t>(pid, gworld + 0x30); // PersistentLevel = 0x30
    if (!persistent_level) return nullptr;

    uintptr_t actor_array = Read<uintptr_t>(pid, persistent_level + 0xA0); // ActorArray = 0xA0
    int actor_count = Read<int>(pid, persistent_level + 0xA8);            // ActorCount = 0xA8

    std::vector<Vector3> temp_players;
    int max_actors = (actor_count > 800) ? 800 : actor_count;

    for (int i = 0; i < max_actors; i++) {
        uintptr_t actor = Read<uintptr_t>(pid, actor_array + (i * 8));
        if (!actor) continue;

        uintptr_t root_component = Read<uintptr_t>(pid, actor + 0x208); // RootComponent = 0x208
        if (!root_component) continue;

        Vector3 location = Read<Vector3>(pid, root_component + 0x1E4); // RelativeLocation = 0x1E4
        if (location.x != 0.0f && location.y != 0.0f) {
            temp_players.push_back(Vector3{location.x, location.y, location.z});
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
