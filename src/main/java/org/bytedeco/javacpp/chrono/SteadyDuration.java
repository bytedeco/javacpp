package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.*;

@Name("std::chrono::steady_clock::duration") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class SteadyDuration extends Pointer {
    public SteadyDuration() { allocate(); }
    private native void allocate();
    public SteadyDuration(long r) { allocate(r); }
    private native void allocate(long r);

    public native @Name("operator=") @ByRef SteadyDuration put(@Const @ByRef SteadyDuration other);
    public native @Name("operator-") @ByVal SteadyDuration negate();
    public native @Name("operator++") @ByRef SteadyDuration increment();
    public native @Name("operator--") @ByRef SteadyDuration decrement();
    public native @Name("operator+=") @ByRef SteadyDuration addPut(@Const @ByRef SteadyDuration d);
    public native @Name("operator-=") @ByRef SteadyDuration subtractPut(@Const @ByRef SteadyDuration d);
    public native @Name("operator*=") @ByRef SteadyDuration multiplyPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef SteadyDuration modPut(@Const @ByRef long rhs);
    public native @Name("operator%=") @ByRef SteadyDuration modPut(@Const @ByRef SteadyDuration rhs);

    public native long count();
    static public native @ByVal @Name("zero") SteadyDuration zero_();
    static public native @ByVal SteadyDuration min();
    static public native @ByVal SteadyDuration max();}
