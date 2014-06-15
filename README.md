Overview
========

Jamm provides MemoryMeter, a java agent to measure actual object
memory use including JVM overhead.


Building
========


Jamm can be built with either ant or maven:

- `ant jar`, optionally run with tests `ant test`

- `mvn package`


Use
===

To use MemoryMeter, start the JVM with "-javaagent:<path to>/jamm.jar"

You can then use MemoryMeter in your code like this:

    MemoryMeter meter = new MemoryMeter();
    meter.measure(object);
    meter.measureDeep(object);
    meter.countChildren(object);


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

By default, MemoryMeter keeps track of descendents visited by
measureDeep with an IdentityHashMap.  This prevents both overcounting
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
