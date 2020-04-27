package org.bytedeco.javacpp.indexer;

public class OneIndex extends Index {

    private final long[] sizes;

    /**
     * TODO
     *
     * @param size
     */
    protected OneIndex(long size) {
        this.sizes = new long[]{size};
    }

    @Override
    public long index(long i) {
        return i;
    }

    @Override
    public long index(long i, long j) {
        return i;
    }

    @Override
    public long index(long i, long j, long k) {
        return i;
    }

    @Override
    public long index(long... indices) {
        return indices[0];
    }

    @Override
    public long[] sizes() {
        return sizes;
    }
}
