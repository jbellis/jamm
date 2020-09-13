package org.github.jamm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;

abstract class MemoryLayoutSpecification
{
    abstract int getArrayHeaderSize();

    abstract int getObjectHeaderSize();

    abstract int getObjectAlignment();

    abstract int getReferenceSize();

    abstract int getSuperclassFieldPadding();

    abstract String impl();

    public String toString() {
        return "MemoryLayoutSpecification[" +
               "getArrayHeaderSize=" + getArrayHeaderSize() +
               ",getObjectHeaderSize=" + getObjectHeaderSize() +
               ",getObjectAlignment=" + getObjectAlignment() +
               ",getReferenceSize=" + getReferenceSize() +
               ",getSuperclassFieldPadding=" + getSuperclassFieldPadding() +
               ",impl=" + impl() +
               "]";
    }

    static MemoryLayoutSpecification getEffectiveMemoryLayoutSpecification() {

        final String dataModel = System.getProperty("sun.arch.data.model");
        if ("32".equals(dataModel)) {
            // Running with 32-bit data model
            return new MemoryLayoutSpecification() {
                public String impl() {
                    return "32";
                }

                public int getArrayHeaderSize() {
                    return 12;
                }

                public int getObjectHeaderSize() {
                    return 8;
                }

                public int getObjectAlignment() {
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
                    public String impl() {
                        return "modern";
                    }

                    public int getArrayHeaderSize() {
                        return 16;
                    }

                    public int getObjectHeaderSize() {
                        return 12;
                    }

                    public int getObjectAlignment() {
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
            public String impl() {
                return "64";
            }

            public int getArrayHeaderSize() {
                return 24;
            }

            public int getObjectHeaderSize() {
                return 16;
            }

            public int getObjectAlignment() {
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
                } catch (Exception ignore) {}
            }
        }
        return 8;
    }
}
