jclass findNVDClass(JNIEnv *env, const char *className) {
    if (gClassLoader == nullptr) { return nullptr; }

    // Ensure the gClassLoader is valid
    jclass classLoaderClass = env->GetObjectClass(gClassLoader);
    if (classLoaderClass == nullptr) { return nullptr; }

    // Find the loadClass method
    jmethodID loadClassMethod = env->GetMethodID(classLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    if (loadClassMethod == nullptr) { return nullptr; }

    // Convert the class name to a Java string
    jstring javaClassName = env->NewStringUTF(className);
    if (javaClassName == nullptr) { return nullptr; }

    // Use the ClassLoader to load the class
    jclass loadedClass = static_cast<jclass>(env->CallObjectMethod(gClassLoader, loadClassMethod, javaClassName));

    // Clean up the local reference for the Java string
    env->DeleteLocalRef(javaClassName);

    // Check for exceptions or null result
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe(); // Optional: Print the exception
        env->ExceptionClear();
    }

    return loadedClass;
}


void ensureContextClassLoader(JNIEnv *env) {
    if (gClassLoader == nullptr) { throw std::runtime_error("Class loader not set. Call setClassLoader first."); }

    // Get the current thread
    jclass threadClass = env->FindClass("java/lang/Thread");
    if (!threadClass) { throw std::runtime_error("Failed to find Thread class."); }

    jmethodID currentThreadMethod = env->GetStaticMethodID(threadClass, "currentThread", "()Ljava/lang/Thread;");
    if (!currentThreadMethod) { throw std::runtime_error("Failed to find currentThread method."); }

    jobject currentThread = env->CallStaticObjectMethod(threadClass, currentThreadMethod);
    if (!currentThread) { throw std::runtime_error("Failed to get current thread."); }

    // Get the getContextClassLoader method
    jmethodID getContextClassLoaderMethod = env->GetMethodID(threadClass, "getContextClassLoader", "()Ljava/lang/ClassLoader;");
    if (!getContextClassLoaderMethod) { throw std::runtime_error("Failed to find getContextClassLoader method."); }

    jobject currentClassLoader = env->CallObjectMethod(currentThread, getContextClassLoaderMethod);

    // Check if the context class loader is null
    if (currentClassLoader == nullptr) {
        // Get the setContextClassLoader method
        jmethodID setContextClassLoaderMethod = env->GetMethodID(threadClass, "setContextClassLoader", "(Ljava/lang/ClassLoader;)V");
        if (!setContextClassLoaderMethod) { throw std::runtime_error("Failed to find setContextClassLoader method."); }

        // Set the stored global class loader
        env->CallVoidMethod(currentThread, setContextClassLoaderMethod, gClassLoader);
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe(); // Optional: Print the exception
            env->ExceptionClear();
            throw std::runtime_error("Failed to set context class loader.");
        }
    }
}

void setClassLoader(jobject classLoader) {
    JNIEnv* env = nullptr;
    JavaVM* jvm = nullptr;

    // Attach the current thread to the JVM if needed
    jint res = JavaCPP_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_2);
    if (res != JNI_OK) {
        throw std::runtime_error("Failed to get JNIEnv.");
    }

    std::lock_guard<std::mutex> lock(gClassLoaderMutex);

    if (gClassLoader) {
        env->DeleteGlobalRef(gClassLoader);
        gClassLoader = nullptr;
    }

    gClassLoader = env->NewGlobalRef(classLoader);
}