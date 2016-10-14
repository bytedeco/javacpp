package my.presets;

import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Properties;
import org.bytedeco.javacpp.caffe;
import org.bytedeco.javacpp.tools.InfoMap;
import org.bytedeco.javacpp.tools.InfoMapper;

@Properties(
    inherit = caffe.class,
    target = "my.test",
    value = {
        @Platform(
            includepath = {"include"},
            include= {"test.hpp"}
        )
    }
)
public abstract class test implements InfoMapper {
    public void map(InfoMap infoMap) {
    }
}
