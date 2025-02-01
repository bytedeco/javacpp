package org.bytedeco.javacpp.chrono;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.ByVal;
import org.bytedeco.javacpp.annotation.MemberGetter;
import org.bytedeco.javacpp.annotation.Name;
import org.bytedeco.javacpp.annotation.Properties;

@Name("std::chrono::high_resolution_clock") @Properties(inherit = org.bytedeco.javacpp.presets.chrono.class)
public class HighResolutionClock extends Pointer {
    static public native @ByVal HighResolutionTime now();
    static public native @MemberGetter boolean is_steady();
}
