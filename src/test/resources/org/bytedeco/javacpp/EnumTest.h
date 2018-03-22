enum class CharEnum : char {
    CHAR = 42
};

enum class ShortEnum : short {
    SHORT = 654
};

enum /* no class */ IntEnum : int {
    INT = 987
};

enum /* no class */ LongEnum : long long {
    LONG = 121110
};

ShortEnum Char2Short(CharEnum e) {
    return (ShortEnum)e;
}

LongEnum Int2Long(IntEnum e) {
    return (LongEnum)e;
}

LongEnum enumCallback(LongEnum (*f)(CharEnum e)) {
    return f(CharEnum::CHAR);
}
