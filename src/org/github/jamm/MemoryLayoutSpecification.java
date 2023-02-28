package org.github.jamm;

/**
 * Information about the memory layout used by the JVM running the code.
 * This code assume that the JVM is an HotSpot JVM.
 * 
 * <p>The memory layout for normal Java objects start with an object header which consists of mark and class words plus
 *  possible alignment paddings. After the object header, there may be zero or more references to instance fields.</p>
 *
 * <p>For arrays, the header contains a 4-byte array length in addition to the mark and class word. Array headers
 *  might also contain some padding as array base is aligned (https://shipilev.net/jvm/objects-inside-out/#_observation_array_base_is_aligned, 
 *  https://bugs.openjdk.org/browse/JDK-8139457).</p> 
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

        final int objectHeaderSize;
        final int referenceSize;
        final int heapWordSize;

        if (VM.is32Bits()) {

            // Running with 32-bit data model
            objectHeaderSize = 8; // mark word (4 bytes) + class word (4 bytes)
            referenceSize = 4; // reference size for 32 bit
            heapWordSize = 4;

        } else {

            heapWordSize = 8;
            if (VM.useCompressedOops()) {

                objectHeaderSize = 12; // mark word (8 bytes) + class word (4 bytes)
                referenceSize = 4; // compressed reference

            } else {

                // In other cases, it's a 64-bit uncompressed OOPs object model
                // Prior to Java 15, the use of compressed class pointers assumed the use of compressed oops.
                // This was changed in Java 15 by JDK-8241825 (https://bugs.openjdk.org/browse/JDK-8241825).
                objectHeaderSize = VM.useCompressedClassPointers() ? 12  // mark word (8 bytes) + class word (4 bytes)
                                                                   : 16; // mark word (8 bytes) + class word (8 bytes)
                referenceSize = 8; // uncompressed reference
            }
        }

        final int objectAlignment = VM.getObjectAlignmentInBytes();
        final int arrayLength = 4; // space in bytes used to store the array length after the mark and class word
        final int arrayHeaderSize = MathUtils.roundTo(objectHeaderSize + arrayLength, heapWordSize);

        return new MemoryLayoutSpecification() {

            @Override
            public int getArrayHeaderSize() {
                return arrayHeaderSize;
            }

            @Override
            public int getObjectHeaderSize() {
                return objectHeaderSize;
            }

            @Override
            public int getObjectAlignment() {
                return objectAlignment;
            }

            @Override
            public int getReferenceSize() {
                return referenceSize;
            }

            @Override
            public String toString()
            {
                return new StringBuilder().append("Memory Layout: [objectHeaderSize=")
                                          .append(objectHeaderSize)
                                          .append(" , arrayHeaderSize=")
                                          .append(arrayHeaderSize)
                                          .append(", objectAlignement=")
                                          .append(objectAlignment)
                                          .append(", referenceSize=")
                                          .append(referenceSize)
                                          .append(']')
                                          .toString();
            }
        };
    }
}
