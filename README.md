# Overview

Jamm provides `MemoryMeter`, a Java agent for all Java versions to
measure actual object memory use including JVM overhead.

Jamm assume that the JVM running the code is an HotSpot JVM. It has not been tested with other type of JVMs.

# Building

"mvn package"; optionally, "mvn install"

# Setup your toolchains.xml

We want to compile and test using different JVM versions.  Configuration option jdkToolchain can be used to supply an alternate 
toolchain specification. To achieve our goal we need first to setup `toolchains.xml`.
The `toolchains.xml` file is the configuration file where you set the installation paths of your toolchains. This file 
should be put in your `${user.home}/.m2` directory. When the maven-toolchains-plugin executes, it looks for the `toolchains.xml`
file, reads it and looks for a toolchain matching the toolchains requirements configured in the plugin.
Jamm repo contains a `toolchains.example.xml` which you can use as a baseline for your own `toolchains.xml`. You need it
to be able to run the tests. Copy `toolchains.example.xml` to `${user.home}/.m2`, rename the file to `toolchains.xml`.
In `toolchains.xml`, check to update your vendor and jdkHome for JDK8, JDK11 and JDK17 which you have installed on your machine.

# Running Tests

The tests can be run with "mvn test". The `JvmArgs` property can be used to specify the JVM arguments that can be used for running the tests.
For example:

```
mvn test -DjvmArgs="-Xmx64g"
mvn test -DjvmArgs="-Xmx64g -XX:ObjectAlignmentInBytes=16 -XX:-UseCompressedClassPointers"
```

`mvn test` runs all tests with JDK8, JDK11, and then with JDK17
To run the tests with only one particular JDK version run:
* for JDK8:
```
mvn surefire:test@test-default
```
or
```
mvn surefire:test
```
* for JDK11:
```
mvn surefire:test@test-jdk11
```
* for JDK17:
```
mvn surefire:test@test-jdk17
```

# Use


The best way to use `MemoryMeter` is to start the JVM with "-javaagent:<path to>/jamm.jar" in order to use the
`Instrumentation` strategy to guess objects' sizes.

`MemoryMeter` can be used in your code like this:

    MemoryMeter meter = MemoryMeter.builder().build();
    meter.measure(object);
    meter.measureDeep(object);


If you would like to use `MemoryMeter` in a web application, make sure
that you do NOT put this jar in `WEB-INF/lib`, as that may cause problems
since your code is accessing a `MemoryMeter` from a different class loader
than the one loaded by the `-javaagent` and won't see it as initialized.

If you want `MemoryMeter` not to measure some specific classes, you can
mark the classes (or interfaces) using the `Unmetered` annotation.

# The Maven coordinates for the latest version of Jamm

```
  <groupId>com.github.jbellis</groupId>
  <artifactId>jamm</artifactId>
  <version>0.4.0-SNAPSHOT</version>
```

# 0.4.0 breaking changes

The 0.4.0 version comes with speed improvements and support java versions up to Java 17 but also some breaking
changes at the API level. 
* The `MemoryMeter` constructor and the static methods used to configure the different options (`omitSharedBufferOverhead`, `withGuessing`, `ignoreOuterClassReference`, `ignoreKnownSingletons`, `ignoreNonStrongReferences`, `enableDebug`) have been removed. Instead `MemoryMeter` instances must be created through a `Builder`.
* The ability to provide a tracker for visited object has been removed.
* `Guess.NEVER` has been renamed `Guess.ALWAYS_INSTRUMENTATION` for more clarity.
* `MemoryMeter.countChildren` has been removed.
* The `MemoryMeter.measure` and `MemoryMeter.measureDeep` now accept `null` parameters
* The behavior of the `omitSharedBufferOverhead` has been changed as it was incorrect. It was not counting correctly for direct ByteBuffers and considering some buffer has shared even if they were not.
* Jamm is not trying anymore to support non Hotspot JVM (e.g. OpenJ9)
* By default `MemoryMeter.measureDeep` is now ignoring the space occupied by known singletons such as `Class` objects, `enums`, `ClassLoaders`, `AccessControlContexts` as well as non-strong references
(like weak/soft/phantom references). If you want `MemoryMeter` to measure them you need to enable those measurements through `MemoryMeter.builder().measureKnownSingletons()` and `MemoryMeter.builder().measureNonStrongReferences()`.
* When measuring direct `ByteBuffer` objects `MemoryMeter` is ignoring some fields from the Cleaner as it might lead to some incorrect measurements by including references to other Cleaner instances
* When measuring `Thread` objects `MemoryMeter` is ignoring the `group` field as it references the all the threads from the group

