#include <jni.h>
#include <string.h>
#include "prototypes.h"
#include "ui.h"

//#define JNICALL
/*
 * begin extern
 */
extern "C" {

JNIEXPORT jint JNICALL Java_link_infra_sslsockspro_service_StunnelService_beginStunnel(JNIEnv *env, jobject obj, jstring jstr) {
    jboolean isCopy = true;
    const char * convstr = (env)->GetStringUTFChars(jstr, &isCopy);
    char* cstr = strdup(convstr);
    env->ReleaseStringUTFChars(jstr, convstr);

    static int started = 0;
    if (started) {
        return 10;
    }
    started = 1;
    int argc = 2;
    char* argv[2];
    int retval;
    argv[0] = strdup("mystunnel");
    argv[1] = cstr;
    retval = main_stunnel(argc, argv);
    started = 0;
    free(argv[0]);
    free(cstr);
    return retval;
}

JNIEXPORT void JNICALL Java_link_infra_sslsockspro_service_StunnelService_reloadStunnel(JNIEnv *env, jobject obj) {
    signal_handler(SIGNAL_RELOAD_CONFIG);
//    s_log(LOG_NOTICE, "Starting idle mode configuration");
}

JNIEXPORT void JNICALL Java_link_infra_sslsockspro_service_StunnelService_endStunnel(JNIEnv *env, jobject obj) {
    signal_handler(SIGINT);
}

/*
 * end extern
 */
}
