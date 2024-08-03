package org.bytedeco.javacpp.presets;

import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Properties;
import org.bytedeco.javacpp.tools.Info;
import org.bytedeco.javacpp.tools.InfoMap;
import org.bytedeco.javacpp.tools.InfoMapper;

@Properties(
    inherit = javacpp.class,
    target = "org.bytedeco.javacpp.chrono",
    global = "org.bytedeco.javacpp.global.chrono"
)
public class chrono implements InfoMapper {
    @Override public void map(InfoMap infoMap) {
        infoMap
            .put(new Info("std::chrono::high_resolution_clock").pointerTypes("HighResolutionClock"))
            .put(new Info("std::chrono::steady_clock").pointerTypes("SteadyClock"))
            .put(new Info("std::chrono::system_clock").pointerTypes("SystemClock"))

            .put(new Info("std::chrono::time_point<std::chrono::high_resolution_clock>").pointerTypes("HighResolutionTime"))
            .put(new Info("std::chrono::time_point<std::chrono::steady_clock>").pointerTypes("SteadyTime"))
            .put(new Info("std::chrono::time_point<std::chrono::system_clock>").pointerTypes("SystemTime"))

            .put(new Info("std::chrono::high_resolution_clock::duration").pointerTypes("HighResolutionDuration"))
            .put(new Info("std::chrono::steady_clock::duration").pointerTypes("SteadyDuration"))
            .put(new Info("std::chrono::system_clock::duration").pointerTypes("SystemDuration"))

            .put(new Info("std::chrono::hours").pointerTypes("Hours"))
            .put(new Info("std::chrono::minutes").pointerTypes("Minutes"))
            .put(new Info("std::chrono::seconds", "std::chrono::duration<long>", "std::chrono::duration<long,std::ratio<1> >", "std::chrono::duration<long,std::ratio<1,1> >").pointerTypes("Seconds"))
            .put(new Info("std::chrono::milliseconds", "std::chrono::duration<long,std::milli>", "std::chrono::duration<long,std::ratio<1,1000> >").pointerTypes("Milliseconds"))
            .put(new Info("std::chrono::microseconds", "std::chrono::duration<long,std::micro>", "std::chrono::duration<long,std::ratio<1,1000000> >").pointerTypes("Microseconds"))
            .put(new Info("std::chrono::nanoseconds", "std::chrono::duration<long,std::nano>", "std::chrono::duration<long,std::ratio<1,1000000000> >").pointerTypes("Nanoseconds"))

            .put(new Info("std::chrono::duration<float>", "std::chrono::duration<float,std::ratio<1> >", "std::chrono::duration<float,std::ratio<1,1> >").pointerTypes("SecondsFloat"))
            .put(new Info("std::chrono::duration<double>", "std::chrono::duration<double,std::ratio<1> >", "std::chrono::duration<double,std::ratio<1,1> >").pointerTypes("SecondsDouble"))
        ;
    }
}
