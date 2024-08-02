package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::high_resolution_clock::duration") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class HighResolutionDuration extends Pointer {
    public HighResolutionDuration() { allocate(); }
    private native void allocate();
    public HighResolutionDuration(long r) { allocate(r); }
    private native void allocate(long r);

    public native @Name("operator=") @ByRef HighResolutionDuration put(@Const @ByRef HighResolutionDuration other);
    public native @Name("operator-") @ByVal HighResolutionDuration negate();
    public native @Name("operator++") @ByRef HighResolutionDuration increment();
    public native @Name("operator--") @ByRef HighResolutionDuration decrement();
    public native @Name("operator+=") @ByRef HighResolutionDuration addPut(@Const @ByRef HighResolutionDuration d);
    public native @Name("operator-=") @ByRef HighResolutionDuration subtractPut(@Const @ByRef HighResolutionDuration d);
    public native @Name("operator*=") @ByRef HighResolutionDuration multiplyPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef HighResolutionDuration modPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef HighResolutionDuration modPut(@Const @ByRef HighResolutionDuration rhs);

    public native long count();
    static public native @ByVal @Name("zero") HighResolutionDuration zero_();
    static public native @ByVal HighResolutionDuration min();
    static public native @ByVal HighResolutionDuration max();
}
