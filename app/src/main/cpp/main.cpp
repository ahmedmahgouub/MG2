#include <jni.h>
#include <string>
#include <vector>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>
#include <cstdio>
#include "Offsets.h"

struct Vector3 {
    float x, y, z;
};

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

// 🟢 محرك المسح الديناميكي الشامل لقنص بصمة الـ GWorld الحية من قلب الذاكرة بدون أوفستات ثابتة
uintptr_t scan_gworld_dynamic(int pid, uintptr_t base_address, size_t search_size) {
    std::vector<uint8_t> memory_buffer(search_size);
    struct iovec local_io, remote_io;
    local_io.iov_base = memory_buffer.data();
    local_io.iov_len = search_size;
    remote_io.iov_base = reinterpret_cast<void*>(base_address);
    remote_io.iov_len = search_size;

    if (process_vm_readv(pid, &local_io, 1, &remote_io, 1, 0) <= 0) return 0;

    // البصمة الفولاذية المحدثة لمحرك 64 بت القياسي المفتوح للتخطي الفوري وعزل الحماية
    const uint8_t signature[] = { 0x02, 0x00, 0x80, 0x52, 0x01, 0x00, 0x00, 0x14, 0x00, 0x00, 0x80, 0xD2 };
    const char* mask = "xxxxxx??xxxx";
    size_t sig_len = sizeof(signature);

    for (size_t i = 0; i < search_size - sig_len; i++) {
        bool match = true;
        for (size_t j = 0; j < sig_len; j++) {
            if (mask[j] == 'x' && memory_buffer[i + j] != signature[j]) {
                match = false;
                break;
            }
        }
        if (match) {
            // حساب العنوان الحقيقي حركياً بناءً على إزاحة بايتات المحرك الحية
            uintptr_t instruction_addr = base_address + i + 12;
            int32_t relative_offset = *reinterpret_cast<int32_t*>(&memory_buffer[i + 8]) & 0x00FFFFFF;
            if (relative_offset & 0x00800000) relative_offset |= 0xFF000000;
            return instruction_addr + (relative_offset * 4);
        }
    }
    return 0;
}

uintptr_t decrypt_gworld(uintptr_t encrypted_gworld) {
    if (!encrypted_gworld) return 0;
    uintptr_t key = encrypted_gworld ^ 0x5C2E7A4B9F1D8E30ULL; 
    return (key >> 16) | (key << 48); 
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_muhgoub_hud_MemoryUtils_getPlayersLocations(JNIEnv *env, jobject thiz, jint pid, jlong base_address_java) {
    if (pid <= 0 || base_address_java <= 0) return nullptr;

    uintptr_t base_address = (uintptr_t)base_address_java;
    char status_buf = {0};

    // استدعاء محرك المسح الديناميكي لمسح 96 ميجا بايت من الذاكرة الحية وقنص العنوان الحقيقي فوراً
    uintptr_t gworld_address = scan_gworld_dynamic(pid, base_address, 0x6000000);
    
    // خط دفاع احتياطي صلب إذا تأخر المسح الحركي في ساحة الانتظار لضمان عدم حدوث تعليقة
    if (!gworld_address) {
        gworld_address = base_address + 0xF624D40; 
    }

    uintptr_t encrypted_gworld = Read<uintptr_t>(pid, gworld_address);
    uintptr_t gworld = decrypt_gworld(encrypted_gworld);
    
    std::vector<Vector3> temp_players;
    int actor_count = 0;

    if (gworld) {
        uintptr_t persistent_level = Read<uintptr_t>(pid, gworld + Offsets::PersistentLevel);
        if (persistent_level) {
            uintptr_t actor_array = Read<uintptr_t>(pid, persistent_level + Offsets::ActorArray); 
            actor_count = Read<int>(pid, persistent_level + Offsets::ActorCount);        

            if (actor_count > 0 && actor_count < 2000) {
                int max_actors = (actor_count > 800) ? 800 : actor_count;

                for (int i = 0; i < max_actors; i++) {
                    uintptr_t actor = Read<uintptr_t>(pid, actor_array + (i * 8));
                    if (!actor) continue;

                    uintptr_t root_component = Read<uintptr_t>(pid, actor + Offsets::RootComponent);
                    if (!root_component) continue;

                    Vector3 location = Read<Vector3>(pid, root_component + Offsets::RelativeLocation);
                    if (location.x != 0.0f && location.y != 0.0f) {
                        temp_players.push_back(Vector3{location.x, location.y, location.z});
                    }
                }
            }
        }
    }

    if (gworld) {
        snprintf(status_buf, sizeof(status_buf), "PID: %d | Base: 0x%lx | Players: %d", pid, base_address, (int)temp_players.size());
    } else {
        snprintf(status_buf, sizeof(status_buf), "PID: %d | Base: 0x%lx | In Lobby", pid, base_address);
    }
    
    jclass memoryUtilsClass = env->FindClass("com/muhgoub/hud/MemoryUtils");
    if (memoryUtilsClass) {
        jfieldID statusField = env->GetStaticFieldID(memoryUtilsClass, "nativeStatusMessage", "Ljava/lang/String;");
        if (statusField) {
            jstring statusStr = env->NewStringUTF(status_buf);
            env->SetStaticObjectField(memoryUtilsClass, statusField, statusStr);
            env->DeleteLocalRef(statusStr);
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
