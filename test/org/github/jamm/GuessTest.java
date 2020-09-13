package org.github.jamm;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assume.assumeThat;

/**
 * UGLY AS SIN, BUT QUICK TO WRITE AND WORKS.
 * We generate java source code for random data classes (with parent hierarchy), write it to a temp dir,
 * call javac on it, and instrument it.
 */
@RunWith(Parameterized.class)
public class GuessTest {

    @SuppressWarnings("deprecation")
    @Parameterized.Parameters
    public static Collection<MemoryMeter.Guess> guesses() {
        List<MemoryMeter.Guess> guesses = new ArrayList<>();
        // Ignoring "unsafe" here, as it is extremely unreliable and often wrong:
        // Depends on both the information on j.l.r.Field and Unsafe.objectFieldOffset,
        // which just returns a "cookie" and not a meaningful value that can be used
        // for arithmetics. See javadoc for Unsafe.objectFieldOffset():
        // "Do not expect to perform any sort of arithmetic on this offset;
        // it is just a cookie which is passed to the unsafe heap memory accessors."
        //
        // if (MemoryMeterUnsafe.hasUnsafe())
        //     guesses.add(MemoryMeter.Guess.ALWAYS_UNSAFE);
        guesses.add(MemoryMeter.Guess.ALWAYS_SPEC);
        return guesses;
    }

    private final MemoryMeter.Guess guess;

    public GuessTest(MemoryMeter.Guess guess)
    {
        this.guess = guess;
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeepNecessaryClasses() {
        assumeThat(guess, not(is(MemoryMeter.Guess.ALWAYS_SPEC)));
        Assert.assertTrue("MemoryMeter not initialised " + guess, MemoryMeterInstrumentation.hasInstrumentation());
        final MemoryMeter instrument = MemoryMeter.builder().withGuessing(MemoryMeter.Guess.ALWAYS_INSTRUMENTATION).build();
        final MemoryMeter guess = MemoryMeter.builder().withGuessing(this.guess).build();
        final List<Object> objects = new ArrayList<>(); {
            final ConcurrentSkipListMap<Long, Long> map = new ConcurrentSkipListMap<>();
            for (long i = 0 ; i < 100 ; i++)
                map.put(i, i);
            objects.add(map);
        }
        int failures = 0;
        for (final Object obj : objects) {
            long instrumented = instrument.measureDeep(obj);
            long guessed = guess.measureDeep(obj);
            // SPEC is allowed to overcount (aka: better safe than sorry)
            if (!verify(instrumented, guessed, "Deep necessary / Guessed %d, instrumented %d for %s for %s", guessed, instrumented, obj.getClass().getName(), guess))
                failures++;
        }
        Assert.assertEquals("Not all guesses matched the instrumented values. See output for details. " + guess, 0, failures);
    }

    @Test
    public void testProblemClasses() throws InterruptedException, ExecutionException, IOException, IllegalAccessException, InstantiationException {
        Assert.assertTrue("MemoryMeter not initialised " + guess, MemoryMeterInstrumentation.hasInstrumentation());
        final MemoryMeter instrument = MemoryMeter.builder().withGuessing(MemoryMeter.Guess.ALWAYS_INSTRUMENTATION).build();
        final MemoryMeter guess = MemoryMeter.builder().withGuessing(this.guess).build();
        List<Def> defs = new ArrayList<>();
        defs.add(Def.parse("{long*1}->{float*1}"));
        defs.add(Def.parse("{long*1}->{byte*4}"));
        defs.add(Def.parse("{long*1}->{byte*7}"));
        defs.add(Def.parse("{long*1}->{byte*9}"));
        defs.add(Def.parse("{long*1}->{float*1}->{long*1}->{float*1}"));
        final List<GeneratedClass> classes = compile(defs);
        int failures = 0;
        for (final GeneratedClass clazz : classes) {
            Object obj = clazz.clazz.newInstance();
            long instrumented = instrument.measure(obj);
            long guessed = guess.measure(obj);
            // SPEC is allowed to overcount (aka: better safe than sorry)
            if (!verify(instrumented, guessed, "Problem class / Guessed %d, instrumented %d for %s for %s", guessed, instrumented, clazz.description, guess))
                failures++;
        }
        Assert.assertEquals("Not all guesses matched the instrumented values. See output for details. " + guess, 0, failures);
    }

    @Test
    public void testRandomClasses() throws InterruptedException, ExecutionException {
        Assert.assertTrue("MemoryMeter not initialised " + guess, MemoryMeterInstrumentation.hasInstrumentation());
        final int testsPerCPU = 100;
        final MemoryMeter instrument = MemoryMeter.builder().withGuessing(MemoryMeter.Guess.ALWAYS_INSTRUMENTATION).build();
        final MemoryMeter guess = MemoryMeter.builder().withGuessing(this.guess).build();
        final List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0 ; i < Runtime.getRuntime().availableProcessors() ; i++) {
            results.add(EXEC.submit(() -> {
                final List<GeneratedClass> classes = randomClasses(testsPerCPU);
                int failures = 0;
                for (final GeneratedClass clazz : classes) {
                    Object obj = clazz.clazz.newInstance();
                    long instrumented = instrument.measure(obj);
                    long guessed = guess.measure(obj);
                    // SPEC is allowed to overcount (aka: better safe than sorry)
                    if (!verify(instrumented, guessed, "Random / Guessed %d, instrumented %d for %s for %s", guessed, instrumented, clazz.description, guess))
                        failures++;
                }
                return failures;
            }));
        }
        int failures = 0;
        for (Future<Integer> result : results)
            failures += result.get();
        Assert.assertEquals("Not all guesses matched the instrumented values. See output for details. " + guess, 0, failures);
    }

