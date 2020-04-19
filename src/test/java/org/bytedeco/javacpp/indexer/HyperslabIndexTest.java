/*
 * Copyright (C) 2014-2019 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bytedeco.javacpp.indexer;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.bytedeco.javacpp.indexer.HyperslabIndex.hyperslab;
import static org.junit.Assert.assertEquals;

public class HyperslabIndexTest {

    private static float[] ARRAY;

    @BeforeClass
    public static void beforeClass() {
        ARRAY = new float[12 * 10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                ARRAY[i * 10 + j] = i * 10f + j;
            }
        }
        for (int i = 11; i < 12; i++) {
            for (int j = 0; j < 10; j++) {
                ARRAY[i * 10 + j] = -1f;
            }
        }
    }

    @Test
    public void testIndexI() {
        long[] sizes = new long[]{120};
        Index index = hyperslab(
                sizes,
                new long[]{1},
                new long[]{4},
                new long[]{2},
                new long[]{3}
        );
        FloatIndexer indexer1d = FloatIndexer.create(ARRAY, index);

        assertEquals(1f, indexer1d.get(0), 0f);
        assertEquals(2f, indexer1d.get(1), 0f);
        assertEquals(3f, indexer1d.get(2), 0f);
        assertEquals(5f, indexer1d.get(3), 0f);
        assertEquals(6f, indexer1d.get(4), 0f);
        assertEquals(7f, indexer1d.get(5), 0f);
    }

    @Test
    public void testIndexIJ() {
        long[] sizes = new long[]{12, 10};
        Index index = hyperslab(
                sizes,
                new long[]{1, 1},
                new long[]{4, 3},
                new long[]{2, 3},
                new long[]{3, 2}
        );

        FloatIndexer indexer2d = FloatIndexer.create(ARRAY, index);

        assertEquals(11f, indexer2d.get(0, 0), 0f);
        assertEquals(21f, indexer2d.get(1, 0), 0f);
        assertEquals(31f, indexer2d.get(2, 0), 0f);
        assertEquals(51f, indexer2d.get(3, 0), 0f);
        assertEquals(61f, indexer2d.get(4, 0), 0f);
        assertEquals(71f, indexer2d.get(5, 0), 0f);
        //
        assertEquals(12f, indexer2d.get(0, 1), 0f);
        assertEquals(22f, indexer2d.get(1, 1), 0f);
        assertEquals(32f, indexer2d.get(2, 1), 0f);
        assertEquals(52f, indexer2d.get(3, 1), 0f);
        assertEquals(62f, indexer2d.get(4, 1), 0f);
        assertEquals(72f, indexer2d.get(5, 1), 0f);
        //
        //
        assertEquals(14f, indexer2d.get(0, 2), 0f);
        assertEquals(24f, indexer2d.get(1, 2), 0f);
        assertEquals(34f, indexer2d.get(2, 2), 0f);
        assertEquals(54f, indexer2d.get(3, 2), 0f);
        assertEquals(64f, indexer2d.get(4, 2), 0f);
        assertEquals(74f, indexer2d.get(5, 2), 0f);
        //
        assertEquals(15f, indexer2d.get(0, 3), 0f);
        assertEquals(25f, indexer2d.get(1, 3), 0f);
        assertEquals(35f, indexer2d.get(2, 3), 0f);
        assertEquals(55f, indexer2d.get(3, 3), 0f);
        assertEquals(65f, indexer2d.get(4, 3), 0f);
        assertEquals(75f, indexer2d.get(5, 3), 0f);
        //
        //
        assertEquals(17f, indexer2d.get(0, 4), 0f);
        assertEquals(27f, indexer2d.get(1, 4), 0f);
        assertEquals(37f, indexer2d.get(2, 4), 0f);
        assertEquals(57f, indexer2d.get(3, 4), 0f);
        assertEquals(67f, indexer2d.get(4, 4), 0f);
        assertEquals(77f, indexer2d.get(5, 4), 0f);
        //
        assertEquals(18f, indexer2d.get(0, 5), 0f);
        assertEquals(28f, indexer2d.get(1, 5), 0f);
        assertEquals(38f, indexer2d.get(2, 5), 0f);
        assertEquals(58f, indexer2d.get(3, 5), 0f);
        assertEquals(68f, indexer2d.get(4, 5), 0f);
        assertEquals(78f, indexer2d.get(5, 5), 0f);
    }

    @Test
    public void testIndexIJK() {
    }

    @Test
    public void testIndexIndices() {
    }
}
