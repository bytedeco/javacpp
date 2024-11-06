static JavaCPP_noinline jclass JavaCPP_mapJavaExceptions(JNIEnv *env, const std::exception& e){
        if (dynamic_cast<const std::invalid_argument*>(&e)) {
            return env->FindClass("java/lang/IllegalArgumentException");
        }
        else if (dynamic_cast<const std::out_of_range*>(&e)) {
            return env->FindClass("java/lang/IndexOutOfBoundsException");
        }
        else if (dynamic_cast<const std::runtime_error*>(&e)) {
            return env->FindClass("java/lang/RuntimeException");
        }
        return env->FindClass("java/lang/Exception");
}

static JavaCPP_noinline jthrowable JavaCPP_createJavaException(JNIEnv *env, const std::exception& e, jthrowable cause = nullptr) {
    jclass exClass = JavaCPP_mapJavaExceptions(env, e);
    jstring message = env->NewStringUTF(e.what());
    jmethodID constructor = env->GetMethodID(exClass, "<init>", "(Ljava/lang/String;)V");

    if(cause) {
        jmethodID initCause = env->GetMethodID(exClass, "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;");
        jobject exWithCause = static_cast<jthrowable>(env->NewObject(exClass, constructor, message));
        env->CallVoidMethod(exWithCause, initCause, cause);
        return static_cast<jthrowable>(exWithCause);
    }
    return  static_cast<jthrowable>(env->NewObject(exClass, constructor, message));
}

static JavaCPP_noinline jthrowable JavaCPP_handleException(JNIEnv *env, const std::exception& e) {
        try {
            std::rethrow_if_nested(e);
        } catch (std::exception &nested) {
            jthrowable cause = static_cast<jthrowable>(JavaCPP_handleException(env, nested));
            return JavaCPP_createJavaException(env, e, cause);
        }
        return JavaCPP_createJavaException(env, e);
}