    @Test
    public void testRandomArrays() throws InterruptedException, ExecutionException {
        Assert.assertTrue("MemoryMeter not initialised " + guess, MemoryMeterInstrumentation.hasInstrumentation());
        final MemoryMeter instrument = MemoryMeter.builder().withGuessing(MemoryMeter.Guess.ALWAYS_INSTRUMENTATION).build();
        final MemoryMeter guess = MemoryMeter.builder().withGuessing(this.guess).build();
        final List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0 ; i < 10000 ; i++) {
            results.add(EXEC.submit(() -> {
                Object obj = Array.newInstance(TYPES[rnd.nextInt(TYPES.length)].clazz, rnd.nextInt(1000));
                long instrumented = instrument.measure(obj);
                long guessed = guess.measure(obj);
                if (!verify(instrumented, guessed, "%s of length %d. Guessed %d, instrumented %d for %s", obj.getClass(), Array.getLength(obj), guessed, instrumented, guess))
                    return Boolean.FALSE;
                return Boolean.TRUE;
            }));
        }
        for (Future<Boolean> result : results)
            Assert.assertTrue("Failed test - see output for details " + guess, result.get());
    }

    @SuppressWarnings("deprecation")
    private boolean verify(long instrumented, long guessed, String format, Object... args) {
        if (GuessTest.this.guess == MemoryMeter.Guess.ALWAYS_SPEC) {
            long allowed = Math.max(instrumented / 10, 16);
            long min = instrumented - allowed;
            long max = instrumented + allowed;
            // SPEC is allowed to under/overcount (aka: better safe than sorry)
            if (guessed >= min && guessed <= max)
                return true;
        }
        else if (instrumented == guessed)
            return true;

        System.err.println(String.format(format, args));
        return false;
    }

    private static final Types[] TYPES = Types.values();
    private static final Random rnd = new Random();
    private static final AtomicInteger id = new AtomicInteger();
    private static final MyClassLoader CL = new MyClassLoader();
    private static File tempDir;
    private static String javac;
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    private static final ExecutorService CONSUME_PROCESS_OUTPUT = Executors.newCachedThreadPool(new DaemonThreadFactory());
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @BeforeClass
    public static void setupTempDir() throws IOException {
        tempDir = tempFolder.newFolder();

        Path javaHome = Paths.get(System.getProperty("java.home"));
        Path javac = javaHome.resolve("bin").resolve("javac");
        if (!Files.exists(javac))
            javac = javaHome.resolve("..").resolve("bin").resolve("javac");
        GuessTest.javac = javac.toString();
    }

    // declare all the primitive types
    private enum Types {
        BOOLEAN(boolean.class), BYTE(byte.class), CHAR(char.class), SHORT(short.class), INT(int.class),
        FLOAT(float.class), LONG(long.class), DOUBLE(double.class), OBJECT(Object.class);

        final Class<?> clazz;
        final String name;
        Types(Class<?> clazz) {
            this.clazz = clazz;
            this.name = clazz.getSimpleName();
        }
    }

    // a class loader for loading our random classes; permits loading arbitrary bytes to arbitrary class name
    private static final class MyClassLoader extends ClassLoader {
        Class<?> load(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }

    // a simple data-only compiled class (defined by us) and its readable description
    private static final class GeneratedClass {
        final Class<?> clazz;
        final String description;
        private GeneratedClass(Class<?> clazz, String description) {
            this.clazz = clazz;
            this.description = description;
        }
    }

    // a simple data-only class-hierarchy definition (our representation)
    private static final class Def {

        // a type and a number of occurrences
        private static final class TypeDef {
            final Types type;
            final int count;
            private TypeDef(Types type, int count) {
                this.type = type;
                this.count = count;
            }
        }

        // a single class - just a set of TypeDef
        private static final class ClassDef {
            final TypeDef[] typedefs;
            private ClassDef(TypeDef[] typedefs) {
                this.typedefs = typedefs;
            }
        }

        // the class hierarchy; classdefs[x] is an ancestor of classdefs[x+1..]
        final ClassDef[] classdefs;
        private Def(ClassDef[] classdefs) {
            this.classdefs = classdefs;
        }

        // generate a random class hierarchy
        private static Def random() {
            ClassDef[] classdefs = new ClassDef[1 + rnd.nextInt(4)];
            for (int d = 0 ; d != classdefs.length ; d++) {
                final List<TypeDef> typedefs = new ArrayList<>();
                int fieldCount = rnd.nextInt(100);
                int f = 0;
                while (f < fieldCount) {
                    Types type = TYPES[rnd.nextInt(TYPES.length)];
                    int fc = 1 + rnd.nextInt(fieldCount - f);
                    typedefs.add(new TypeDef(type, fc));
                    f += fc;

                }
                classdefs[d] = new ClassDef(typedefs.toArray(new TypeDef[0]));
            }
            return new Def(classdefs);
        }

        // parse one of our readable descriptions into a Def
        private static Def parse(String description) {
            final Pattern clazz = Pattern.compile("\\{([a-zO]+\\*[0-9]+ ?)+\\}");
            final Pattern type = Pattern.compile("([a-zO]+)\\*([0-9]+)");
            Matcher cm = clazz.matcher(description);
            List<ClassDef> classdefs = new ArrayList<>();
            while (cm.find()) {
                Matcher tm = type.matcher(cm.group());
                List<TypeDef> typedefs = new ArrayList<>();
                while (tm.find()) {
                    typedefs.add(new TypeDef(
                            Types.valueOf(tm.group(1).toUpperCase()),
                            Integer.parseInt(tm.group(2))));
                }
                classdefs.add(new ClassDef(typedefs.toArray(new TypeDef[0])));
            }
            return new Def(classdefs.toArray(new ClassDef[0]));
        }

        // transform the definition into a Java declaration, with associated files on disk
        Decl declare() throws IOException {
            String prev = null;
            List<Decl.ClassDecl> parts = new ArrayList<>();
            for (ClassDef classdef : classdefs) {
                String name = "Test" + id.incrementAndGet();
                StringBuilder decl = new StringBuilder("public class ");
                decl.append(name);
                if (prev != null) {
                    decl.append(" extends ");
                    decl.append(prev);
                }
                decl.append(" {\n");
                int field = 0;
                for (TypeDef typedef : classdef.typedefs) {
                    for (int i = 0 ; i < typedef.count ; i++) {
                        decl.append("public ");
                        decl.append(typedef.type.name);
                        decl.append(" field");
                        decl.append(field++);
                        decl.append(";\n");
                    }
                }
                decl.append("}");
                File src = new File(tempDir, name + ".java");
                if (src.exists())
                    throw new IllegalStateException();
                final FileWriter writer = new FileWriter(src);
                writer.append(decl);
                writer.close();
                File trg = new File(tempDir, name + ".class");
                parts.add(new Decl.ClassDecl(src, trg, name, decl.toString()));
                prev = name;
            }
            return new Decl(parts.toArray(new Decl.ClassDecl[0]), this);
        }

        // generate a simple description - these can be parsed by Def.parse()
        String description() {
            final StringBuilder description = new StringBuilder();
            for (ClassDef classdef : classdefs) {
                if (description.length() > 0)
                    description.append("->");
                description.append("{");
                boolean first = true;
                for (TypeDef typedef : classdef.typedefs) {
                    if (!first)
                        description.append(" ");
                    description.append(typedef.type);
                    description.append("*");
                    description.append(typedef.count);
                    first = false;
                }
                description.append("}");
            }
            return description.toString();
        }

    }

    // translate a def into a concrete declaration with source files
    private static final class Decl {
    	@SuppressWarnings("unused")
        private static final class ClassDecl {
            final File srcfile;
            final File binfile;
            final String name;
            final String declaration;
            private ClassDecl(File srcfile, File binfile, String name, String declaration) {
                this.srcfile = srcfile;
                this.binfile = binfile;
                this.name = name;
                this.declaration = declaration;
            }
        }

        final ClassDecl[] classdecls;
        final Def def;
        private Decl(ClassDecl[] classdecls, Def def) {
            this.classdecls = classdecls;
            this.def = def;
        }
    }

    // compile the provided defs by declaring them in source files and calling javac
    private static List<GeneratedClass> compile(List<Def> defs) throws IOException, ExecutionException, InterruptedException {
        final List<String> args = new ArrayList<>(Arrays.asList(javac, "-d", tempDir.getAbsolutePath()));
        final List<Decl> decls = new ArrayList<>();
        for (Def def : defs)
            decls.add(def.declare());
        for (Decl decl : decls)
            for (Decl.ClassDecl classdecl : decl.classdecls)
                args.add(classdecl.srcfile.getAbsolutePath());

        // compile
        final Process p = new ProcessBuilder(args.toArray(new String[0])).start();
        final Future<String> stdout = CONSUME_PROCESS_OUTPUT.submit(new ConsumeOutput(p.getInputStream()));
        final Future<String> stderr = CONSUME_PROCESS_OUTPUT.submit(new ConsumeOutput(p.getErrorStream()));
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            throw new IllegalStateException();
        }

        final List<GeneratedClass> generated = new ArrayList<>();
        // load
        for (Decl decl : decls) {
            Class<?> loaded = null;
            for (Decl.ClassDecl classdecl : decl.classdecls) {
                File trg = classdecl.binfile;
                if (!trg.exists()) {
                    System.out.println(stdout.get());
                    System.err.println(stderr.get());
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(trg));
                int i;
                while ( (i = in.read()) >= 0)
                    buffer.write(i);
                in.close();
                loaded = CL.load(classdecl.name, buffer.toByteArray());
            }
            generated.add(new GeneratedClass(loaded, decl.def.description()));
        }
        return generated;
    }

    // generate some random classes
    private static List<GeneratedClass> randomClasses(int count) throws IOException, ExecutionException, InterruptedException {
        // define
        final List<Def> defs = new ArrayList<>();
        while (defs.size() < count)
            defs.add(Def.random());
        return compile(defs);
    }

    // consume process output into a string
    private static final class ConsumeOutput implements Callable<String> {

        final BufferedReader in;
        final StringBuilder sb = new StringBuilder();

        private ConsumeOutput(InputStream in) {
            this.in = new BufferedReader(new InputStreamReader(in));
        }

        @Override
        public String call() {
            try {
                String line;
                while (null != (line = in.readLine())) {
                    sb.append(line);
                    sb.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    }

}
