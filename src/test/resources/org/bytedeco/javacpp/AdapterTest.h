#include <memory>
#include <string>
#include <vector>

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

int constructorCount = 0;
int destructorCount = 0;

struct SharedData {
    int data;
    SharedData(int data) : data(data) {
        constructorCount++;
    }
    ~SharedData() {
        destructorCount++;
    }
};

std::shared_ptr<SharedData> createSharedData() {
    return std::shared_ptr<SharedData>(new SharedData(42));
}

std::shared_ptr<SharedData> sharedData;
void storeSharedData(std::shared_ptr<SharedData> s) {
    sharedData = s;
    sharedData->data = 13;
}

std::shared_ptr<SharedData> fetchSharedData() {
    std::shared_ptr<SharedData> s = sharedData;
    sharedData.reset();
    return s;
}

struct UniqueData {
    int data;
    UniqueData(int data) : data(data) { }
};

void createUniqueData(std::unique_ptr<UniqueData> *u) {
    u->reset(new UniqueData(42));
}

std::unique_ptr<UniqueData> uniqueData(new UniqueData(13));
void storeUniqueData(const std::unique_ptr<UniqueData>* u) {
    uniqueData->data = (*u)->data;
}

const std::unique_ptr<UniqueData>* fetchUniqueData() {
    return &uniqueData;
}

std::vector<int> testStdVectorByVal(std::vector<int> v) {
    return v;
}

const std::vector<int>& testStdVectorByRef(std::vector<int>& v) {
    return v;
}

std::vector<int>* testStdVectorByPtr(std::vector<int>* v) {
    return v;
}
