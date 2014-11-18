package org.github.jamm;

import java.util.ArrayList;
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
     * The factory for <code>TreePrinter</code> instances.
     */
    public static MemoryMeterListener.Factory FACTORY = new MemoryMeterListener.Factory() {

        @Override
        public MemoryMeterListener newInstance() {
            return new TreePrinter();
        }
    };

    /**
     * Mapping between objects and their information
     */
    private Map<Object, ObjectInfo> mapping = new IdentityHashMap<Object, ObjectInfo>();

    /**
     * The root object
     */
    private Object root;

    @Override
    public void started(Object obj) {
        root = obj;
        mapping.put(obj, ObjectInfo.newRoot(obj.getClass()));
    }

    @Override
    public void fieldAdded(Object obj, String fieldName, Object fieldValue) {
        ObjectInfo field = new ObjectInfo(fieldName, fieldValue.getClass());
        ObjectInfo parent = mapping.get(obj);
        parent.addChild(field);

        mapping.put(fieldValue, field);
    }

    @Override
    public void objectMeasured(Object current, long size) {
        ObjectInfo field = mapping.get(current);
        field.size = size;
    }

    @Override
    public void objectCounted(Object current) {
    }

    @Override
    public void done(long size) {
        System.out.println(mapping.get(root).toString());
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
         * The field children
         */
        private final List<ObjectInfo> children = new ArrayList<ObjectInfo>();

        /**
         * The object size.
         */
        private long size;

        /**
         * The total size (lazy loaded)
         */
        private long totalSize = -1;

        public ObjectInfo(String name, Class<?> clazz) {
            this.name = name;
            this.className = className(clazz);
        }

        /**
         * Creates a new root <code>ObjectInfo</code> for the specified class.
         *
         * @param clazz the root class
         * @return a new root <code>ObjectInfo</code>
         */
        public static ObjectInfo newRoot(Class<?> clazz) {
            return new ObjectInfo(ROOT_NAME, clazz);
        }

        /**
         * Adds the specified child
         * @param child the child to add
         */
        public void addChild(ObjectInfo child) {
            children.add(child);
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
            return append("", true, new StringBuilder().append(LINE_SEPARATOR).append(LINE_SEPARATOR)).toString();
        }

        /**
         * Appends the representation of this <code>ObjectInfo</code> to the specified builder.
         *
         * @param indentation the indentation to use
         * @param isLast <code>true</code> if this object is the last child from is parent
         * @param builder the <code>StringBuilder</code> to append to
         * @return the <code>StringBuilder</code>
         */
        private StringBuilder append(String indentation, boolean isLast, StringBuilder builder)
        {
            if (!name.equals(ROOT_NAME)) {
                builder.append(indentation)
                       .append('|')
                       .append(LINE_SEPARATOR)
                       .append(indentation)
                       .append("+--");
            }

            builder.append(name)
                   .append(" [")
                   .append(className)
                   .append("] ");

            if (size != 0) {
                appendSizeTo(builder, totalSize());
                builder.append(" (");
                appendSizeTo(builder, size);
                builder.append(")");
            }
            return appendChildren(childIntentation(indentation, isLast), builder.append(LINE_SEPARATOR));
        }

        /**
         * Appends to the specified <code>StringBuilder</code> the String representation of the children
         *
         * @param indentation the indentation
         * @param builder the builder to append to
         */
        private StringBuilder appendChildren(String indentation, StringBuilder builder) {
            for (int i = 0, m = children.size(); i < m; i++) {
                ObjectInfo child = children.get(i);
                boolean isLast = i == m - 1;
                child.append(indentation, isLast, builder);
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
        private static String childIntentation(String indentation, boolean isLast) {
            return isLast ? indentation + "  " : indentation + "|  ";
        }

        /**
         * Returns the name of the specified class.
         *
         * @param clazz the class
         * @return the name of the specified class
         */
        private static String className(Class<? extends Object> clazz) {

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
}
