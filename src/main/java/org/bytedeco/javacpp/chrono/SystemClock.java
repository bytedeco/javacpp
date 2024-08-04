package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::system_clock") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class SystemClock extends Pointer {
    static public native @ByVal SystemTime now();
    static public native @Cast("time_t") long to_time_t(@Const @ByRef SystemTime t);
    static public native @ByVal SystemTime from_time_t(@Cast("time_t") long t);
}
