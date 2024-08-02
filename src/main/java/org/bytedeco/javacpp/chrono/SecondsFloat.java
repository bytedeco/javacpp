package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::duration<float>") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class SecondsFloat extends Pointer {
    public SecondsFloat() { allocate(); }
    private native void allocate();
    public SecondsFloat(float r) { allocate(r); }
    private native void allocate(float r);
    public SecondsFloat(Nanoseconds d) { allocate(d); }
    private native void allocate(@Const @ByRef Nanoseconds d);
    public SecondsFloat(Seconds d) { allocate(d); }
    private native void allocate(@Const @ByRef Seconds d);

    public native @Name("operator=") @ByRef SecondsFloat put(@Const @ByRef SecondsFloat other);
    public native @Name("operator-") @ByVal SecondsFloat negate();
    public native @Name("operator++") @ByRef SecondsFloat increment();
    public native @Name("operator--") @ByRef SecondsFloat decrement();
    public native @Name("operator+=") @ByRef SecondsFloat addPut(@Const @ByRef SecondsFloat d);
    public native @Name("operator-=") @ByRef SecondsFloat subtractPut(@Const @ByRef SecondsFloat d);
    public native @Name("operator*=") @ByRef SecondsFloat multiplyPut(@Const @ByRef float rhs);

    public native float count();
    static public native @ByVal @Name("zero") SecondsFloat zero_();
    static public native @ByVal SecondsFloat min();
    static public native @ByVal SecondsFloat max();
}
