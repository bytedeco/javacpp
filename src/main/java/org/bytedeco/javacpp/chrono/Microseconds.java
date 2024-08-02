package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::microseconds") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class Microseconds extends Pointer {
    public Microseconds() { allocate(); }
    private native void allocate();
    public Microseconds(long r) { allocate(r); }
    private native void allocate(long r);

    public Microseconds(Milliseconds d) { allocate(d); }
    private native void allocate(@Const @ByRef Milliseconds d);
    public Microseconds(Seconds d) { allocate(d); }
    private native void allocate(@Const @ByRef Seconds d);
    public Microseconds(Minutes d) { allocate(d); }
    private native void allocate(@Const @ByRef Minutes d);
    public Microseconds(Hours d) { allocate(d); }
    private native void allocate(@Const @ByRef Hours d);

    public native @Name("operator=") @ByRef Microseconds put(@Const @ByRef Microseconds other);
    public native @Name("operator-") @ByVal Microseconds negate();
    public native @Name("operator++") @ByRef Microseconds increment();
    public native @Name("operator--") @ByRef Microseconds decrement();
    public native @Name("operator+=") @ByRef Microseconds addPut(@Const @ByRef Microseconds d);
    public native @Name("operator-=") @ByRef Microseconds subtractPut(@Const @ByRef Microseconds d);
    public native @Name("operator*=") @ByRef Microseconds multiplyPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef Microseconds modPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef Microseconds modPut(@Const @ByRef Microseconds rhs);

    public native long count();
    static public native @ByVal @Name("zero") Microseconds zero_();
    static public native @ByVal Microseconds min();
    static public native @ByVal Microseconds max();
}
