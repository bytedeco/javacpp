module javacpp.core {
  requires static org.slf4j; // 'static' means that downstream libraries don't need the SLF4J dependency
  requires jdk.unsupported; // For UnsafeRaw

  exports org.bytedeco.javacpp;
  exports org.bytedeco.javacpp.annotation;
  exports org.bytedeco.javacpp.indexer;
  exports org.bytedeco.javacpp.tools;
}
