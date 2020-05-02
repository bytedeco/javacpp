/*
 * Copyright (C) 2020 Matteo Di Giovinazzo
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HyperslabIndexTest {

    private static final FloatIndexer INDEXER_1D;
    private static final FloatIndexer INDEXER_2D;
    private static final FloatIndexer INDEXER_3D;

    static {
        float[] array = new float[12 * 10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                array[i * 10 + j] = i * 10f + j;
            }
        }
        for (int i = 11; i < 12; i++) {
            for (int j = 0; j < 10; j++) {
                array[i * 10 + j] = -1f;
            }
        }
        INDEXER_1D = FloatIndexer.create(array, HyperslabIndex.create(
                new long[]{120}, // size
                new long[]{1}, // offset
                new long[]{4}, // stride
                new long[]{2}, // count
                new long[]{3} // block           
        ));
        INDEXER_2D = FloatIndexer.create(array, HyperslabIndex.create(
                new long[]{12, 10}, // size
                new long[]{1, 1}, // offset
                new long[]{4, 3}, // stride
                new long[]{2, 3}, // count
                new long[]{3, 2} // block
        ));
        INDEXER_3D = FloatIndexer.create(new float[]{
            1, 2, 3,
            4, 5, 6,
            //
            7, 8, 9,
            10, 11, 12
        }, HyperslabIndex.create(
                new long[]{2, 2, 3}, // size
                new long[]{0, 0, 1}, // offset
                new long[]{1, 1, 1}, // stride
                new long[]{1, 1, 1}, // count
                new long[]{2, 1, 1} // block
        ));
    }

    @Test
    public void testIndexI() {
        assertEquals(1f, INDEXER_1D.get(0), 0f);
        assertEquals(2f, INDEXER_1D.get(1), 0f);
        assertEquals(3f, INDEXER_1D.get(2), 0f);
        assertEquals(5f, INDEXER_1D.get(3), 0f);
        assertEquals(6f, INDEXER_1D.get(4), 0f);
        assertEquals(7f, INDEXER_1D.get(5), 0f);
    }

    @Test
    public void testIndexIJ() {

        assertEquals(11f, INDEXER_2D.get(0, 0), 0f);
        assertEquals(21f, INDEXER_2D.get(1, 0), 0f);
        assertEquals(31f, INDEXER_2D.get(2, 0), 0f);
        assertEquals(51f, INDEXER_2D.get(3, 0), 0f);
        assertEquals(61f, INDEXER_2D.get(4, 0), 0f);
        assertEquals(71f, INDEXER_2D.get(5, 0), 0f);
        //
        assertEquals(12f, INDEXER_2D.get(0, 1), 0f);
        assertEquals(22f, INDEXER_2D.get(1, 1), 0f);
        assertEquals(32f, INDEXER_2D.get(2, 1), 0f);
        assertEquals(52f, INDEXER_2D.get(3, 1), 0f);
        assertEquals(62f, INDEXER_2D.get(4, 1), 0f);
        assertEquals(72f, INDEXER_2D.get(5, 1), 0f);
        //
        //
        assertEquals(14f, INDEXER_2D.get(0, 2), 0f);
        assertEquals(24f, INDEXER_2D.get(1, 2), 0f);
        assertEquals(34f, INDEXER_2D.get(2, 2), 0f);
        assertEquals(54f, INDEXER_2D.get(3, 2), 0f);
        assertEquals(64f, INDEXER_2D.get(4, 2), 0f);
        assertEquals(74f, INDEXER_2D.get(5, 2), 0f);
        //
        assertEquals(15f, INDEXER_2D.get(0, 3), 0f);
        assertEquals(25f, INDEXER_2D.get(1, 3), 0f);
        assertEquals(35f, INDEXER_2D.get(2, 3), 0f);
        assertEquals(55f, INDEXER_2D.get(3, 3), 0f);
        assertEquals(65f, INDEXER_2D.get(4, 3), 0f);
        assertEquals(75f, INDEXER_2D.get(5, 3), 0f);
        //
        //
        assertEquals(17f, INDEXER_2D.get(0, 4), 0f);
        assertEquals(27f, INDEXER_2D.get(1, 4), 0f);
        assertEquals(37f, INDEXER_2D.get(2, 4), 0f);
        assertEquals(57f, INDEXER_2D.get(3, 4), 0f);
        assertEquals(67f, INDEXER_2D.get(4, 4), 0f);
        assertEquals(77f, INDEXER_2D.get(5, 4), 0f);
        //
        assertEquals(18f, INDEXER_2D.get(0, 5), 0f);
        assertEquals(28f, INDEXER_2D.get(1, 5), 0f);
        assertEquals(38f, INDEXER_2D.get(2, 5), 0f);
        assertEquals(58f, INDEXER_2D.get(3, 5), 0f);
        assertEquals(68f, INDEXER_2D.get(4, 5), 0f);
        assertEquals(78f, INDEXER_2D.get(5, 5), 0f);
    }

    @Test
    public void testIndexIJK() {
        assertEquals(2, INDEXER_3D.get(0, 0, 0), 0f);
        assertEquals(5, INDEXER_3D.get(0, 1, 0), 0f);
        assertEquals(8, INDEXER_3D.get(1, 0, 0), 0f);
        assertEquals(11, INDEXER_3D.get(1, 1, 0), 0f);
    }

    @Test
    public void testIndexIndices1D() {
        assertEquals(1f, INDEXER_1D.get(new long[]{0}), 0f);
        assertEquals(2f, INDEXER_1D.get(new long[]{1}), 0f);
        assertEquals(3f, INDEXER_1D.get(new long[]{2}), 0f);
        assertEquals(5f, INDEXER_1D.get(new long[]{3}), 0f);
        assertEquals(6f, INDEXER_1D.get(new long[]{4}), 0f);
        assertEquals(7f, INDEXER_1D.get(new long[]{5}), 0f);
    }

    @Test
    public void testIndexIndices2D() {

        assertEquals(11f, INDEXER_2D.get(new long[]{0, 0}), 0f);
        assertEquals(21f, INDEXER_2D.get(new long[]{1, 0}), 0f);
        assertEquals(31f, INDEXER_2D.get(new long[]{2, 0}), 0f);
        assertEquals(51f, INDEXER_2D.get(new long[]{3, 0}), 0f);
        assertEquals(61f, INDEXER_2D.get(new long[]{4, 0}), 0f);
        assertEquals(71f, INDEXER_2D.get(new long[]{5, 0}), 0f);
        //
        assertEquals(12f, INDEXER_2D.get(new long[]{0, 1}), 0f);
        assertEquals(22f, INDEXER_2D.get(new long[]{1, 1}), 0f);
        assertEquals(32f, INDEXER_2D.get(new long[]{2, 1}), 0f);
        assertEquals(52f, INDEXER_2D.get(new long[]{3, 1}), 0f);
        assertEquals(62f, INDEXER_2D.get(new long[]{4, 1}), 0f);
        assertEquals(72f, INDEXER_2D.get(new long[]{5, 1}), 0f);
        //
        //
        assertEquals(14f, INDEXER_2D.get(new long[]{0, 2}), 0f);
        assertEquals(24f, INDEXER_2D.get(new long[]{1, 2}), 0f);
        assertEquals(34f, INDEXER_2D.get(new long[]{2, 2}), 0f);
        assertEquals(54f, INDEXER_2D.get(new long[]{3, 2}), 0f);
        assertEquals(64f, INDEXER_2D.get(new long[]{4, 2}), 0f);
        assertEquals(74f, INDEXER_2D.get(new long[]{5, 2}), 0f);
        //
        assertEquals(15f, INDEXER_2D.get(new long[]{0, 3}), 0f);
        assertEquals(25f, INDEXER_2D.get(new long[]{1, 3}), 0f);
        assertEquals(35f, INDEXER_2D.get(new long[]{2, 3}), 0f);
        assertEquals(55f, INDEXER_2D.get(new long[]{3, 3}), 0f);
        assertEquals(65f, INDEXER_2D.get(new long[]{4, 3}), 0f);
        assertEquals(75f, INDEXER_2D.get(new long[]{5, 3}), 0f);
        //
        //
        assertEquals(17f, INDEXER_2D.get(new long[]{0, 4}), 0f);
        assertEquals(27f, INDEXER_2D.get(new long[]{1, 4}), 0f);
        assertEquals(37f, INDEXER_2D.get(new long[]{2, 4}), 0f);
        assertEquals(57f, INDEXER_2D.get(new long[]{3, 4}), 0f);
        assertEquals(67f, INDEXER_2D.get(new long[]{4, 4}), 0f);
        assertEquals(77f, INDEXER_2D.get(new long[]{5, 4}), 0f);
        //
        assertEquals(18f, INDEXER_2D.get(new long[]{0, 5}), 0f);
        assertEquals(28f, INDEXER_2D.get(new long[]{1, 5}), 0f);
        assertEquals(38f, INDEXER_2D.get(new long[]{2, 5}), 0f);
        assertEquals(58f, INDEXER_2D.get(new long[]{3, 5}), 0f);
        assertEquals(68f, INDEXER_2D.get(new long[]{4, 5}), 0f);
        assertEquals(78f, INDEXER_2D.get(new long[]{5, 5}), 0f);
    }

    @Test
    public void testIndexIndices3D() {
        assertEquals(2, INDEXER_3D.get(new long[]{0, 0, 0}), 0f);
        assertEquals(5, INDEXER_3D.get(new long[]{0, 1, 0}), 0f);
        assertEquals(8, INDEXER_3D.get(new long[]{1, 0, 0}), 0f);
        assertEquals(11, INDEXER_3D.get(new long[]{1, 1, 0}), 0f);
    }
}
