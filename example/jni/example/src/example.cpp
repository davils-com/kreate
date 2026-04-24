#include <jni.h>

extern "C" {
    JNIEXPORT jstring JNICALL
    Java_com_davils_example_JNI_hello(JNIEnv* env, jobject _) {
        return env->NewStringUTF("Hello from C++");
    }
}
