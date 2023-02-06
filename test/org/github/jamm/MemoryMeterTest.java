package org.github.jamm;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.function.Predicate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * In order to test the correctness of the values being return the tests assume that the correct value is the one 
 * returned by the Instrumentation strategy.
 */
@RunWith(Parameterized.class)
public class MemoryMeterTest
{
    @Parameterized.Parameters
    public static Collection<MemoryMeter.Guess> guesses() {

        return Arrays.asList(MemoryMeter.Guess.ALWAYS_INSTRUMENTATION, MemoryMeter.Guess.ALWAYS_UNSAFE, MemoryMeter.Guess.ALWAYS_SPEC);
    }

    private final MemoryMeter.Guess guess;

    public MemoryMeterTest(MemoryMeter.Guess guess)
    {
        this.guess = guess;
    }

    @Test
    public void testCycle() throws Exception {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        Recursive dummy = new Recursive();
        long shallowSize = meter.measure(dummy);
        assertEquals("Deep size of Recursive is shallow size when child==null", shallowSize, meter.measureDeep(dummy));
        dummy.child = dummy;
        assertEquals("Deep size of Recursive is shallow size when child==this", shallowSize, meter.measureDeep(dummy));
    }

    @Test
    public void testDeep() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        Recursive root = new Recursive();
        Recursive recursive = root;
        for (int i = 0; i < 100000; i++) {
            recursive.child = new Recursive();
            recursive = recursive.child;
        }
        long shallowSize = meter.measure(root);
        assertEquals(shallowSize * 100001, meter.measureDeep(root));
    }

    @SuppressWarnings("unused")
    private static class Parent {
        private int i;
    }

    @SuppressWarnings("unused")
    private static class Child extends Parent {
        private int j;
    }

    @SuppressWarnings("unused")
    private static class Recursive {
        int i;
        Recursive child = null;
    }

    public static class Outer {
        public int[] somethingHeavy = new int[100];
        public Inner inner = new Inner();

        private class Inner {
            public int integer = 1;
        }
    }

    @Test
    public void testWithInnerClass () {
        Outer outer = new Outer();
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        long outerSize = meter.measure(outer);
        long innerSize = meter.measure(outer.inner);
        long somethingHeavySize = meter.measure(outer.somethingHeavy);

        long size = outerSize + innerSize + somethingHeavySize;

        assertEquals(size, meter.measureDeep(outer));

        meter = MemoryMeter.builder().withGuessing(guess).ignoreOuterClassReference().build();

        assertEquals(innerSize, meter.measureDeep(outer.inner));
    }

    @Test
    public void testIgnoreKnownSingletons() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        long classFieldSize = meter.measureDeep(new HasClassField());
        long enumFieldSize = meter.measureDeep(new HasEnumField());

        meter = MemoryMeter.builder().withGuessing(guess).ignoreKnownSingletons().build();

        assertNotEquals(classFieldSize, meter.measureDeep(new HasClassField()));
        assertNotEquals(enumFieldSize, meter.measureDeep(new HasEnumField()));
    }

    @Test
    public void testIgnoreNonStrongReferences() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        long classFieldSize = meter.measureDeep(new HasReferenceField());

        meter = MemoryMeter.builder().withGuessing(guess).ignoreNonStrongReferences().build();

        assertNotEquals(classFieldSize, meter.measureDeep(new HasClassField()));
    }

    @SuppressWarnings("unused")
    private static class HasClassField {
        private Class<?> cls = String.class;
    }

    @SuppressWarnings("unused")
    private static class HasEnumField {
        enum Giant {Fee, Fi, Fo, Fum}

        private Giant grunt = Giant.Fee;
    }

    @SuppressWarnings("unused")
    private static class HasReferenceField {
        private SoftReference<Date> ref = new SoftReference<Date>(new Date());
    }

    @Test
    public void testUnmeteredAnnotationOnFields() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        String s = "test";

        long stringSize = meter.measureDeep(s);
        long withoutSize = meter.measureDeep(new WithoutAnnotationField(null));
        assertEquals(stringSize + withoutSize, meter.measureDeep(new WithoutAnnotationField(s)));

        long withSize = meter.measureDeep(new WithAnnotationField(null));
        assertEquals(withSize, meter.measureDeep(new WithAnnotationField(s)));
    }

    @Test
    public void testUnmeteredTypeAnnotation() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        String s = "test";
        assertEquals(0, meter.measureDeep(new WithTypeAnnotation(s)));
    }

    @Test
    public void testUnmeteredAnnotationOnParent() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        String s = "test";
        assertEquals(0, meter.measureDeep(new WithParentWithAnnotation(s)));
    }

    @Test
    public void testUnmeteredAnnotationOnFieldParent() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        long withoutSize = meter.measureDeep(new WithFieldWithAnnotatedParent(null));

        WithParentWithAnnotation field = new WithParentWithAnnotation("test");
        long withSize = meter.measureDeep(new WithFieldWithAnnotatedParent(field));
        assertEquals(withoutSize, withSize);
    }

    @Test
    public void testUnmeteredAnnotationOnFieldInterface() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        long withoutSize = meter.measureDeep(new WithFieldAnnotatedInterface(null));

        AnnotatedInterface field = new AnnotatedInterface() {
        };
        long withSize = meter.measureDeep(new WithFieldAnnotatedInterface(field));
        assertEquals(withoutSize, withSize);
    }

    @SuppressWarnings("unused")
    private static class WithoutAnnotationField {
        private String s;

        public WithoutAnnotationField(String s) {
            this.s = s;
        }
    }

    private static class WithAnnotationField {

        @org.github.jamm.Unmetered
        private String s;

        public WithAnnotationField(String s) {
            this.s = s;
        }
    }

    @Unmetered
    @SuppressWarnings("unused")
    private static class WithTypeAnnotation {
        private String s;

        public WithTypeAnnotation(String s) {
            this.s = s;
        }
    }

    private static class WithParentWithAnnotation extends WithTypeAnnotation {

        public WithParentWithAnnotation(String s) {
            super(s);
        }
    }

    @SuppressWarnings("unused")
    private static class WithFieldWithAnnotatedParent {

        private final WithParentWithAnnotation field;

        public WithFieldWithAnnotatedParent(WithParentWithAnnotation field) {
            this.field = field;
        }
    }

    @Unmetered
    private interface AnnotatedInterface {

    }

    @SuppressWarnings("unused")
    private static class WithFieldAnnotatedInterface {

        private final AnnotatedInterface field;

        public WithFieldAnnotatedInterface(AnnotatedInterface field) {
            this.field = field;
        }
    }
    
    @Test
    public void testMeasureWithLambdaField() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        meter.measureDeep(new WithLambdaField("test"));
    }

    private static class WithLambdaField {

        private final String field;

        private final Predicate<String> greater;

        public WithLambdaField(String field)
        {
            this.field = field;
            this.greater = (e) -> WithLambdaField.this.isGreater(e);
        }
        
        public boolean isGreater(String s)
        {
            return s.compareTo(field) > 0;
        }
    }
}
