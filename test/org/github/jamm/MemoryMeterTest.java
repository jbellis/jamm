package org.github.jamm;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.github.jamm.testedclasses.PublicClassWithPackageProtectedClassField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(Parameterized.class)
public class MemoryMeterTest {

    @BeforeClass
    public static void logInfoAtStartup() {
        System.setProperty("org.github.jamm.strategies.LogInfoAtStartup", "true");
    }

    @Parameterized.Parameters
    public static Collection<MemoryMeter.Guess> guesses() {

        return Arrays.asList(MemoryMeter.Guess.INSTRUMENTATION,
                             MemoryMeter.Guess.INSTRUMENTATION_AND_SPECIFICATION,
                             MemoryMeter.Guess.UNSAFE,
                             MemoryMeter.Guess.SPECIFICATION);
    }

    private final MemoryMeter.Guess guess;

    public MemoryMeterTest(MemoryMeter.Guess guess)
    {
        this.guess = guess;
    }

    @Test
    public void testWithNull() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals(0L, meter.measure(null));
        assertEquals(0L, meter.measureDeep(null));
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
    private static class Recursive {
        int i;
        Recursive child = null;
    }

    @SuppressWarnings("unused")
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
    public void testMeasureKnownSingletons() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).measureKnownSingletons().build();

        long classFieldSize = meter.measureDeep(new HasClassField());
        long enumFieldSize = meter.measureDeep(new HasEnumField());

        meter = MemoryMeter.builder().withGuessing(guess).build();

        assertNotEquals(classFieldSize, meter.measureDeep(new HasClassField()));
        assertNotEquals(enumFieldSize, meter.measureDeep(new HasEnumField()));
    }

    @Test
    public void testClassFilteringWithObjectField() {
        // Measure excluding class and enum object
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        HasObjectField withClass = new HasObjectField(String.class);
        HasObjectField withString = new HasObjectField(1);

        assertEquals(meter.measure(withClass), meter.measureDeep(withClass));
        assertEquals(meter.measure(withString) + meter.measure(1), meter.measureDeep(withString));
    }

    @Test
    public void testIgnoreNonStrongReferences() throws InterruptedException {
        // The Reference fields 'next' and 'discovered' are used by 'ReferenceQueue' instances and are not part of what
        // we want to measure. The 'queue' field is either a singleton 'ReferenceQueue.NULL' or a provided queue that user hold a reference to.
        // Therefore, those fields should be ignored.
        MemoryMeter meterIgnoring = MemoryMeter.builder().withGuessing(guess).build();
        MemoryMeter meterMeasuring = MemoryMeter.builder().withGuessing(guess).measureNonStrongReferences().build();

        // test SoftReference
        SoftReference<Date> ref = new SoftReference<Date>(new Date());
        long refShallowSize = meterIgnoring.measure(ref);
        assertEquals(refShallowSize,  meterIgnoring.measureDeep(ref));

        // SoftReference + deep queue + referent
        long deepSize = refShallowSize + meterMeasuring.measureDeep(new ReferenceQueue<Object>()) + meterMeasuring.measure(new Date());
        assertEquals(deepSize,  meterMeasuring.measureDeep(ref));

        HasReferenceField hasReferenceField = new HasReferenceField(ref);
        long hasReferenceFieldShallowSize = meterIgnoring.measure(hasReferenceField);
        assertEquals(refShallowSize + hasReferenceFieldShallowSize, meterIgnoring.measureDeep(hasReferenceField));

        // HasReferenceField + SoftReference + deep queue + referent
        deepSize = hasReferenceFieldShallowSize + refShallowSize + meterMeasuring.measureDeep(new ReferenceQueue<Object>()) + meterMeasuring.measure(new Date());
        assertEquals(deepSize,  meterMeasuring.measureDeep(hasReferenceField));

        // Test ReferenceQueue measurement with one object
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        MyPhantomReference p = new MyPhantomReference(new Object(), queue);

        refShallowSize = meterIgnoring.measure(p);
        assertEquals(refShallowSize,  meterIgnoring.measureDeep(p));

        // PhantomReference + deep queue + referent
        deepSize = refShallowSize + meterMeasuring.measureDeep(new ReferenceQueue<Object>()) + meterMeasuring.measure(new Object());
        assertEquals(deepSize,  meterMeasuring.measureDeep(p));

        System.gc();
        // wait for garbage collection
        long start = System.currentTimeMillis();
        while (!p.isEnqueued() && (System.currentTimeMillis() - start) < 1000)
            Thread.yield();

        Assert.assertTrue(p.isEnqueued());

        long queueShallowSize = meterIgnoring.measure(queue);
        long lockSize = meterIgnoring.measure(new Object()); // The ReferenceQueue lock field has the same size as an empty object.
        assertEquals(queueShallowSize + lockSize,  meterIgnoring.measureDeep(queue));

        assertEquals(p, queue.poll());
    }

    private static class MyPhantomReference extends PhantomReference<Object> {

        public MyPhantomReference(Object referent, ReferenceQueue<Object> q) {
            super(referent, q);
        }
    }

    @SuppressWarnings("unused")
    private static class HasObjectField {
        private Object obj = String.class;

        public HasObjectField(Object obj) {
            this.obj = obj;
        }
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
        private final SoftReference<Date> ref;

        public HasReferenceField(SoftReference<Date> ref) {
            this.ref = ref;
        }
    }

    @Test
    public void testUnmeteredAnnotationOnFields() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        String s = "test";

        long stringDeepSize = meter.measureDeep(s);
        long shallowSize = meter.measure(new WithoutAnnotationField(null));
        assertEquals(stringDeepSize + shallowSize, meter.measureDeep(new WithoutAnnotationField(s)));

        shallowSize = meter.measure(new WithAnnotationField(null));
        assertEquals(shallowSize, meter.measureDeep(new WithAnnotationField(s)));
    }

    @Test
    public void testUnmeteredTypeAnnotation() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        String s = "test";
        assertEquals(0, meter.measureDeep(new ClassWithAnnotation(s)));
    }

    @Test
    public void testUnmeteredAnnotationOnParent() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals(0, meter.measureDeep(new WithParentWithAnnotation("test")));
    }

    @Test
    public void testUnmeteredAnnotationOnImplementedInteface() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals(0, meter.measureDeep(new WithAnnotatedInterface()));
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

    @Test
    public void testUnmeteredAnnotationOnFieldImplementedInterface() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        long withoutSize = meter.measureDeep(new WithFieldTypeWithAnnotatedInterface(null));

        WithAnnotatedInterface field = new WithAnnotatedInterface();
        long withSize = meter.measureDeep(new WithFieldTypeWithAnnotatedInterface(field));
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

        @Unmetered
        private String s;

        public WithAnnotationField(String s) {
            this.s = s;
        }
    }

    @Unmetered
    @SuppressWarnings("unused")
    private static class ClassWithAnnotation {
        private String s;

        public ClassWithAnnotation(String s) {
            this.s = s;
        }
    }

    private static class WithParentWithAnnotation extends ClassWithAnnotation {

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

    private static class WithAnnotatedInterface implements AnnotatedInterface {

    }

    @SuppressWarnings("unused")
    private static class WithFieldTypeWithAnnotatedInterface {

        private final WithAnnotatedInterface field;

        public WithFieldTypeWithAnnotatedInterface(WithAnnotatedInterface field) {
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

        @SuppressWarnings("unused")
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

    @Test
    public void testMeasureDeepWithPublicFieldInPackagePrivate() {

        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();
        PublicClassWithPackageProtectedClassField object = new PublicClassWithPackageProtectedClassField("publicField", "packageProtected", "protected", "private");
        long expected = object.measureDeep(meter);
        assertEquals(expected, meter.measureDeep(object));
    }

    @Test
    public void testMeasurableInstance() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        ChildMeasurable bart = new ChildMeasurable("Bart");
        ChildMeasurable lisa = new ChildMeasurable("Lisa");
        ChildMeasurable maggie = new ChildMeasurable("Maggie");
        ParentMeasurable homer = new ParentMeasurable("Homer", bart, lisa, maggie);

        long expectedSize = meter.measure(homer) 
                + meter.measureDeep("Homer")
                + meter.measureArray(new Object[3])
                + meter.measure(bart)
                + meter.measureDeep("Bart")
                + meter.measure(lisa)
                + meter.measureDeep("Lisa")
                + meter.measure(maggie)
                + meter.measureDeep("Maggie");

        assertEquals(expectedSize, meter.measureDeep(homer));
        Assert.assertTrue("the addChildrenTo method has not been called", homer.checkUsage());
    }

    private static class ParentMeasurable implements Measurable
    {
        private final String name;

        private final ChildMeasurable[] children;

        boolean hasAddChildrenBeenUsed; 

        public ParentMeasurable(String name, ChildMeasurable... children) {
            this.name = name;
            this.children = children;
        }

        @Override
        public void addChildrenTo(MeasurementStack stack) {
            hasAddChildrenBeenUsed = true;

            stack.pushObject(this, "name", name);
            stack.pushObject(this, "children", children);
        }

        public boolean checkUsage() {

            boolean check = hasAddChildrenBeenUsed;
            for (ChildMeasurable child : children)
                check &= child.hasAddChildrenBeenUsed;
            return check;
        }
    }

    private static class ChildMeasurable implements Measurable
    {
        private String name;
        boolean hasAddChildrenBeenUsed; 

        public ChildMeasurable(String name) {
            this.name = name;
        }
 
        @Override
        public void addChildrenTo(MeasurementStack stack) {
            hasAddChildrenBeenUsed = true;

            stack.pushObject(this, "name", name);
        }
    }
}
