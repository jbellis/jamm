Overview
========

Jamm provides `MemoryMeter`, a Java agent for all Java versions to
measure actual object memory use including JVM overhead.


Building
========

Build + test:

    ./gradlew jar test

To run the microbenchmark:

    ./gradlew microbench
    build/microbench <JMH options...>

Public to local Maven repo (in case you need that)

    ./gradlew publishToMavenLocal

IDEs
====

You can just open the project in IntelliJ. If you've opened the Maven-ish branch(es) in IntelliJ before,
you'll have to remove the `.idea` folder.

Benchmarks
==========

See [`Microbench.java`](./microbench/org/github/jamm/jmh/Microbench.java)

To run the microbenchmarks, execute `./gradlew microbench` and run `build/microbench` from the command line.

Some microbenchmark results
(JVM options: `-Xms16g -Xmx16g -XX:+UseG1GC -XX:+AlwaysPreTouch`):

    Benchmark                        (guess)  (nested)  (refs)  Mode  Cnt    Score    Error  Units
    Microbench.cls1              ALWAYS_SPEC       100       4  avgt    3    0.243 ±  0.170  us/op
    Microbench.cls1            ALWAYS_UNSAFE       100       4  avgt    3    0.304 ±  0.078  us/op
    Microbench.cls1                    NEVER       100       4  avgt    3    0.210 ±  0.126  us/op
    Microbench.cls2              ALWAYS_SPEC       100       4  avgt    3    2.924 ±  0.272  us/op
    Microbench.cls2            ALWAYS_UNSAFE       100       4  avgt    3    3.086 ±  0.419  us/op
    Microbench.cls2                    NEVER       100       4  avgt    3    2.624 ±  0.131  us/op
    Microbench.cls3              ALWAYS_SPEC       100       4  avgt    3    5.919 ±  1.433  us/op
    Microbench.cls3            ALWAYS_UNSAFE       100       4  avgt    3    6.031 ±  1.632  us/op
    Microbench.cls3                    NEVER       100       4  avgt    3    5.329 ±  0.513  us/op
    Microbench.deeplyNested      ALWAYS_SPEC       100       4  avgt    3  500.569 ± 27.362  us/op
    Microbench.deeplyNested    ALWAYS_UNSAFE       100       4  avgt    3  495.529 ± 66.984  us/op
    Microbench.deeplyNested            NEVER       100       4  avgt    3  388.130 ± 83.949  us/op
    Microbench.justByteArray     ALWAYS_SPEC       100       4  avgt    3    0.012 ±  0.011  us/op
    Microbench.justByteArray   ALWAYS_UNSAFE       100       4  avgt    3    0.013 ±  0.004  us/op
    Microbench.justByteArray           NEVER       100       4  avgt    3    0.055 ±  0.010  us/op
    Microbench.justByteBuffer    ALWAYS_SPEC       100       4  avgt    3    0.942 ±  0.230  us/op
    Microbench.justByteBuffer  ALWAYS_UNSAFE       100       4  avgt    3    0.935 ±  0.154  us/op
    Microbench.justByteBuffer          NEVER       100       4  avgt    3    0.689 ±  0.247  us/op
    Microbench.justString        ALWAYS_SPEC       100       4  avgt    3    0.616 ±  0.165  us/op
    Microbench.justString      ALWAYS_UNSAFE       100       4  avgt    3    0.684 ±  0.636  us/op
    Microbench.justString              NEVER       100       4  avgt    3    0.574 ±  0.098  us/op


Use
===

To use MemoryMeter, start the JVM with "-javaagent:<path to>/jamm.jar"

You can then use MemoryMeter in your code like this:

    MemoryMeter meter = MemoryMeter.builder().build();
    meter.measure(object);
    meter.measureDeep(object);


If you would like to use MemoryMeter in a web application, make sure
that you do NOT put this jar in WEB-INF/lib, as that may cause problems
since your code is accessing a MemoryMeter from a different class loader
than the one loaded by the -javaagent and won't see it as initialized.

If you want MemoryMeter not to measure some specific classes, you can
mark the classes (or interfaces) using the Unmetered annotation.

The Maven coordinates for the latest version of Jamm are
========================================================

groupId:    com.github.jbellis
artifactId: jamm
version:    0.4.0

The fine print
==============

MemoryMeter can use the Runtime.deepSizeOf() functionality added in
Java 16, which is by far the most accurate and also fastest way to
measure the heap size occupied by an object or object-tree.

Alternatively, MemoryMeter can use
java.lang.instrument.Instrumentation.getObjectSize, which only claims
to provide "approximate" results, but in practice seems to work as
expected.

Two more implementations are available as well. One is using
sun.misc.Unsafe w/ reflection and one just uses reflection. Both are
inaccurate and rely on guesses. It is strongly recommended to *not
use* "unsafe" or "specification" and jamm will emit a warning on
stderr:
* The "Unsafe" implementation performs arithmetics on the "cookies"
  returned by `Unsafe.objectFieldOffset()`, althought the Javadoc says:
  `Do not expect to perform any sort of arithmetic on this offset;
  it is just a cookie which is passed to the unsafe heap memory accessors.`
  The implementation does not always consider Java object layouts in under
  all circumstances for all JVMs.
* The "specification" is just a best-effort guess-timate that can
  easily produce results that are wrong w/ newer JVMs and GCs and/or
  newer Java features.
