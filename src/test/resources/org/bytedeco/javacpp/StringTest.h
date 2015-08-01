#include <string>

std::string testStdString(std::string str) {
    return str;
}

char *testCharString(const char *str) {
    return strdup(str);
    // memory leak...
}

unsigned short *testShortString(unsigned short *str) {
    return str;
}

int *testIntString(int *str) {
    return str;
}
