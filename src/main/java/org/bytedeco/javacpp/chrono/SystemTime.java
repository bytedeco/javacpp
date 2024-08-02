package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::time_point<std::chrono::system_clock>") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class SystemTime extends Pointer {
    public SystemTime() { allocate(); }
    private native void allocate();

    public SystemTime(SystemDuration d) { allocate(d); }
    private native void allocate(@Const @ByRef SystemDuration d);

    public native @ByVal SystemDuration time_since_epoch();

    public native @Name("operator +=") @ByRef SystemTime addPut(@Const @ByRef SystemDuration d);
    public native @Name("operator -=") @ByRef SystemTime subtractPut(@Const @ByRef SystemDuration d);
    static public native @ByVal SystemTime min();
    static public native @ByVal SystemTime max();
}
