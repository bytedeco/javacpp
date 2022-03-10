#include <thread>

class Callback {
public:
    virtual void callback(int value) = 0;
};

static void doIt(Callback* callback, int value) {
    for(int i = 1; i <= value; i++) {
        callback->callback(i);
    }
}

static void run(Callback* callback, int count) {
    std::thread thread(doIt, callback, count);

    thread.join();
}