# Supported Java versions

The 0.4.0 release has been tested with Java 8, 11 and 17 and the following JVM arguments that can affect the memory layout:
* `-Xmx`
* `UseCompressedClassPointers`
* `ObjectAlignmentInBytes`
* `UseCompressedOops`
* `RestrictContended`
* `EnableContended`
* `ContendedPaddingWidth`
* `UseEmptySlotsInSupers`

The `Specification` strategy does not work correctly with `UseEmptySlotsInSupers` disabled for some classes (like direct `ByteBuffer`)
that interleave fields from different classes when they should not.

# The fine print

## Measurement strategies

`MemoryMeter` can use different strategies to guess the objects sizes. We have tried to ensure that the output of the strategies is the same.

### Instrumentation

If the JVM has been started with `-javaagent`, `MemoryMeter` will use 
`java.lang.instrument.Instrumentation.getObjectSize` to get an estimate of the space required to store
the given object. It is the safest strategy.

### Unsafe

`MemoryMeter` will use `Unsafe.objectFieldOffset` to guess the object offset.
Java 14 introduced records and Java 15 introduced Hidden classes which are used from Java 15 onward for Lambda expressions. 
Unfortunately, calling `Unsafe.objectFieldOffset` on the `Field` of a record or hidden class will result into an `UnsupportedOperationException` therefore
for record and hidden classes the unsafe strategy delegates the measurement to the specification strategy.

### Specification

`MemoryMeter` will guess the object size based on what it knows from the JVM.

## Object graph crawling

When `measureDeep` is called `MemoryMeter` will use reflection to crawl the object graph.
In order to prevent infinite loops due to cycles in the object graph `MemoryMeter` tracks visited objects
imposing a memory cost of its own.

Java 9 introduced the Java Platform Module System (JPMS) that made illegal reflective access between some modules. This is breaking
the ability for Jamm to crawl the object graph. To avoid that problem, if Jamm detects that it cannot use reflection to retrieve
field data it will rely on `Unsafe` to do it. Unfortunately, despite the fact that the code is designed to go around those 
illegal accesses the JVM might emit some warning for access that only will be illegal in future versions. The `Unsafe` approach
 might also fail for some scenarios as `Unsafe.objectFieldOffset` do not work for `records` or `hidden` classes such 
 as lambda expressions.
 
 By default `MemoryMeter.measureDeep` is ignoring known singletons such as `Class` objects, `enums`, `ClassLoaders`, `AccessControlContexts` as well as non-strong references
(like weak/soft/phantom references). If you want `MemoryMeter` to measure them you need to enable those measurements through
 `MemoryMeter.builder().measureKnownSingletons()` and `MemoryMeter.builder().measureNonStrongReferences()`.

## Skipping objects

If you want `MemoryMeter` not to measure some specific classes or fields, you can
mark the classes/interfaces or fields using the
[`@Unmetered`](./src/org/github/jamm/Unmetered.java) annotation.

```
    public class WithAnnotationField {

        @Unmetered
        private String s;

        public WithAnnotationField(String s) {
            this.s = s;
        }
        ...
    }
```

```
    @Unmetered
    private static class WithTypeAnnotation {
        private String s;

        public WithTypeAnnotation(String s) {
            this.s = s;
        }
        ...
    }
```

For a finer control on which classes and fields should be filtered out it is possible to use the `MemoryMeter(MemoryMeterStrategy, FieldAndClassFilter, FieldFilter , boolean, MemoryMeterListener.Factory)` constructor.

## ByteBuffer measurements

