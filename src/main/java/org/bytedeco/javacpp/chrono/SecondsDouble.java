package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::duration<double>") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class SecondsDouble extends Pointer {
    public SecondsDouble() { allocate(); }
    private native void allocate();
    public SecondsDouble(double r) { allocate(r); }
    private native void allocate(double r);
    public SecondsDouble(Nanoseconds d) { allocate(d); }
    private native void allocate(@Const @ByRef Nanoseconds d);
    public SecondsDouble(Seconds d) { allocate(d); }
    private native void allocate(@Const @ByRef Seconds d);

    public native @Name("operator=") @ByRef SecondsDouble put(@Const @ByRef SecondsDouble other);
    public native @Name("operator-") @ByVal SecondsDouble negate();
    public native @Name("operator++") @ByRef SecondsDouble increment();
    public native @Name("operator--") @ByRef SecondsDouble decrement();
    public native @Name("operator+=") @ByRef SecondsDouble addPut(@Const @ByRef SecondsDouble d);
    public native @Name("operator-=") @ByRef SecondsDouble subtractPut(@Const @ByRef SecondsDouble d);
    public native @Name("operator*=") @ByRef SecondsDouble multiplyPut(@Const @ByRef double rhs);

    public native double count();
    static public native @ByVal @Name("zero") SecondsDouble zero_();
    static public native @ByVal SecondsDouble min();
    static public native @ByVal SecondsDouble max();
}
