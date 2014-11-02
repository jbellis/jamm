package org.github.jamm;

import org.junit.*;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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

/**
 * UGLY AS SIN, BUT QUICK TO WRITE AND WORKS.
 * We generate java source code for random data classes (with parent hierarchy), write it to a temp dir,
 * call javac on it, and instrument it.
 */
public class GuessTest {

    @Test
    public void testDeepNecessaryClasses() {
        final MemoryMeter instrument = new MemoryMeter().withTrackerProvider(TRACKER_PROVIDER);
        final MemoryMeter guess = new MemoryMeter().withGuessing(MemoryMeter.Guess.ALWAYS_SPEC).withTrackerProvider(TRACKER_PROVIDER);
        Assert.assertTrue("MemoryMeter not initialised", MemoryMeter.hasInstrumentation());
        final List<Object> objects = new ArrayList<Object>(); {
            final ConcurrentSkipListMap<Long, Long> map = new ConcurrentSkipListMap<Long, Long>();
            for (long i = 0 ; i < 100 ; i++)
                map.put(i, i);
            objects.add(map);
        }
        int failures = 0;
        for (final Object obj : objects) {
            long instrumented = instrument.measureDeep(obj);
            long guessed = guess.measureDeep(obj);
            if (instrumented != guessed) {
                System.err.println(String.format("Guessed %d, instrumented %d for %s", guessed, instrumented, obj.getClass().getName()));
                failures++;
            }
        }
        Assert.assertEquals("Not all guesses matched the instrumented values. See output for details.", 0, failures);
    }

    @Test
    public void testProblemClasses() throws InterruptedException, ExecutionException, IOException, IllegalAccessException, InstantiationException {
        final MemoryMeter instrument = new MemoryMeter();
        final MemoryMeter guess = new MemoryMeter().withGuessing(MemoryMeter.Guess.ALWAYS_UNSAFE);
        Assert.assertTrue("MemoryMeter not initialised", MemoryMeter.hasInstrumentation());
        List<Def> defs = new ArrayList<Def>();
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
            if (instrumented != guessed) {
                System.err.println(String.format("Guessed %d, instrumented %d for %s", guessed, instrumented, clazz.description));
                failures++;
            }
        }
        Assert.assertEquals("Not all guesses matched the instrumented values. See output for details.", 0, failures);
    }

    @Test
    public void testRandomClasses() throws InterruptedException, ExecutionException {
        final int testsPerCPU = 100;
        final MemoryMeter instrument = new MemoryMeter();
        final MemoryMeter guess = new MemoryMeter().withGuessing(MemoryMeter.Guess.ALWAYS_UNSAFE);
        Assert.assertTrue("MemoryMeter not initialised", MemoryMeter.hasInstrumentation());
        final List<Future<Integer>> results = new ArrayList<Future<Integer>>();
        for (int i = 0 ; i < Runtime.getRuntime().availableProcessors() ; i++) {
            results.add(EXEC.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    final List<GeneratedClass> classes = randomClasses(testsPerCPU);
                    int failures = 0;
                    for (final GeneratedClass clazz : classes) {
                        Object obj = clazz.clazz.newInstance();
                        long instrumented = instrument.measure(obj);
                        long guessed = guess.measure(obj);
                        if (instrumented != guessed) {
                            System.err.println(String.format("Guessed %d, instrumented %d for %s", guessed, instrumented, clazz.description));
                            failures++;
                        }
                    }
                    return failures;
                }
            }));
        }
        int failures = 0;
        for (Future<Integer> result : results)
            failures += result.get();
        Assert.assertEquals("Not all guesses matched the instrumented values. See output for details.", 0, failures);
    }

    @Test
    public void testRandomArrays() throws InterruptedException, ExecutionException {
        final MemoryMeter instrument = new MemoryMeter();
        final MemoryMeter guess = new MemoryMeter().withGuessing(MemoryMeter.Guess.ALWAYS_UNSAFE);
        Assert.assertTrue("MemoryMeter not initialised", MemoryMeter.hasInstrumentation());
        final List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        for (int i = 0 ; i < 10000 ; i++) {
            results.add(EXEC.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    Object obj = Array.newInstance(TYPES[rnd.nextInt(TYPES.length)].clazz, rnd.nextInt(1000));
                    long instrumented = instrument.measure(obj);
                    long guessed = guess.measure(obj);
                    if (instrumented != guessed) {
                        System.err.println(String.format("%s of length %d. Guessed %d, instrumented %d", obj.getClass(), Array.getLength(obj), guessed, instrumented));
                        return false;
                    }
                    return true;
                }
            }));
        }
        for (Future<Boolean> result : results)
            Assert.assertTrue("Failed test - see output for details", result.get());
    }

    private static final Types[] TYPES = Types.values();
    private static final Random rnd = new Random();
    private static final AtomicInteger id = new AtomicInteger();
    private static final MyClassLoader CL = new MyClassLoader();
    private static final File tempDir = new File(System.getProperty("java.io.tmpdir"), "testclasses");

    private static final Callable<Set<Object>> TRACKER_PROVIDER = new TrackerProvider();
    private static final ExecutorService CONSUME_PROCESS_OUTPUT = Executors.newCachedThreadPool(new DaemonThreadFactory());
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    static {
        // clear out the temp dir
        tempDir.mkdirs();
        for (File file : tempDir.listFiles()) {
            if (!file.delete() || file.exists())
                throw new IllegalStateException();
        }
    }

    // declare all the primitive types
    private static enum Types {
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
                final List<TypeDef> typedefs = new ArrayList<TypeDef>();
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
            List<ClassDef> classdefs = new ArrayList<ClassDef>();
            while (cm.find()) {
                Matcher tm = type.matcher(cm.group());
                List<TypeDef> typedefs = new ArrayList<TypeDef>();
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
            List<Decl.ClassDecl> parts = new ArrayList<Decl.ClassDecl>();
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
        final List<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList("javac", "-d", tempDir.getAbsolutePath()));
        final List<Decl> decls = new ArrayList<Decl>();
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

        final List<GeneratedClass> generated = new ArrayList<GeneratedClass>();
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
        final List<Def> defs = new ArrayList<Def>();
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
        public String call() throws Exception {
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

    private static final class TrackerProvider implements Callable<Set<Object>> {
        @Override
        public Set<Object> call() throws Exception {
            return new HashSet<Object>();
        }
    };

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    }

}
