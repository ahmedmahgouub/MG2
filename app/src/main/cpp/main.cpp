#include <jni.h>
#include <string>
#include <vector>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>

struct Vector3 {
    float x, y, z;
};

// الأوفست الجديد الذي أرسلته في الصورة للـ GEngine
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

    // 1. الدخول عبر بوابة الـ GEngine غير المشفّرة
    uintptr_t gengine = Read<uintptr_t>(pid, base_address + O_GEngine);
    if (!gengine) return nullptr;

    // 2. الانتقال داخل هيكل المحرك: GameViewportClient (أوفست 0x780 القياسي)
    uintptr_t game_viewport = Read<uintptr_t>(pid, gengine + 0x780);
    if (!game_viewport) return nullptr;

    // 3. جلب الـ World النظيف المباشر (أوفست 0x80 القياسي للـ Viewport في المحرك)
    uintptr_t gworld = Read<uintptr_t>(pid, game_viewport + 0x80);
    if (!gworld) return nullptr;

    // 4. قراءة الـ PersistentLevel ومصفوفة الكائنات كالعادة
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
