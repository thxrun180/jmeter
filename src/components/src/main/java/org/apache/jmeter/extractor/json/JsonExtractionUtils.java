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

package org.apache.jmeter.extractor.json;

/**
 * Shared utilities for JSON extractors.
 */
public final class JsonExtractionUtils {

    private JsonExtractionUtils() {
        // Utility class
    }

    /**
     * Ensures a consistent null-handling policy across extractors.
     *
     * @param rawValue the extracted value (possibly null)
     * @param defaultValue the configured default value
     * @return {@code defaultValue} when {@code rawValue} is {@code null}, otherwise {@code rawValue.toString()}
     */
    public static String finalizeValue(Object rawValue, String defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }
        return rawValue.toString();
    }
}
