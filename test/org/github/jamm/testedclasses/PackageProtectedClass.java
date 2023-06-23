package org.github.jamm.testedclasses;

import org.github.jamm.MemoryMeter;

/**
 * Package protected class used by tests
 *
 */
class PackageProtectedClass
{
    public String publicField;

    String packageProtectedField;

    protected String protectedField;

    private String privateField;

    public PackageProtectedClass(String publicField,
                                 String packageProtectedField,
                                 String protectedField,
                                 String privateField) {

        this.publicField = publicField;
        this.packageProtectedField = packageProtectedField;
        this.protectedField = protectedField;
        this.privateField = privateField;
    }

    public long measureDeep(MemoryMeter meter) {
        return meter.measure(this) 
                + meter.measureDeep(publicField)
                + meter.measureDeep(packageProtectedField)
                + meter.measureDeep(protectedField)
                + meter.measureDeep(privateField);
    }
}
