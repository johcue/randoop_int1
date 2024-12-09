/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.imaging.formats.jpeg.decoder;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.bytesource.ByteSource;
import org.apache.commons.imaging.test.TestResources;
import org.junit.jupiter.api.Test;

/**
 * Tests for the JpegDecoder.
 */
public class JpegDecoderTest {

    /**
     * Test that a bad file does not hang or take too long to be processed.
     */
    @Test
    public void testDecodeBadFile() {
        // From IMAGING-220
        final File inputFile = TestResources.resourceToFile("/IMAGING-220/timeout-48eb4251935b4ca8b26d1859ea525c1b42ae0c78.jpeg");
        final ByteSource byteSourceFile = ByteSource.file(inputFile);
        assertThrows(ImagingException.class, () -> new JpegDecoder().decode(byteSourceFile));
    }
}
