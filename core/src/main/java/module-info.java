module javacpp.core {
  requires org.slf4j;
  requires jdk.unsupported; // For UnsafeRaw

  exports org.bytedeco.javacpp;
  exports org.bytedeco.javacpp.annotation;
  exports org.bytedeco.javacpp.indexer;
  exports org.bytedeco.javacpp.tools;
}
