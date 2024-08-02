package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::system_clock::duration") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class SystemDuration extends Pointer {
    public SystemDuration() { allocate(); }
    private native void allocate();
    public SystemDuration(long r) { allocate(r); }
    private native void allocate(long r);

    public native @Name("operator=") @ByRef SystemDuration put(@Const @ByRef SystemDuration other);
    public native @Name("operator-") @ByVal SystemDuration negate();
    public native @Name("operator++") @ByRef SystemDuration increment();
    public native @Name("operator--") @ByRef SystemDuration decrement();
    public native @Name("operator+=") @ByRef SystemDuration addPut(@Const @ByRef SystemDuration d);
    public native @Name("operator-=") @ByRef SystemDuration subtractPut(@Const @ByRef SystemDuration d);
    public native @Name("operator*=") @ByRef SystemDuration multiplyPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef SystemDuration modPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef SystemDuration modPut(@Const @ByRef SystemDuration rhs);

    public native long count();
    static public native @ByVal @Name("zero") SystemDuration zero_();
    static public native @ByVal SystemDuration min();
    static public native @ByVal SystemDuration max();
}
