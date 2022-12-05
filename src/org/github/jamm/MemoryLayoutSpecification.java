package org.github.jamm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;

public abstract class MemoryLayoutSpecification
{
    public abstract int getArrayHeaderSize();

    public abstract int getObjectHeaderSize();

    public abstract int getObjectPadding();

    public abstract int getReferenceSize();

    public abstract int getSuperclassFieldPadding();

    public static MemoryLayoutSpecification getEffectiveMemoryLayoutSpecification() {

        final String dataModel = System.getProperty("sun.arch.data.model");
        if ("32".equals(dataModel)) {
            // Running with 32-bit data model
            return new MemoryLayoutSpecification() {
                public int getArrayHeaderSize() {
                    return 12;
                }

                public int getObjectHeaderSize() {
                    return 8;
                }

                public int getObjectPadding() {
                    return 8;
                }

                public int getReferenceSize() {
                    return 4;
                }

                public int getSuperclassFieldPadding() {
                    return 4;
                }
            };
        }

        boolean modernJvm = true;

        final String strSpecVersion = System.getProperty("java.specification.version");
        final boolean hasDot = strSpecVersion.indexOf('.') != -1;
        if (hasDot) {
            if ("1".equals(strSpecVersion.substring(0, strSpecVersion.indexOf('.')))) {
                // Java 1.6, 1.7, 1.8
                final String strVmVersion = System.getProperty("java.vm.version");
                if (strVmVersion.startsWith("openj9"))
                {
                    modernJvm = true;
                }
                else
                {
                    final int vmVersion = Integer.parseInt(strVmVersion.substring(0, strVmVersion.indexOf('.')));
                    modernJvm = vmVersion >= 17;
                }
            }
        }

        final int alignment = getAlignment();
        if (modernJvm) {

            long maxMemory = 0;
            for (MemoryPoolMXBean mp : ManagementFactory.getMemoryPoolMXBeans()) {
                maxMemory += mp.getUsage().getMax();
            }

            if (maxMemory < 30L * 1024 * 1024 * 1024) {
                // HotSpot 17.0 and above use compressed OOPs below 30GB of RAM
                // total for all memory pools (yes, including code cache).
                return new MemoryLayoutSpecification() {

                    public int getArrayHeaderSize() {
                        return 16;
                    }

                    public int getObjectHeaderSize() {
                        return 12;
                    }

                    public int getObjectPadding() {
                        return alignment;
                    }

                    public int getReferenceSize() {
                        return 4;
                    }

                    public int getSuperclassFieldPadding() {
                        return 4;
                    }
                };
            }
        }

        /* Worst case we over count. */

        // In other cases, it's a 64-bit uncompressed OOPs object model
        return new MemoryLayoutSpecification() {

            public int getArrayHeaderSize() {
                return 24;
            }

            public int getObjectHeaderSize() {
                return 16;
            }

            public int getObjectPadding() {
                return alignment;
            }

            public int getReferenceSize() {
                return 8;
            }

            public int getSuperclassFieldPadding() {
                return 8;
            }
        };
    }

    // check if we have a non-standard object alignment we need to round to
    private static int getAlignment() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        for (String arg : runtimeMxBean.getInputArguments()) {
            if (arg.startsWith("-XX:ObjectAlignmentInBytes=")) {
                try {
                    return Integer.parseInt(arg.substring("-XX:ObjectAlignmentInBytes=".length()));
                } catch (Exception e){}
            }
        }
        return 8;
    }


}
