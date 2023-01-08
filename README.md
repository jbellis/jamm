# Overview

Jamm provides `MemoryMeter`, a Java agent for all Java versions to
measure actual object memory use including JVM overhead.


# Building


"mvn package"; optionally, "mvn install"


# Use


The best way to use `MemoryMeter` is to start the JVM with "-javaagent:<path to>/jamm.jar" in order to use the
`Instrumentation` strategy to guess objects sizes.

`MemoryMeter` can be used in your code like this:

    MemoryMeter meter = new MemoryMeter();
    meter.measure(object);
    meter.measureDeep(object);
    meter.countChildren(object);


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

# The fine print

## Measurement strategies

`MemoryMeter` can use different strategies to guess the objects sizes.

### Instrumentation

If the JVM has been started with `-javaagent`, `MemoryMeter` will use 
`java.lang.instrument.Instrumentation.getObjectSize` to get an estimate of the space required to store
the given object.

### Unsafe

`MemoryMeter` will use `Unsafe.objectFieldOffset` to guess the object offset. Unfortunately, this
method might not always be accurate as the value returned is not the true offset, but a cookie that looks like
the offset in the current Hotspot implementation.

### Specification

`MemoryMeter` will try to guess the object size based on what it knows from the JVM.

## Object graph crawling

When `measureDeep` is called `MemoryMeter` will uses reflection to crawl the object graph.
In order to prevent infinite loops due to cycles in the object graph `MemoryMeter` track visited objects
imposing a memory cost of its own.

## Skipping objects

If you want `MemoryMeter` not to measure some specific classes or fields, you can
mark the classes/interfaces or fields using the
[`@Unmetered`](./src/org/github/jamm/Unmetered.java) annotation.

