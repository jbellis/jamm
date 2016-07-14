package org.github.jamm;

import org.junit.Test;

import static org.junit.Assert.*;

public class MemoryLayoutSpecificationTest {
    @Test
    public void getVmVersion() throws Exception {
        assertEquals(25, MemoryLayoutSpecification.getVmVersion("25.72-b15"));
        assertEquals(24, MemoryLayoutSpecification.getVmVersion("24.79-b02"));
        assertEquals(26, MemoryLayoutSpecification.getVmVersion("9-ea+126"));

    }

}