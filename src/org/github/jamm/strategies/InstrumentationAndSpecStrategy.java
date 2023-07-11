/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.github.jamm.strategies;

import java.lang.instrument.Instrumentation;

/**
 * Strategy that use {@code java.lang.instrument.Instrumentation} to measure non array object and the {@code Specification} approach to measure arrays.
 * This strategy tries to combine the best of both strategies the accuracy and speed of {@code Instrumentation} for non array object
 * and the speed of {@code Specification} for measuring array objects for which all strategy are accurate. For some reason {@code Instrumentation} is slower for arrays before Java 17.
 */
public class InstrumentationAndSpecStrategy extends MemoryLayoutBasedStrategy {

    private final Instrumentation instrumentation;

    public InstrumentationAndSpecStrategy(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @Override
    public long measureInstance(Object object, Class<?> type) {
        return instrumentation.getObjectSize(object);
    }
}
