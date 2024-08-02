package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::time_point<std::chrono::steady_clock>") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class SteadyTime extends Pointer {
    public SteadyTime() { allocate(); }
    private native void allocate();

    public SteadyTime(SteadyDuration d) { allocate(d); }
    private native void allocate(@Const @ByRef SteadyDuration d);

    public native @ByVal SteadyDuration time_since_epoch();

    public native @Name("operator +=") @ByRef SteadyTime addPut(@Const @ByRef SteadyDuration d);
    public native @Name("operator -=") @ByRef SteadyTime subtractPut(@Const @ByRef SteadyDuration d);
    static public native @ByVal SteadyTime min();
    static public native @ByVal SteadyTime max();

}
