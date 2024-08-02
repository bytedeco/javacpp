package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::hours") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class Hours extends Pointer {
    public Hours() { allocate(); }
    private native void allocate();
    public Hours(long r) { allocate(r); }
    private native void allocate(long r);

    public native @Name("operator=") @ByRef Hours put(@Const @ByRef Hours other);
    public native @Name("operator-") @ByVal Hours negate();
    public native @Name("operator++") @ByRef Hours increment();
    public native @Name("operator--") @ByRef Hours decrement();
    public native @Name("operator+=") @ByRef Hours addPut(@Const @ByRef Hours d);
    public native @Name("operator-=") @ByRef Hours subtractPut(@Const @ByRef Hours d);
    public native @Name("operator*=") @ByRef Hours multiplyPut(@Const @ByRef int rhs);
    public native @Name("operator%=") @ByRef Hours modPut(@Const @ByRef int rhs);
    public native @Name("operator%=") @ByRef Hours modPut(@Const @ByRef Hours rhs);

    public native int count();
    static public native @ByVal @Name("zero") Hours zero_();
    static public native @ByVal Hours min();
    static public native @ByVal Hours max();
}
