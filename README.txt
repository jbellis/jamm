Jamm provides MemoryMeter, a java agent to measure actual object
memory use including JVM overhead.

To use MemoryMeter, start the JVM with "-javaagent:<path to>/jamm.jar"

You can then use MemoryMeter in your code like this:

    MemoryMeter meter = new MemoryMeter();
    meter.measure(object);
    meter.measureDeep(object);

The fine print:

MemoryMeter is as accurate as
java.lang.instrument.Instrumentation.getObjectSize, which only claims
to provide "approximate" results, but in practice seems to work as
expected.
