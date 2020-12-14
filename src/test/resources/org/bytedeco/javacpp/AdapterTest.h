#include <memory>
#include <string>
#include <vector>

std::string testStdString(std::string str) {
    return str;
}

std::wstring testStdWString(std::wstring str) {
    return str;
}

std::string testStdString2(std::string str) {
    return str;
}

std::wstring testStdWString2(std::wstring str) {
    return str;
}

std::u16string testStdU16String(std::u16string str) {
    return str;
}

std::u32string testStdU32String(std::u32string str) {
    return str;
}

char *testCharString(const char *str) {
    return strdup(str);
    // memory leak...
}

const unsigned short *testShortString(const unsigned short *str) {
    return str;
}

unsigned short *testShortString(unsigned short *str) {
    return str;
}

int *testIntString(int *str) {
    return str;
}

const std::string& getConstStdString() {
    static std::string test("test");
    return test;
}

const std::string& getConstStdString2() {
    return getConstStdString();
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

std::unique_ptr<UniqueData> createUniqueData() {
    return std::unique_ptr<UniqueData>(new UniqueData(5));
}

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

std::vector<const char*> testStdVectorConstPointer(std::vector<const char*> v) {
    return v;
}

struct MovedData {
    int data;
    MovedData(int data) : data(data) { }
};

MovedData movedData(13);
MovedData&& getMovedData() {
    return std::move(movedData);
}

void putMovedData(MovedData&& m) {
    movedData = m;
}
