package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::milliseconds") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class Milliseconds extends Pointer {
    public Milliseconds() { allocate(); }
    private native void allocate();
    public Milliseconds(long r) { allocate(r); }
    private native void allocate(long r);
    public Milliseconds(Seconds d) { allocate(d); }
    private native void allocate(@Const @ByRef Seconds d);
    public Milliseconds(Minutes d) { allocate(d); }
    private native void allocate(@Const @ByRef Minutes d);
    public Milliseconds(Hours d) { allocate(d); }
    private native void allocate(@Const @ByRef Hours d);

    public native @Name("operator=") @ByRef Milliseconds put(@Const @ByRef Milliseconds other);
    public native @Name("operator-") @ByVal Milliseconds negate();
    public native @Name("operator++") @ByRef Milliseconds increment();
    public native @Name("operator--") @ByRef Milliseconds decrement();
    public native @Name("operator+=") @ByRef Milliseconds addPut(@Const @ByRef Milliseconds d);
    public native @Name("operator-=") @ByRef Milliseconds subtractPut(@Const @ByRef Milliseconds d);
    public native @Name("operator*=") @ByRef Milliseconds multiplyPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef Milliseconds modPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef Milliseconds modPut(@Const @ByRef Milliseconds rhs);

    public native long count();
    static public native @ByVal @Name("zero") Milliseconds zero_();
    static public native @ByVal Milliseconds min();
    static public native @ByVal Milliseconds max();
}
