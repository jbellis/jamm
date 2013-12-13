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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class GuessTest
{

    private static enum Types
    {
        BOOLEAN(boolean.class), BYTE(byte.class), CHAR(char.class), SHORT(short.class), INT(int.class),
        FLOAT(float.class), LONG(long.class), DOUBLE(double.class), OBJECT(Object.class);

        final Class<?> clazz;
        Types(Class<?> clazz)
        {
            this.clazz = clazz;
        }
    }

    private static final Types[] TYPES = Types.values();
    private static final Random rnd = new Random();
    private static final AtomicInteger id = new AtomicInteger();
    private static final MyClassLoader CL = new MyClassLoader();
    private static final File tempDir = new File(System.getProperty("java.io.tmpdir"), "testclasses");

    private static final class DaemonThreadFactory implements ThreadFactory
    {
        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    }

    private static final ExecutorService CONSUME_PROCESS_OUTPUT = Executors.newCachedThreadPool(new DaemonThreadFactory());
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    static
    {
        tempDir.mkdirs();
    }

    private static final class MyClassLoader extends ClassLoader
    {
        Class<?> load(String name, byte[] bytes)
        {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }

    private static final class Def
    {
        final List<DefPart> parts;
        final Class<?> clazz;
        private Def(List<DefPart> parts, Class<?> clazz)
        {
            this.parts = parts;
            this.clazz = clazz;
        }
    }

    private static final class DefPart
    {
        final File srcfile;
        final File binfile;
        final String name;
        final String declaration;
        private DefPart(File srcfile, File binfile, String name, String declaration)
        {
            this.srcfile = srcfile;
            this.binfile = binfile;
            this.name = name;
            this.declaration = declaration;
        }
    }

    // UGLY AS SIN, BUT QUICK TO WRITE AND WORKS
    private static Def generateClassDef() throws IOException, ExecutionException, InterruptedException
    {
        int superClasses = rnd.nextInt(4);
        String prev = null;
        List<DefPart> defs = new ArrayList<DefPart>();
        for (int s = 0 ; s < superClasses + 1 ; s++)
        {
            String name = "Test" + id.incrementAndGet();
            int fields = rnd.nextInt(100);
            StringBuilder decl = new StringBuilder("public class ");
            decl.append(name);
            if (prev != null)
            {
                decl.append(" extends ");
                decl.append(prev);
            }
            decl.append(" {\n");
            for (int i = 0 ; i < fields ; i++)
            {
                decl.append("public ");
                decl.append(TYPES[rnd.nextInt(TYPES.length)].clazz.getSimpleName());
                decl.append(" field");
                decl.append(i);
                decl.append(";\n");
            }
            decl.append("}");
            File src = new File(tempDir, name + ".java");
            final FileWriter writer = new FileWriter(src);
            writer.append(decl);
            writer.close();
            File trg = new File(tempDir, name + ".class");
            defs.add(new DefPart(src, trg, name, decl.toString()));
            prev = name;
        }
        final String[] args = new String[3 + defs.size()];
        args[0] = "javac";
        args[1] = "-d";
        args[2] = tempDir.getAbsolutePath();
        for (int s = 0 ; s < defs.size() ; s++)
            args[3 + s] = defs.get(s).srcfile.getAbsolutePath();
        final Process p = new ProcessBuilder(args).start();
        final Future<String> stdout = CONSUME_PROCESS_OUTPUT.submit(new ConsumeOutput(p.getInputStream()));
        final Future<String> stderr = CONSUME_PROCESS_OUTPUT.submit(new ConsumeOutput(p.getErrorStream()));
        try
        {
            p.waitFor();
        } catch (InterruptedException e)
        {
            throw new IllegalStateException();
        }
        Class<?> loaded = null;
        for (int s = 0 ; s < defs.size() ; s++)
        {
            File trg = defs.get(s).binfile;
            if (!trg.exists())
            {
                System.out.println(stdout.get());
                System.err.println(stderr.get());
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(trg));
            int i;
            while ( (i = in.read()) >= 0)
                buffer.write(i);
            loaded = CL.load(defs.get(s).name, buffer.toByteArray());
        }
        return new Def(defs, loaded);
    }

    private static final class ConsumeOutput implements Callable<String>
    {
        final BufferedReader in;
        final StringBuilder sb = new StringBuilder();

        private ConsumeOutput(InputStream in)
        {
            this.in = new BufferedReader(new InputStreamReader(in));
        }


        @Override
        public String call() throws Exception
        {
            try
            {
                String line;
                while (null != (line = in.readLine()))
                {
                    sb.append(line);
                    sb.append("\n");
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            return sb.toString();
        }
    }

    @Test
    public void testRandomClasses() throws InterruptedException, ExecutionException, IOException, IllegalAccessException, InstantiationException
    {
        final MemoryMeter mm = new MemoryMeter();
        Assert.assertTrue("MemoryMeter not initialised", mm.isInitialized());
        final List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        for (int i = 0 ; i < 1000 ; i++)
        {
            results.add(EXEC.submit(new Callable<Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    final Def def = generateClassDef();
                    Object obj = def.clazz.newInstance();
                    long instrumented = mm.measure(obj);
                    long guessed = mm.guess(obj);
                    if (instrumented != guessed)
                    {
                        System.err.println(String.format("Guessed %d, instrumented %d", guessed, instrumented));
                        Class<?> clazz = def.clazz;
                        while (clazz != null)
                        {
                            System.err.println(clazz.getName());
                            for (Field f : clazz.getDeclaredFields())
                            {
                                System.err.println(f);
                            }
                            clazz = clazz.getSuperclass();
                        }
                        for (DefPart part : def.parts)
                        {
                            System.err.println(part.declaration);
                        }
                        return false;
                    }
                    return true;
                }
            }));
        }
        for (Future<Boolean> result : results)
            Assert.assertTrue("Failed test - see output for details", result.get());
    }

    @Test
    public void testRandomArrays() throws InterruptedException, ExecutionException, IOException, IllegalAccessException, InstantiationException
    {
        final MemoryMeter mm = new MemoryMeter();
        Assert.assertTrue("MemoryMeter not initialised", mm.isInitialized());
        final List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        for (int i = 0 ; i < 100000 ; i++)
        {
            results.add(EXEC.submit(new Callable<Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    Object obj = Array.newInstance(TYPES[rnd.nextInt(TYPES.length)].clazz, rnd.nextInt(1000));
                    long instrumented = mm.measure(obj);
                    long guessed = mm.guess(obj);
                    if (instrumented != guessed)
                    {
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

}
