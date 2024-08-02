package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::minutes") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class Minutes extends Pointer {
    public Minutes() { allocate(); }
    private native void allocate();
    public Minutes(long r) { allocate(r); }
    private native void allocate(long r);
    public Minutes(Hours d) { allocate(d); }
    private native void allocate(@Const @ByRef Hours d);

    public native @Name("operator=") @ByRef Minutes put(@Const @ByRef Minutes other);
    public native @Name("operator-") @ByVal Minutes negate();
    public native @Name("operator++") @ByRef Minutes increment();
    public native @Name("operator--") @ByRef Minutes decrement();
    public native @Name("operator+=") @ByRef Minutes addPut(@Const @ByRef Minutes d);
    public native @Name("operator-=") @ByRef Minutes subtractPut(@Const @ByRef Minutes d);
    public native @Name("operator*=") @ByRef Minutes multiplyPut(@Const @ByRef int rhs);
    public native @Name("operator%=") @ByRef Minutes modPut(@Const @ByRef int rhs);
    public native @Name("operator%=") @ByRef Minutes modPut(@Const @ByRef Minutes rhs);

    public native int count();
    static public native @ByVal @Name("zero") Minutes zero_();
    static public native @ByVal Minutes min();
    static public native @ByVal Minutes max();
}