`MemoryMeter` has 2 ways to measure ByteBuffers. The default one or the `omitSharedBufferOverhead` one.
By default, `MemoryMeter.measureDeep` will crawl the object graph and sum its different elements.
For a `HeapByteBuffer` like `ByteBuffer.allocate(20)` the crawled graph will be:

```
root [java.nio.HeapByteBuffer] 96 bytes (56 bytes)
  |
  +--hb [byte[]] 40 bytes (40 bytes)
```
For a `DirectByteBuffer` like `ByteBuffer.allocateDirect(20)` the crawled graph will be:

```
root [java.nio.DirectByteBuffer] 136 bytes (64 bytes)
  |
  +--cleaner [jdk.internal.ref.Cleaner] 72 bytes (40 bytes)
    |
    +--thunk [java.nio.DirectByteBuffer$Deallocator] 32 bytes (32 bytes)
```

In reality the `cleaner` field has some extra fields that `MemoryMeter` is excluding as they might result in an incorrect
measurement. Those fields are: 
* `queue` as it is a dummy queue referenced by all `Cleaner` instances
* `next` and `prev` as they are used to create a doubly-linked list of live cleaners and therefore refer to other Cleaners instances

If a slice of the previous buffers is used `buffer.slice().limit(5)` the heap buffer will have a reference to the original buffer array
while the direct buffer will have a direct reference to the original direct buffer:

```
root [java.nio.DirectByteBuffer] 200 bytes (64 bytes)
  |
  +--att [java.nio.DirectByteBuffer] 136 bytes (64 bytes)
    |
    +--cleaner [jdk.internal.ref.Cleaner] 72 bytes (40 bytes)
      |
      +--thunk [java.nio.DirectByteBuffer$Deallocator] 32 bytes (32 bytes)
```

In both cases, the size of the buffers includes part of the size coming from the original buffer that we might not be interested in measuring.
If the `omitSharedBufferOverhead` option is used `MemoryMeter` will try to remove the size of shared data from its measurements.
For heap buffers, if only a portion of the array is used (capacity > remaining), `MemoryMeter` will only take into account the remaining bytes
instead of the size of the array object. For direct buffer, `MemoryMeter` will ignore the field referencing the original buffer object in its computation.

A read-only buffer might actually be the only representation of a `ByteBuffer` so `MemoryMeter` will not by default consider read-only buffers as shared.
Pre-java 12, a read-only buffer created from a direct `ByteBuffer` was using the source buffer as an attachment for liveness rather than the source buffer's
 attachment (https://bugs.openjdk.org/browse/JDK-8208362). Therefore prior to Java 12, `MemoryMeter` could easily determine for read-only direct buffers which part was shared.
Unfortunately, the approach did not work anymore since Java 12. Due to that, for java versions >= 12, `MemoryMeter` use the same approach as the one used for heap buffers to determine
if the data of a read-only direct buffer is shared (capacity > remaining).

## @Contended

 `@Contended` was introduced in Java 8 as `sun.misc.Contended` but was repackaged in the `jdk.internal.vm.annotation` package in Java 9.
 Therefore, in Java 9+ unless `-XX:-RestrictContended` or `--add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED` are specified `MemoryMeter` will not have access
 to the `value()` method of `@Contended` and will be unable to retrieve the contention group tags. Making it potentially unable to computes the correct sizes with the `Unsafe` or `Spec` strategies.
 As it also means that only the internal Java classes will use that annotation, `MemoryMeter` will rely on its knowledge of those internal classes to try to go around that problem.

## Debugging

In order to see the object tree visited when calling `MemoryMeter.measureDeep` and ensuring that it matches your
expectations you can build the `MemoryMeter` instance using `printVisitedTree`:

```
    MemoryMeter meter = MemoryMeter.builder().printVisitedTree().build();
```

If a problem occurs while crawling the graph, `MemoryMeter` will not print the graph in the `System.out` but instead will
print in `System.err` the stack from the object that could not be accessed up to the original input object.

## JMH Benchmarks

The Jamm JMH benchmarks can be run using:

```
    mvn jmh:benchmark
```

