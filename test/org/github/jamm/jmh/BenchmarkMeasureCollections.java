package org.github.jamm.jmh;

import org.github.jamm.MemoryMeter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Threads(3)
@Fork(value = 1, jvmArgsPrepend = {
        "-javaagent:target/jamm-0.4.1-SNAPSHOT.jar",
})
@Warmup(iterations = 4, time = 5)
@Measurement(iterations = 5, time = 5)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BenchmarkMeasureCollections {
    public static final int NUM_COLLECTIONS_OF_EACH_TYPE = 100;

    /**
     * The {@link #percentages} & {@link #numElementBoundaries} work together to define the distribution of
     * size of the collections.
     *
     * In this case, we want the random collection sizes to have distribution:
     * 70% between 0 and 100 elements
     * 20% between 100 and 700 elements
     * 10% between 700 and 1000 elements
     */
    public static final int[] percentages = {70, 20, 10};
    public static final int[] numElementBoundaries = {100, 700, 1000};

    @Param({"INSTRUMENTATION", "INSTRUMENTATION_AND_SPECIFICATION", "SPECIFICATION", "UNSAFE"})
    private String guess;

    private MemoryMeter meter;

    private static ArrayList<Person>[] arrayLists;

    /**
     * Measuring linked lists is special as it imitates measuring very deep object graphs.
     */
    private static LinkedList<Person>[] linkedLists;

    private static HashMap<String, Person>[] hashMaps;

    static {
        try {
            validateConfigurationOfSizeDistribution();
            // IMPORTANT: We must use a fixed seed so that multiple runs are comparable especially
            // when comparing the performance impact of code changes. The size and contents of
            // each collection is chosen randomly so a fixed seed guarantees that multiple runs
            // are testing against the exact same data otherwise we might perceive an invalid
            // performance improvement when in fact the data was easier to measure in the 2nd run
            Random random = new Random(0);

            arrayLists = createArrayLists(random);

            linkedLists = new LinkedList[arrayLists.length];
            for (int i = 0; i < arrayLists.length; i++) {
                linkedLists[i] = new LinkedList<Person>(arrayLists[i]);
            }

            hashMaps = new HashMap[arrayLists.length];
            for (int i = 0; i < hashMaps.length; i++) {
                hashMaps[i] = mapFromNameToPerson(arrayLists[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Setup(Level.Iteration)
    public void setup() throws Exception {
        MemoryMeter.Guess guess = MemoryMeter.Guess.valueOf(this.guess);
        this.meter = MemoryMeter.builder().withGuessing(guess).build();
    }

    @Benchmark
    public void measureArrayListDeep(Blackhole bh) {
        for (ArrayList<Person> list : arrayLists) {
            bh.consume(meter.measureDeep(list));
        }
    }

    @Benchmark
    public void measureLinkedListDeep(Blackhole bh) {
        for (LinkedList<Person> linkedList : linkedLists) {
            bh.consume(meter.measureDeep(linkedList));
        }
    }

    @Benchmark
    public void measureHashMapDeep(Blackhole bh) {
        for (HashMap<String, Person> map : hashMaps) {
            bh.consume(meter.measureDeep(map));
        }
    }

    private static void validateConfigurationOfSizeDistribution() {
        if (numElementBoundaries.length == 0) {
            throw new IllegalStateException("At least one boundary must be defined");
        }
        if (percentages.length != numElementBoundaries.length) {
            throw new IllegalStateException("The # of percentages must match the # of boundaries");
        }

        int totalPercent = 0;
        for (int percent : percentages) {
            if (percent <= 0) {
                throw new IllegalStateException("The percentages must be positive");
            }
            totalPercent += percent;
        }
        if (totalPercent != 100) {
            throw new IllegalStateException("The percentages should add to 100");
        }

        int previousBoundary = 0;
        for (int boundary : numElementBoundaries) {
            if (boundary <= previousBoundary) {
                throw new IllegalStateException("Each boundary must be greater than the previous boundary");
            }
            previousBoundary = boundary;
        }
    }

    private static ArrayList<Person>[] createArrayLists(Random random) {
        // The ratio of people to employers is 10 to 1
        int numEmployers = 1 + numElementBoundaries[numElementBoundaries.length - 1] / 10;
        ArrayList<Employer> employers = new ArrayList<>(numEmployers);
        for (int i = 0; i < numEmployers; i++) {
            employers.add(new Employer(createRandomName(random)));
        }

        ArrayList<Person>[] lists = new ArrayList[NUM_COLLECTIONS_OF_EACH_TYPE];
        for (int i = 0; i < lists.length; i++) {
            int numElements = determineCollectionSize(random);

            ArrayList<Person> people = new ArrayList<>(numElements);
            for (int j = 0; j < numElements; j++) {
                people.add(createRandomPerson(random, employers));
            }
            lists[i] = people;
        }

        return lists;
    }

    private static int determineCollectionSize(Random random) {
        double selector = random.nextInt(100);

        int minNumElements = 0;
        int currentPercent = 0;
        for (int i = 0; i < percentages.length; i++) {
            currentPercent += percentages[i];
            if (selector < currentPercent) {
                int maxNumElements = numElementBoundaries[i];
                return minNumElements + random.nextInt(maxNumElements - minNumElements);
            }
            minNumElements = numElementBoundaries[i];
        }
        throw new IllegalStateException("This line should never be reached");
    }

    private static HashMap<String, Person> mapFromNameToPerson(List<Person> people) {
        HashMap<String, Person> map = new HashMap<>((int) (people.size() / 0.7));
        for (Person person : people) {
            map.put(person.lastName, person);
        }
        return map;
    }

    private static Person createRandomPerson(Random random, List<Employer> employers) {
        int age = random.nextInt(100);
        String firstName = createRandomName(random);
        String lastName = createRandomName(random);
        boolean isMarried = random.nextBoolean();
        int numDependents = random.nextInt(5);

        // children and retires aren't employed
        if (age < 18 || age > 65) {
            return new Person(firstName, lastName, age, isMarried, numDependents);
        }

        Employer employer = employers.get(random.nextInt(employers.size()));
        long salaryCents = random.nextLong();

        // 10% of employees are managers
        if (random.nextDouble() < 0.1) {
            int numReports = 1 + random.nextInt(15);
            return new Manager(employer, salaryCents, numReports, firstName, lastName, age, isMarried, numDependents);
        }

        return new Employee(employer, salaryCents, firstName, lastName, age, isMarried, numDependents);
    }

    private static String createRandomName(Random random) {
        int length = 2 + random.nextInt(7);

        return random.ints('a', 'z' + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static class Person {
        private String firstName;

        private String lastName;

        private int age;

        private boolean isMarried;

        private int numDependents;

        public Person(String firstName, String lastName, int age, boolean isMarried, int numDependents) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
            this.isMarried = isMarried;
            this.numDependents = numDependents;
        }

        @Override
        public String toString() {
            // Using the fields to prevent any optimizations that might eliminate them
            return firstName + lastName + age + isMarried + numDependents;
        }
    }

    public static class Employer {
        private String companyName;

        public Employer(String companyName) {
            this.companyName = companyName;
        }

        @Override
        public String toString() {
            // Using the fields to prevent any optimizations that might eliminate them
            return companyName;
        }
    }

    public static class Employee extends Person {
        private Employer employer;

        private long salaryCents;

        public Employee(
                Employer employer,
                Long salaryCents,
                String firstName,
                String lastName,
                int age,
                boolean isMarried,
                int numDependents
        ) {
            super(firstName, lastName, age, isMarried, numDependents);
            this.employer = employer;
            this.salaryCents = salaryCents;
        }

        @Override
        public String toString() {
            // Using the fields to prevent any optimizations that might eliminate them
            return super.toString() + salaryCents + employer.toString();
        }
    }

    public static class Manager extends Employee {
        private int numReports;

        public Manager(
                Employer employer,
                long salaryCents,
                int numReports,
                String firstName,
                String lastName,
                int age,
                boolean isMarried,
                int numDependents
        ) {
            super(employer, salaryCents, firstName, lastName, age, isMarried, numDependents);
            this.numReports = numReports;
        }

        @Override
        public String toString() {
            // Using the fields to prevent any optimizations that might eliminate them
            return super.toString() + numReports;
        }
    }
}
