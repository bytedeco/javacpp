package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::time_point<std::chrono::high_resolution_clock>") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class HighResolutionTime extends Pointer {
    public HighResolutionTime() { allocate(); }
    private native void allocate();

    public HighResolutionTime(HighResolutionDuration d) { allocate(d); }
    private native void allocate(@Const @ByRef HighResolutionDuration d);

    public native @ByVal HighResolutionTime time_since_epoch();

    public native @Name("operator +=") @ByRef HighResolutionTime addPut(@Const @ByRef HighResolutionDuration d);
    public native @Name("operator -=") @ByRef HighResolutionTime subtractPut(@Const @ByRef HighResolutionDuration d);
    static public native @ByVal HighResolutionTime min();
    static public native @ByVal HighResolutionTime max();
}
