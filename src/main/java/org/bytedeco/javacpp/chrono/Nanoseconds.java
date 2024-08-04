package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::nanoseconds") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class Nanoseconds extends Pointer {
    public Nanoseconds() { allocate(); }
    private native void allocate();
    public Nanoseconds(long r) { allocate(r); }
    private native void allocate(long r);
    public Nanoseconds(Microseconds d) { allocate(d); }
    private native void allocate(@Const @ByRef Microseconds d);
    public Nanoseconds(Milliseconds d) { allocate(d); }
    private native void allocate(@Const @ByRef Milliseconds d);
    public Nanoseconds(Seconds d) { allocate(d); }
    private native void allocate(@Const @ByRef Seconds d);
    public Nanoseconds(Minutes d) { allocate(d); }
    private native void allocate(@Const @ByRef Minutes d);
    public Nanoseconds(Hours d) { allocate(d); }
    private native void allocate(@Const @ByRef Hours d);
    public Nanoseconds(SystemDuration d) {  super((Pointer)null); allocate(d); };
    private native void allocate(@Const @ByRef SystemDuration d);
    public Nanoseconds(HighResolutionDuration d) {  super((Pointer)null); allocate(d); };
    private native void allocate(@Const @ByRef HighResolutionDuration d);
    public Nanoseconds(SteadyDuration d) {  super((Pointer)null); allocate(d); };
    private native void allocate(@Const @ByRef SteadyDuration d);

    public native @Name("operator=") @ByRef Nanoseconds put(@Const @ByRef Nanoseconds other);
    public native @Name("operator-") @ByVal Nanoseconds negate();
    public native @Name("operator++") @ByRef Nanoseconds increment();
    public native @Name("operator--") @ByRef Nanoseconds decrement();
    public native @Name("operator+=") @ByRef Nanoseconds addPut(@Const @ByRef Nanoseconds d);
    public native @Name("operator-=") @ByRef Nanoseconds subtractPut(@Const @ByRef Nanoseconds d);
    public native @Name("operator*=") @ByRef Nanoseconds multiplyPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef Nanoseconds modPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef Nanoseconds modPut(@Const @ByRef Nanoseconds rhs);

    public native long count();
    static public native @ByVal @Name("zero") Nanoseconds zero_();
    static public native @ByVal Nanoseconds min();
    static public native @ByVal Nanoseconds max();
}
