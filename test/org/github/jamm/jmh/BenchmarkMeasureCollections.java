package org.github.jamm.jmh;

import org.github.jamm.MemoryMeter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.HashMap;
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

    public static final int NUM_ELEMENTS_PER_COLLECTION = 1000;

    @Param({"INSTRUMENTATION", "INSTRUMENTATION_AND_SPECIFICATION", "SPECIFICATION", "UNSAFE"})
    private String guess;

    private MemoryMeter meter;

    private static ArrayList<Person>[] arrayLists;

    private static HashMap[] hashMaps;

    static {
        try {
            Random random = new Random();

            arrayLists = createArrayLists(random);

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
        for (ArrayList list : arrayLists) {
            bh.consume(meter.measureDeep(list));
        }
    }

    @Benchmark
    public void measureHashMapDeep(Blackhole bh) {
        for (HashMap map : hashMaps) {
            bh.consume(meter.measureDeep(map));
        }
    }

    private static ArrayList<Person>[] createArrayLists(Random random) {
        ArrayList<Employer> employers = new ArrayList<>();
        for (int i = 0; i < NUM_ELEMENTS_PER_COLLECTION; i++) {
            employers.add(new Employer(createRandomName(random)));
        }

        ArrayList<Person>[] lists = new ArrayList[NUM_COLLECTIONS_OF_EACH_TYPE];
        for (int i = 0; i < lists.length; i++) {
            ArrayList<Person> people = new ArrayList<>(NUM_ELEMENTS_PER_COLLECTION);
            for (int j = 0; j < NUM_ELEMENTS_PER_COLLECTION; j++) {
                people.add(createRandomPerson(random, employers));
            }
            lists[i] = people;
        }

        return lists;
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
