package org.bytedeco.javacpp.presets;

import org.bytedeco.javacpp.annotation.Properties;

@Properties(
    inherit = javacpp.class,
    target = "org.bytedeco.javacpp.chrono",
    global = "org.bytedeco.javacpp.chrono.Chrono"
)
public class chrono {
}
