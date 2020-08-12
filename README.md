Overview
========

Jamm provides `MemoryMeter`, a Java agent for all Java versions to
measure actual object memory use including JVM overhead.


Building
========

"mvn package"; optionally, "mvn install"


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
