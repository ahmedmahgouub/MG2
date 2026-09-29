#include <jni.h>
#include <string>
#include <vector>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>

struct Vector3 {
    float x, y, z;
};

// دالة قراءة الذاكرة فائقة السرعة والمستقرة عبر الروت
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

// دالة جلب عنوان ومقدار حجم مكتبة اللعبة من خرائط النظام
uintptr_t get_module_base_and_size(int pid, const char* module_name, size_t &size) {
    uintptr_t addr = 0;
    size = 0;
    char maps_path[64]; 
    snprintf(maps_path, sizeof(maps_path), "/proc/%d/maps", pid);
    FILE* fp = fopen(maps_path, "r");
    if (fp) {
        char line[512]; 
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, module_name) && strstr(line, "r-xp")) {
                uintptr_t start = 0, end = 0;
                if (sscanf(line, "%lx-%lx", &start, &end) == 2) {
                    if (addr == 0) addr = start;
                    size += (end - start);
                }
            }
        }
        fclose(fp);
    }
    return addr;
}

// دالة الـ Pattern Scanner الذكية لمسح ذاكرة المعالج 64 بت حية
uintptr_t scan_pattern(int pid, uintptr_t base, size_t size, const char* pattern, const char* mask) {
    size_t pattern_len = strlen(mask);
    std::vector<uint8_t> buffer(size);
    
    struct iovec local_io, remote_io;
    local_io.iov_base = buffer.data();
    local_io.iov_len = size;
    remote_io.iov_base = reinterpret_cast<void*>(base);
    remote_io.iov_len = size;
    
    if (process_vm_readv(pid, &local_io, 1, &remote_io, 1, 0) <= 0) return 0;
    
    for (size_t i = 0; i < size - pattern_len; i++) {
        bool found = true;
        for (size_t j = 0; j < pattern_len; j++) {
            if (mask[j] != '?' && buffer[i + j] != static_cast<uint8_t>(pattern[j])) {
                found = false;
                break;
            }
        }
        if (found) return base + i;
    }
    return 0;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_muhgoub_hud_MemoryUtils_getPlayersLocations(JNIEnv *env, jobject thiz, jint pid) {
    if (pid <= 0) return nullptr;

    size_t module_size = 0;
    uintptr_t base_address = get_module_base_and_size(pid, "libUE4.so", module_size);
    if (!base_address || module_size == 0) return nullptr;

    // 1. بصمة الـ Hex القياسية للمحرك 64 بت للبحث عن الـ GEngine ديناميكياً
    // التوقيع يبحث عن كود الآلة الافتراضي للـ ViewportClient وتوجيهات المحرك
    const char* gengine_pattern = "\x7F\x45\x4C\x46\x02\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00"; 
    const char* gengine_mask    = "xxxx????????xxxx"; // علامات الاستفهام للبايتات الحركية المتغيرة
    
    uintptr_t gengine_ptr = scan_pattern(pid, base_address, module_size, gengine_pattern, gengine_mask);
    if (!gengine_ptr) return nullptr;

    uintptr_t gengine = Read<uintptr_t>(pid, gengine_ptr);
    if (!gengine) return nullptr;

    // 2. الانتقال الداخلي الآمن داخل الهيكل البنائي للمحرك
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
