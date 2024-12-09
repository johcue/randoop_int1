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
package org.apache.commons.imaging.formats.tiff.constants;

import java.nio.ByteOrder;

/**
 * Defines constants for internal elements from TIFF files and for allowing applications to define parameters for reading and writing TIFF files.
 */
public final class TiffConstants {

    public static final ByteOrder DEFAULT_TIFF_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    public static final int VERSION_STANDARD = 42;
    public static final int VERSION_BIG = 43;

    public static final int HEADER_SIZE = 8;
    public static final int DIRECTORY_HEADER_LENGTH = 2;
    public static final int DIRECTORY_FOOTER_LENGTH = 4;
    public static final int ENTRY_LENGTH = 12;
    public static final int ENTRY_MAX_VALUE_LENGTH = 4;
    public static final int ENTRY_LENGTH_BIG = 12;
    public static final int ENTRY_MAX_VALUE_LENGTH_BIG = 8;

    public static final int COMPRESSION_UNCOMPRESSED_1 = 1;
    public static final int COMPRESSION_UNCOMPRESSED = COMPRESSION_UNCOMPRESSED_1;
    public static final int COMPRESSION_CCITT_1D = 2;
    public static final int COMPRESSION_CCITT_GROUP_3 = 3;
    public static final int COMPRESSION_CCITT_GROUP_4 = 4;
    public static final int COMPRESSION_LZW = 5;
    public static final int COMPRESSION_JPEG_OBSOLETE = 6;
    public static final int COMPRESSION_JPEG = 7;
    public static final int COMPRESSION_UNCOMPRESSED_2 = 32771;
    public static final int COMPRESSION_PACKBITS = 32773;
    public static final int COMPRESSION_DEFLATE_PKZIP = 32946;
    public static final int COMPRESSION_DEFLATE_ADOBE = 8;

    public static final int FLAG_T4_OPTIONS_2D = 1;
    public static final int FLAG_T4_OPTIONS_UNCOMPRESSED_MODE = 2;
    public static final int FLAG_T4_OPTIONS_FILL = 4;
    public static final int FLAG_T6_OPTIONS_UNCOMPRESSED_MODE = 2;

    /**
     * Specifies a larger strip-size to be used for compression. This setting generally produces smaller output files, but requires a slightly longer processing
     * time. Used in conjunction with the PARAM_KEY_LZW_COMPRESSION_STRIP_SIZE
     */
    public static final int LZW_COMPRESSION_BLOCK_SIZE_MEDIUM = 32768;

    /**
     * Specifies a larger strip-size to be used for compression. This setting generally produces smaller output files, but requires a slightly longer processing
     * time. Used in conjunction with the PARAM_KEY_LZW_COMPRESSION_STRIP_SIZE
     */
    public static final int LZW_COMPRESSION_BLOCK_SIZE_LARGE = 65536;

    private TiffConstants() {
    }
}
