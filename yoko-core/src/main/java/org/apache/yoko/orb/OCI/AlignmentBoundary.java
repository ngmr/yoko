/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.yoko.orb.OCI;


import static java.util.Objects.requireNonNull;

/**
 * Enumerate the alignment boundaries in use.
 * Each member implements its methods efficiently.
 *
 * "The boundary lines have fallen for me in pleasant places; surely I have a delightful inheritance."
 */
public enum AlignmentBoundary {
    NO_BOUNDARY {
        int gap(int index) { return 0; }
        int newIndex(int index) { return index; }
    }, TWO_BYTE_BOUNDARY {
        int gap(int index) { return index & 1; }
        int newIndex(int index) { return (index + 1) & ~1; }
    }, FOUR_BYTE_BOUNDARY {
        int gap(int index) { return -index & 3; }
        int newIndex(int index) { return (index + 3) & ~3; }
    }, EIGHT_BYTE_BOUNDARY {
        int gap(int index) { return -index & 7; }
        int newIndex(int index) { return (index + 7) & ~7; }
    };

    /**
     * Calculate the number of bytes between the supplied index and the next alignment boundary.
     */
    abstract int gap(int index);

    /**
     * Calculate the index of the next alignment boundary.
     */
    abstract int newIndex(int index);
}
