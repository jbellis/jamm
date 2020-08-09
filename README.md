Overview
========

Jamm provides MemoryMeter, a java agent to measure actual object
memory use including JVM overhead.


Building
========

```
$ brew install ant  # If no ant
$ vi build.xml  # If using a different java version. default 1.8
$ ant jar
$ ant test  # optionally
```


Use
===

To use MemoryMeter, start the JVM with "-javaagent:<path to>/jamm.jar"

You can then use MemoryMeter in your code like this:

    MemoryMeter meter = new MemoryMeter();
    meter.measure(object);
    meter.measureDeep(object);
    meter.countChildren(object);


If you would like to use MemoryMeter in a web application, make sure
that you do NOT put this jar in WEB-INF/lib, as that may cause problems
since your code is accessing a MemoryMeter from a different class loader
than the one loaded by the -javaagent and won't see it as initialized.

If you want MemoryMeter not to measure or count some specific fields, you can
mark them using the Unmetered annotation.

If you wish to see the Object tree visited by MemoryMeter for debugging purpose,
you can use:

     MemoryMeter meter = new MemoryMeter().enableDebug();

and MemoryMeter will print the tree to System.out.

The Maven coordinates for the latest version of Jamm are
========================================================

groupId:    com.github.jbellis
artifactId: jamm
version:    0.3.3

The fine print
==============

MemoryMeter is as accurate as
java.lang.instrument.Instrumentation.getObjectSize, which only claims
to provide "approximate" results, but in practice seems to work as
expected.

MemoryMeter uses reflection to crawl the object graph for measureDeep.
Reflection is slow: measuring a one-million object Cassandra Memtable
(that is, 1 million children from MemoryMeter.countChildren) took
about 5 seconds wall clock time.

By default, MemoryMeter keeps track of descendants visited by
measureDeep with an IdentityHashMap.  This prevents both over-counting
and infinite loops due to cycles in the object graph.  Of course, this
tracking imposes a memory cost of its own.  You can override this by
passing a different tracker provider to the MemoryMeter constructor.
Jamm provides AlwaysEmptySet, which allows add() calls but never
remembers anything, as one alternative.  (Obviously this will break
painfully if there actually are cycles present!)  A more useful
alternative, but out of Jamm's scope, would be a tracker using a Bloom
filter to implement a probabilistic set interface -- this would have
the potential of _undercounting_ due to false positives, but it would
guarantee not to loop over cycles.

Scala
=====

```
❯ scala -J-javaagent:<path to jamm jar>
Welcome to Scala 2.13.1 (OpenJDK 64-Bit Server VM, Java 1.8.0_192).
Type in expressions for evaluation. Or try :help.

scala> import org.github.jamm
import org.github.jamm

scala> val mm = new jamm.MemoryMeter()
mm: org.github.jamm.MemoryMeter = org.github.jamm.MemoryMeter@6baf25d7

scala> val bytes = Array[Byte]('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j')
bytes: Array[Byte] = Array(97, 98, 99, 100, 101, 102, 103, 104, 105, 106)

scala> val s = (bytes.map(_.toChar)).mkString
s: String = abcdefghij

scala> mm.measure(bytes)
res5: Long = 32

scala> mm.measure(s)
res6: Long = 24

scala> mm.measureDeep(bytes)
res8: Long = 32

scala> mm.measureDeep(s)
res9: Long = 64

scala> mm.countChildren(bytes)
res10: Long = 1

scala> mm.countChildren(s)
res11: Long = 2
```

Ref
===
https://stackoverflow.com/questions/31474626/how-much-memory-does-arraybyte-occupy-in-scala
