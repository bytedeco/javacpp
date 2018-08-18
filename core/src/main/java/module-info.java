/**
 * The core JavaCPP module. This provides the basic data types for native interop ({@link org.bytedeco.javacpp.Pointer},
 * {@link org.bytedeco.javacpp.indexer.Indexer} and friends) and annotations for generating
 */
module javacpp.core {
  requires org.slf4j;
  requires jdk.unsupported; // For UnsafeRaw

  exports org.bytedeco.javacpp;
  exports org.bytedeco.javacpp.annotation;
  exports org.bytedeco.javacpp.indexer;
  exports org.bytedeco.javacpp.tools;
}
