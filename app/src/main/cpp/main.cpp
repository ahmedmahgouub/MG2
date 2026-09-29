#include <jni.h>
#include <string>
#include <vector>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>
#include <cstdio>
#include <cstring>
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

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_muhgoub_hud_MemoryUtils_getPlayersLocations(JNIEnv *env, jobject thiz, jint pid, jlong base_address_java) {
    if (pid <= 0 || base_address_java <= 0) return nullptr;

    uintptr_t base_address = (uintptr_t)base_address_java;
    
    char status_buf[256];
    memset(status_buf, 0, sizeof(status_buf));

    std::vector<Vector3> temp_players;

    // قراءة عنوان الـ GWorld الصافي مباشرة
    uintptr_t gworld = Read<uintptr_t>(pid, base_address + 0xF624D40);

    if (gworld > 0) {
        uintptr_t persistent_level = Read<uintptr_t>(pid, gworld + 0x30); // PersistentLevel
        if (persistent_level) {
            // 🟢 تم التحديث لأحدث أوفستات مصفوفة الكائنات والعداد لنسخة الـ 64 بت الحالية لكسر الـ WAITING
            uintptr_t actor_array = Read<uintptr_t>(pid, persistent_level + 0x98); 
            int actor_count = Read<int>(pid, persistent_level + 0xA0);        

            if (actor_count > 0 && actor_count < 2000) {
                int max_actors = (actor_count > 800) ? 800 : actor_count;

                for (int i = 0; i < max_actors; i++) {
                    uintptr_t actor = Read<uintptr_t>(pid, actor_array + (i * 8));
                    if (!actor) continue;

                    uintptr_t root_component = Read<uintptr_t>(pid, actor + 0x208); // RootComponent
                    if (!root_component) continue;

                    Vector3 location = Read<Vector3>(pid, root_component + 0x1E4); // RelativeLocation
                    
                    if (location.x != 0.0f && location.y != 0.0f) {
                        temp_players.push_back(Vector3{location.x, location.y, location.z});
                    }
                }
            }
        }
    }

    // تحديث شريط الحالة ديناميكياً وبأمان بناءً على البيانات المقروءة حياً
    if (gworld && !temp_players.empty()) {
        snprintf(status_buf, sizeof(status_buf), "PID: %d | Base: 0x%lx | Players: %d", pid, base_address, (int)temp_players.size());
    } else if (gworld) {
        snprintf(status_buf, sizeof(status_buf), "PID: %d | Base: 0x%lx | GWorld Found", pid, base_address);
    } else {
        snprintf(status_buf, sizeof(status_buf), "PID: %d | Base: 0x%lx | WAITING DATA...", pid, base_address);
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
