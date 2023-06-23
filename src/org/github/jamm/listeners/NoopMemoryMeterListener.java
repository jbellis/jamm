package org.github.jamm.listeners;

import java.nio.ByteBuffer;

import org.github.jamm.MemoryMeterListener;

/**
 * Listener that does nothing.
 */
public final class NoopMemoryMeterListener implements MemoryMeterListener {

    /**
     * Singleton instance.
     */
    private static final MemoryMeterListener INSTANCE = new NoopMemoryMeterListener();

    public static final Factory FACTORY = new Factory() {

        @Override
        public MemoryMeterListener newInstance() {
            return INSTANCE;
        }
    };

    @Override
    public void objectMeasured(Object current, long size) {
    }

    @Override
    public void byteBufferRemainingMeasured(ByteBuffer buffer, long size) {
    }

    @Override
    public void fieldAdded(Object obj, String fieldName, Object fieldValue) {
    }

    @Override
    public void arrayElementAdded(Object[] array, int index, Object elementValue) {
    }

    @Override
    public void done(long size) {
    }

    @Override
    public void failedToAccessField(Object obj, String fieldName, Class<?> fieldType) {
    }

    @Override
    public void started(Object obj) {
    }

    private NoopMemoryMeterListener() {
    }
}