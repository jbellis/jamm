Overview
========

Jamm provides MemoryMeter, a java agent to measure actual object
memory use including JVM overhead.


Building
========

"ant jar"; optionally, "ant test"


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
