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

// 🟢 دالة فك التشفير الحركية المحدثة والمعتمدة للإصدار الحالي 4.6.0 لكسر الـ 0 في الساحة
uintptr_t decrypt_gworld(uintptr_t encrypted_gworld) {
    if (!encrypted_gworld) return 0;
    
    // فك التعمية عن طريق الضرب في المفتاح السري وعمل تدوير البتات المتوازن للـ 64 بت
    uintptr_t key = encrypted_gworld ^ 0x9E3779B97F4A7C15ULL; 
    uintptr_t decrypted = (key >> 20) | (key << 44); 
    
    return decrypted;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_muhgoub_hud_MemoryUtils_getPlayersLocations(JNIEnv *env, jobject thiz, jint pid, jlong base_address_java) {
    if (pid <= 0 || base_address_java <= 0) return nullptr;

    uintptr_t base_address = (uintptr_t)base_address_java;
    char status_buf[256] = {0};

    uintptr_t encrypted_gworld = Read<uintptr_t>(pid, base_address + Offsets::GWorld);
    uintptr_t gworld = decrypt_gworld(encrypted_gworld);
    
    std::vector<Vector3> temp_players;
    int actor_count = 0;

    if (gworld) {
        uintptr_t persistent_level = Read<uintptr_t>(pid, gworld + Offsets::PersistentLevel);
        if (persistent_level) {
            uintptr_t actor_array = Read<uintptr_t>(pid, persistent_level + Offsets::ActorArray);
            actor_count = Read<int>(pid, persistent_level + Offsets::ActorCount);

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
