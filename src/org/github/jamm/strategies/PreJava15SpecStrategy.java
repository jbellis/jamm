package org.github.jamm.strategies;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.github.jamm.MemoryLayoutSpecification;

import static org.github.jamm.MathUtils.roundTo;

/**
 * {@code MemoryMeterStrategy} that computes the size of the memory occupied by an object, in a pre-Java 15 JVM, based on 
 * the JVM information.
 * 
 * <p> The layout of a Java object in memory is composed of a header, composed of a mark word and a class word,
 *  followed by the fields information. Each object is also aligned based on the value of the object alignment 
 *  (-XX:ObjectAlignmentInBytes which by default is 8 bytes). 
 * <pre>
 *  +--------+---------+---------+-----+---------+-------------------------+
 *  | header | Field 1 | Field 2 | ... | Field n | gap to ensue alignment  |
 *  +--------+---------+---------+-----+---------+-------------------------+ 
 * </pre>
 * </p>
 * <p>
 * The header size depends on the JVM bitness and for 64-bit JVM on the use of compressed references for Java pointers.
 * On a 64 bit JVM with compressed references enabled (default behavior for heaps lower than 32 GB) object headers occupy 12 bytes.
 * This size leave a gap of 4 bytes before the next alignment that the JVM can use to store some fields. For example, the following class:
 * <pre>
 * class A
 * {
 *      int i;
 *      long l;
 * }
 * </pre>
 * will be stored as:
 * <pre>
 *  +------------------+---------------+---------------+
 *  | header (12 bytes)| int (4 bytes) | long (8 bytes)| Total size: 24 bytes
 *  +------------------+---------------+---------------+ 
 * </pre>
 * As the Java Memory Model guarantees the absence of <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.6">word tearing</a> for fields,
 * field alignment can cause some gaps. For example, the following class:
 * <pre>
 * class LongHolder
 * {
 *      long l;
 * }
 * </pre>
 * will be stored as:
 * <pre>
 *  +------------------+---------------+---------------+
 *  | header (12 bytes)| 4 bytes gap   | long (8 bytes)| Total size: 24 bytes
 *  +------------------+---------------+---------------+ 
 * </pre>
 * </p>
 * <p>
 * Before Java 15, super class fields were always taken care first and sub-class fields could not take the gaps left by 
 * the super class. For example with the following classes:
 * <pre>
 * class Parent
 * {
 *      long l;
 * }
 *
 * class Child extends Parent
 * {
 *      int l;
 * }
 * </pre>
 *  the Child class will be stored as:
 * <pre>
 *  +------------------+---------------+---------------+--------------+---------------+
 *  | header (12 bytes)| 4 bytes gap   | long (8 bytes)| int (4 bytes)| alignment gap | Total size: 32 bytes
 *  +------------------+---------------+---------------+--------------+---------------+ 
 * </pre>
 * where the gap left after the header could not be taken by the int field.
 * <pre>Prior to Java 15, super class fields were also aligned using the object reference size which could cause some hierarchy gaps.
 * For example with the following definitions:
 * <pre>
 * class A
 * {
 *      boolean b1;
 * }
 *
 * class B extends A
 * {
 *      boolean b2;
 * }
 * class C extends B
 * {
 *      boolean b3;
 * }
 * </pre>
 * The class C would be represented in memory as:
 * <pre>
 *  +------------------+--------------+-------------+------------+-------------+-------------+---------------+
 *  | header (12 bytes)|  b1 (1 byte) | 3 bytes gap | b2 (1 byte)| 3 bytes gap | b3 (1 byte) | alignment gap | Total size: 24 bytes
 *  +------------------+--------------+-------------+------------+-------------+-------------+---------------+ 
 * </pre>
 * </p>
 * <p> For more inside on the Java Object Layout, I highly recommend the excellent <a href="https://shipilev.net/jvm/objects-inside-out/">Java Objects Inside Out</a> blog post from Aleksey Shipilëv.
 */
final class PreJava15SpecStrategy extends MemoryLayoutBasedStrategy
{
    public PreJava15SpecStrategy(MemoryLayoutSpecification memoryLayout)
    {
        super(memoryLayout);
    }

    /**
     * Align the size of the fields.
     * <p>Prior to JDK 15 the field blocks for each super class were aligned based using the object reference size.
     * This method provides a hook for that logic.</p>
     *
     * @param sizeOfDeclaredFields the size of the fields for a class of the hierarchy
     * @return the size of the class fields aligned.
     */
    protected long alignFieldBlock(long sizeOfDeclaredFields)
    {
        return roundTo(sizeOfDeclaredFields, memoryLayout.getReferenceSize());
    }

    @Override
    protected int arrayBaseOffset(Class<?> type)
    {
        return memoryLayout.getArrayHeaderSize();
    }

    @Override
    public final long measureInstance(Class<?> type) {

        long size = sizeOfFields(type, false);
        return roundTo(size, memoryLayout.getObjectAlignment());
    }

    private long sizeOfFields(Class<?> type, boolean useFieldBlockAlignement) {

        if (type == Object.class)
            return memoryLayout.getObjectHeaderSize();

        long size = sizeOfFields(type.getSuperclass(), true);

        long sizeOfDeclaredField = 0;
        long sizeTakenBy8BytesFields = 0;
        for (Field f : type.getDeclaredFields())
        {
            if (!Modifier.isStatic(f.getModifiers())) {
                int fieldSize = measureField(f.getType());

                sizeTakenBy8BytesFields += fieldSize & 8; // count only the 8 bytes fields
                sizeOfDeclaredField += fieldSize;
            }
        }

        // Ensure that we take into account the superclass gaps
        if (hasGapSmallerThan8Bytes(size) && hasOnly8BytesFields(sizeOfDeclaredField, sizeTakenBy8BytesFields))
        {
            size = roundTo(size, 8);
        }

        // Ensure that we take into account the hierarchy gaps
        size += useFieldBlockAlignement ? alignFieldBlock(sizeOfDeclaredField) : sizeOfDeclaredField;
        return size;
    }

    /**
     * Checks if the fields for this field block are all 8 bytes fields.
     *
     * @param sizeOfDeclaredField the total size of the fields within this block
     * @param sizeTakenBy8BytesFields the total size taken by the 8 bytes fields within this block
     * * @return {@code true} if the fields for this block are all 8 bytes fields, {@code false} otherwise.
     */
    private boolean hasOnly8BytesFields(long sizeOfDeclaredField, long sizeTakenBy8BytesFields)
    {
        return sizeOfDeclaredField != 0 && sizeOfDeclaredField == sizeTakenBy8BytesFields;
    }

    /**
     * Checks if the previous block has a gap at the end smaller than 8 bytes into which the JVM can store some field
     *  to improve packing.
     *  <p>We know that if there is a gap this gap will always be of 4 bytes as field blocks are always aligned by the 
     *  Object reference size which can only be 4 (compressed reference) or 8.</p>
     *
     * @param size the size of the memory used so far.
     * @return {@code true} if there is some space available (4 bytes), {@code false} otherwise.
     */
    private boolean hasGapSmallerThan8Bytes(long size)
    {
        return (size & 7) > 0;
    }
}
