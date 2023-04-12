package org.github.jamm;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A memory listener that print to the <code>System.out</code> the class tree with the size information.
 */
final class TreePrinter implements MemoryMeterListener {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final int ONE_KB = 1024;

    private static final int ONE_MB = 1024 * ONE_KB;

    /**
     * Mapping between objects and their information
     */
    private final Map<Object, ObjectInfo> mapping = new IdentityHashMap<Object, ObjectInfo>();

    /**
     * The maximum depth of the trees to be printed
     */
    private final int maxDepth;

    /**
     * Specifies is some element will not be printed due to the depth limit.
     */
    private boolean hasMissingElements;

    /**
     * The root object
     */
    private Object root;

    public TreePrinter(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public void started(Object obj) {
        root = obj;
        mapping.put(obj, ObjectInfo.newRoot(obj.getClass()));
    }

    @Override
    public void fieldAdded(Object obj, String fieldName, Object fieldValue) {
        ObjectInfo parent = mapping.get(obj);
        if (parent != null && parent.depth <= maxDepth - 1) {
            ObjectInfo field = parent.addChild(fieldName, fieldValue.getClass());
            mapping.put(fieldValue, field);
        } else {
            hasMissingElements = true;
        }
    }

    @Override
    public void arrayElementAdded(Object[] array, int index, Object elementValue) {
        fieldAdded(array, Integer.toString(index) , elementValue);
    }

    @Override
    public void objectMeasured(Object current, long size) {
        ObjectInfo field = mapping.get(current);
        if (field != null) {
            field.size = size;
        }
    }

    @Override
    public void byteBufferRemainingMeasured(ByteBuffer buffer, long size) {
        ObjectInfo field = mapping.get(buffer);
        if (field != null) {
            field.size += size;
        }
    }

    @Override
    public void done(long size) {
        System.out.println(mapping.get(root).toString(!hasMissingElements));
    }

    @Override
    public void failedToAccessField(Object obj, String fieldName, Class<?> fieldType) {

        String fieldTypeName = ObjectInfo.className(fieldType);
        StringBuilder builder = new StringBuilder("The value of the ").append(fieldName)
                                                                      .append(" field from ")
                                                                      .append(fieldTypeName)
                                                                      .append(" could not be retrieved. Dependency stack below: ")
                                                                      .append(LINE_SEPARATOR);

        builder.append(fieldName)
               .append(" [")
               .append(fieldTypeName)
               .append("] ");

        ObjectInfo parent = mapping.get(obj);
        while (parent != null) {
            builder.append(LINE_SEPARATOR)
                   .append('|')
                   .append(LINE_SEPARATOR);
            parent.appendNameAndClassName(builder);
            parent = parent.parent;
        }

        System.err.println(builder.toString());
    }

    /**
     * Container for the information associated to a field object.
     */
    private static final class ObjectInfo {

        /**
         * The name for a root object
         */
        private static final String ROOT_NAME = "root";

        /**
         * The object name
         */
        private final String name;

        /**
         * The object class name
         */
        private final String className;

        /**
         * The object maxDepth.
         */
        private final int depth;

        /**
         * The object parent.
         */
        private final ObjectInfo parent;

        /**
         * The field children
         */
        private List<ObjectInfo> children = Collections.emptyList();

        /**
         * The object size.
         */
        private long size;

        /**
         * The total size (lazy loaded)
         */
        private long totalSize = -1;

        public ObjectInfo(ObjectInfo parent, String name, Class<?> clazz, int depth) {
            this.parent = parent;
            this.name = name;
            this.className = className(clazz);
            this.depth = depth;
        }

        /**
         * Creates a new root <code>ObjectInfo</code> for the specified class.
         *
         * @param clazz the root class
         * @return a new root <code>ObjectInfo</code>
         */
        public static ObjectInfo newRoot(Class<?> clazz) {
            return new ObjectInfo(null, ROOT_NAME, clazz, 0);
        }

        /**
         * Adds the specified child
         * @param childName the name of the child
         * @param childClass the class of the child
         * @return the child information
         */
        public ObjectInfo addChild(String childName, Class<?> childClass) {
            ObjectInfo child = new ObjectInfo(this, childName, childClass, depth + 1);
            if (children.isEmpty()) {
                children = new ArrayList<TreePrinter.ObjectInfo>();
            }
            children.add(child);
            return child;
        }

        /**
         * Returns the total size of the object and of its children
         * @return the total size of the object and of its children
         */
        public long totalSize() {

            if (totalSize < 0)
                totalSize = computeTotalSize();

            return totalSize;
        }

        /**
         * Computes the total size of the object and of its children
         * @return the total size of the object and of its children
         */
        private long computeTotalSize() {
            long total = size;
            for (ObjectInfo child : children) {
                total += child.totalSize();
            }
            return total;
        }

        @Override
        public String toString()
        {
            return toString(false);
        }

        public String toString(boolean printTotalSize) {
            return append("",
                          true,
                          printTotalSize,
                          new StringBuilder().append(LINE_SEPARATOR).append(LINE_SEPARATOR)).toString();
        }

        /**
         * Appends the representation of this <code>ObjectInfo</code> to the specified builder.
         *
         * @param indentation the indentation to use
         * @param isLast <code>true</code> if this object is the last child from is parent
         * @param printTotalSize <code>true</code> if the total size must be printed, <code>false</code> otherwise
         * @param builder the <code>StringBuilder</code> to append to
         * @return the <code>StringBuilder</code>
         */
        private StringBuilder append(String indentation,
                                     boolean isLast,
                                     boolean printTotalSize,
                                     StringBuilder builder)
        {
            if (!name.equals(ROOT_NAME)) {
                builder.append(indentation)
                       .append('|')
                       .append(LINE_SEPARATOR)
                       .append(indentation)
                       .append("+--");
            }

            appendNameAndClassName(builder);

            if (size != 0) {
                if (printTotalSize) {
                    appendSizeTo(builder, totalSize());
                    builder.append(' ');
                }
                builder.append('(');
                appendSizeTo(builder, size);
                builder.append(')');
            }
            return appendChildren(childIndentation(indentation, isLast),
                                  printTotalSize,
                                  builder.append(LINE_SEPARATOR));
        }

        /**
         * Appends the name and class name of this <code>ObjectInfo</code> to the specified builder.
         *
         * @param builder the <code>StringBuilder</code> to append to
         * @return the <code>StringBuilder</code>
         */
        public StringBuilder appendNameAndClassName(StringBuilder builder) {
            return builder.append(name)
                          .append(" [")
                          .append(className)
                          .append("] ");
        }

        /**
         * Appends to the specified <code>StringBuilder</code> the String representation of the children
         *
         * @param indentation the indentation
         * @param printTotalSize <code>true</code> if the total size must be printed, <code>false</code> otherwise
         * @param builder the builder to append to
         */
        private StringBuilder appendChildren(String indentation, boolean printTotalSize, StringBuilder builder) {
            for (int i = 0, m = children.size(); i < m; i++) {
                ObjectInfo child = children.get(i);
                boolean isLast = i == m - 1;
                child.append(indentation, isLast, printTotalSize, builder);
            }
            return builder;
        }

        /**
         * Returns the indentation to use for the children
         *
         * @param indentation the parent indentation
         * @param isLast <code>true</code> if the parent is the last child of its parent
         * @return the indentation to use for the children
         */
        private static String childIndentation(String indentation, boolean isLast) {
            return isLast ? indentation + "  " : indentation + "|  ";
        }

        /**
         * Returns the name of the specified class.
         *
         * @param clazz the class
         * @return the name of the specified class
         */
        public static String className(Class<? extends Object> clazz) {

            if (clazz.isArray())
            {
                return clazz.getComponentType().getName() + "[]";
            }
            return clazz.getName();
        }

        private static void appendSizeTo(StringBuilder builder, long size) {

            if (size >= ONE_MB) {
                builder.append(String.format("%.2f", (double) size / ONE_MB)).append(" KB");
            } else if (size >= ONE_KB) {
                builder.append(String.format("%.2f", (double) size / ONE_KB)).append(" KB");
            } else {
                builder.append(size).append(" bytes");
            }
        }
    }

    /**
     * Factory for <code>TreePrinter</code> instances.
     */
    public static class Factory implements MemoryMeterListener.Factory {

        /**
         * The maximum depth of the trees to be printed
         */
        private final int maxDepth;

        /**
         * Creates a new <code>Factory</code> instance which create <code>TreePrinter</code> that will print the 
         * visited trees up to the specified maxDepth.
         * @param maxDepth the maximum depth of the trees to be printed
         */
        public Factory(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        @Override
        public MemoryMeterListener newInstance() {
            return new TreePrinter(maxDepth);
        }
    }
}
