signed char *bufferCallback(signed char *(*f)(signed char *buffer)) {
    static signed char value[] = {12};
    return f(value);
}

signed char getByte(signed char *buffer) {
    return buffer[0];
}

void putByte(signed char *buffer, signed char value) {
    buffer[0] = value;
}

short getShort(short *buffer) {
    return buffer[0];
}

void putShort(short *buffer, short value) {
    buffer[0] = value;
}

int getInt(int *buffer) {
    return buffer[0];
}

void putInt(int *buffer, int value) {
    buffer[0] = value;
}

long long getLong(long long *buffer) {
    return buffer[0];
}

void putLong(long long *buffer, long long value) {
    buffer[0] = value;
}

float getFloat(float *buffer) {
    return buffer[0];
}

void putFloat(float *buffer, float value) {
    buffer[0] = value;
}

double getDouble(double *buffer) {
    return buffer[0];
}

void putDouble(double *buffer, double value) {
    buffer[0] = value;
}

