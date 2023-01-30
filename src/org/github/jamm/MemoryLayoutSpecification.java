package org.github.jamm;

/**
 * Information about the memory layout used by the JVM running the code.
 * This code assume that the JVM is an HotSpot JVM.
 * 
 * <p>The memory layout for normal Java objects start with an object header which consists of mark and class words plus
 *  possible alignment paddings. After the object header, there may be zero or more references to instance fields.</p>
 * <p>For arrays, the header contains a 4-byte array length in addition to mark, class, and paddings.</p> 
 *  
 * <p>Objects are aligned: they always start at some multiple of the alignment.</p>
 *
 */
public interface MemoryLayoutSpecification
{
    /**
     * Returns the size of the array header.
     * <p>The array header is composed of the object header + the array length.
     * Its size in bytes is equal to {@code getObjectHeaderSize()} + 4</p>
     * 
     * @return the size of the array header.
     */
    int getArrayHeaderSize();

    /**
     * Returns the size of the object header (mark word + class word).
     * @return the size of the object header
     */
    int getObjectHeaderSize();

    /**
     * Returns the object alignment (padding) in bytes.
     * <p>The alignment is always a power of 2.</p>
     *
     * @return the object alignment in bytes.
     */
    int getObjectAlignment();

    /**
     * Returns the size of the reference to java objects (also called <i>oops</i> for <i>ordinary object pointers</i>)
     *
     * @return the java object reference size
     */
    int getReferenceSize();

    public static MemoryLayoutSpecification getEffectiveMemoryLayoutSpecification() {

        int objectAlignment = VM.getObjectAlignmentInBytes();

        if (VM.is32Bits()) {
            // Running with 32-bit data model
            return new MemoryLayoutSpecification() {

                @Override
                public int getArrayHeaderSize() {
                    return 12; // object header (8 bytes) + array length (4 bytes)
                }

                @Override
                public int getObjectHeaderSize() {
                    return 8; // mark word (4 bytes) + class word (4 bytes)
                }

                @Override
                public int getObjectAlignment() {
                    return objectAlignment;
                }

                @Override
                public int getReferenceSize() {
                    return 4; // reference size for 32 bit
                }
            };
        }

        if (VM.useCompressedOops()) {

            return new MemoryLayoutSpecification() {

                @Override
                public int getArrayHeaderSize() {
                    return 16; // object header (12 bytes) + array length (4 bytes)
                }

                @Override
                public int getObjectHeaderSize() {
                    return 12; // mark word (8 bytes) + class word (4 bytes)
                }

                @Override
                public int getObjectAlignment() {
                    return objectAlignment;
                }

                @Override
                public int getReferenceSize() {
                    return 4;
                }
            };
        }

        // In other cases, it's a 64-bit uncompressed OOPs object model
        return new MemoryLayoutSpecification() {

            @Override
            public int getArrayHeaderSize() {
                return 20; // object header (16 bytes) + array length (4 bytes)
            }

            @Override
            public int getObjectHeaderSize() {
                return 16; // mark word (8 bytes) + class word (8 bytes)
            }

            @Override
            public int getObjectAlignment() {
                return objectAlignment;
            }

            @Override
            public int getReferenceSize() {
                return 8;
            }
        };
    }
}
