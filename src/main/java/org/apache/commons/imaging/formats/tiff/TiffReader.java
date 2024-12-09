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
package org.apache.commons.imaging.formats.tiff;

import static org.apache.commons.imaging.formats.tiff.constants.TiffConstants.ENTRY_MAX_VALUE_LENGTH;
import static org.apache.commons.imaging.formats.tiff.constants.TiffConstants.ENTRY_MAX_VALUE_LENGTH_BIG;
import static org.apache.commons.imaging.formats.tiff.constants.TiffConstants.VERSION_BIG;
import static org.apache.commons.imaging.formats.tiff.constants.TiffConstants.VERSION_STANDARD;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.imaging.FormatCompliance;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.bytesource.ByteSource;
import org.apache.commons.imaging.common.BinaryFileParser;
import org.apache.commons.imaging.common.BinaryFunctions;
import org.apache.commons.imaging.common.ByteConversions;
import org.apache.commons.imaging.formats.jpeg.JpegConstants;
import org.apache.commons.imaging.formats.tiff.TiffDirectory.ImageDataElement;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.fieldtypes.AbstractFieldType;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoDirectory;

public class TiffReader extends BinaryFileParser {

    private static class Collector implements Listener {

        private TiffHeader tiffHeader;
        private final List<TiffDirectory> directories = new ArrayList<>();
        private final List<TiffField> fields = new ArrayList<>();
        private final boolean readThumbnails;

        Collector() {
            this(new TiffImagingParameters());
        }

        Collector(final TiffImagingParameters params) {
            this.readThumbnails = params.isReadThumbnails();
        }

        @Override
        public boolean addDirectory(final TiffDirectory directory) {
            directories.add(directory);
            return true;
        }

        @Override
        public boolean addField(final TiffField field) {
            fields.add(field);
            return true;
        }

        public TiffContents getContents() {
            return new TiffContents(tiffHeader, directories, fields);
        }

        @Override
        public boolean readImageData() {
            return readThumbnails;
        }

        @Override
        public boolean readOffsetDirectories() {
            return true;
        }

        @Override
        public boolean setTiffHeader(final TiffHeader tiffHeader) {
            this.tiffHeader = tiffHeader;
            return true;
        }
    }

    private static final class FirstDirectoryCollector extends Collector {
        private final boolean readImageData;

        FirstDirectoryCollector(final boolean readImageData) {
            this.readImageData = readImageData;
        }

        @Override
        public boolean addDirectory(final TiffDirectory directory) {
            super.addDirectory(directory);
            return false;
        }

        @Override
        public boolean readImageData() {
            return readImageData;
        }
    }

    public interface Listener {
        boolean addDirectory(TiffDirectory directory);

        boolean addField(TiffField field);

        boolean readImageData();

        boolean readOffsetDirectories();

        boolean setTiffHeader(TiffHeader tiffHeader);
    }

    private final boolean strict;
    private boolean bigTiff;
    private boolean standardTiff;
    private int entryMaxValueLength;

    public TiffReader(final boolean strict) {
        this.strict = strict;
    }

    private JpegImageData getJpegRawImageData(final ByteSource byteSource, final TiffDirectory directory) throws ImagingException, IOException {
        final ImageDataElement element = directory.getJpegRawImageDataElement();
        final long offset = element.offset;
        int length = element.length;
        // In case the length is not correct, adjust it and check if the last read byte actually is the end of the image
        if (offset + length > byteSource.size()) {
            length = (int) (byteSource.size() - offset);
        }
        final byte[] data = byteSource.getByteArray(offset, length);
        // check if the last read byte is actually the end of the image data
        if (strict && (length < 2 || ((data[data.length - 2] & 0xff) << 8 | data[data.length - 1] & 0xff) != JpegConstants.EOI_MARKER)) {
            throw new ImagingException("JPEG EOI marker could not be found at expected location");
        }
        return new JpegImageData(offset, length, data);
    }

    private ByteOrder getTiffByteOrder(final int byteOrderByte) throws ImagingException {
        if (byteOrderByte == 'I') {
            return ByteOrder.LITTLE_ENDIAN; // Intel
        }
        if (byteOrderByte == 'M') {
            return ByteOrder.BIG_ENDIAN; // Motorola
        }
        throw new ImagingException("Invalid TIFF byte order " + (0xff & byteOrderByte));
    }

    private AbstractTiffImageData getTiffRawImageData(final ByteSource byteSource, final TiffDirectory directory) throws ImagingException, IOException {

        final List<ImageDataElement> elements = directory.getTiffRawImageDataElements();
        final AbstractTiffImageData.Data[] data = new AbstractTiffImageData.Data[elements.size()];

        for (int i = 0; i < elements.size(); i++) {
            final TiffDirectory.ImageDataElement element = elements.get(i);
            final byte[] bytes = byteSource.getByteArray(element.offset, element.length);
            data[i] = new AbstractTiffImageData.Data(element.offset, element.length, bytes);
        }

        if (directory.imageDataInStrips()) {
            final TiffField rowsPerStripField = directory.findField(TiffTagConstants.TIFF_TAG_ROWS_PER_STRIP);
            //
            // Default value of rowsPerStripField is assumed to be infinity
            // https://www.awaresystems.be/imaging/tiff/tifftags/rowsperstrip.html
            //
            int rowsPerStrip = Integer.MAX_VALUE;

            if (null != rowsPerStripField) {
                rowsPerStrip = rowsPerStripField.getIntValue();
            } else {
                final TiffField imageHeight = directory.findField(TiffTagConstants.TIFF_TAG_IMAGE_LENGTH);
                //
                // if rows per strip not present then rowsPerStrip is equal to
                // imageLength or an infinity value;
                //
                if (imageHeight != null) {
                    rowsPerStrip = imageHeight.getIntValue();
                }

            }

            return new AbstractTiffImageData.Strips(data, rowsPerStrip);
        }
        final TiffField tileWidthField = directory.findField(TiffTagConstants.TIFF_TAG_TILE_WIDTH);
        if (null == tileWidthField) {
            throw new ImagingException("Can't find tile width field.");
        }
        final int tileWidth = tileWidthField.getIntValue();

        final TiffField tileLengthField = directory.findField(TiffTagConstants.TIFF_TAG_TILE_LENGTH);
        if (null == tileLengthField) {
            throw new ImagingException("Can't find tile length field.");
        }
        final int tileLength = tileLengthField.getIntValue();

        return new AbstractTiffImageData.Tiles(data, tileWidth, tileLength);
    }

    public void read(final ByteSource byteSource, final FormatCompliance formatCompliance, final Listener listener) throws ImagingException, IOException {
        readDirectories(byteSource, formatCompliance, listener);
    }

    public TiffContents readContents(final ByteSource byteSource, final TiffImagingParameters params, final FormatCompliance formatCompliance)
            throws ImagingException, IOException {

        final Collector collector = new Collector(params);
        read(byteSource, formatCompliance, collector);
        return collector.getContents();
    }

    public TiffContents readDirectories(final ByteSource byteSource, final boolean readImageData, final FormatCompliance formatCompliance)
            throws ImagingException, IOException {
        final TiffImagingParameters params = new TiffImagingParameters();
        params.setReadThumbnails(readImageData);
        final Collector collector = new Collector(params);
        readDirectories(byteSource, formatCompliance, collector);
        final TiffContents contents = collector.getContents();
        if (contents.directories.isEmpty()) {
            throw new ImagingException("Image did not contain any directories.");
        }
        return contents;
    }

//    NOT USED
//    private static final class DirectoryCollector extends Collector {
//        private final boolean readImageData;
//
//        public DirectoryCollector(final boolean readImageData) {
//            this.readImageData = readImageData;
//        }
//
//        @Override
//        public boolean addDirectory(final TiffDirectory directory) {
//            super.addDirectory(directory);
//            return false;
//        }
//
//        @Override
//        public boolean readImageData() {
//            return readImageData;
//        }
//    }

    private void readDirectories(final ByteSource byteSource, final FormatCompliance formatCompliance, final Listener listener)
            throws ImagingException, IOException {
        final TiffHeader tiffHeader = readTiffHeader(byteSource);
        if (!listener.setTiffHeader(tiffHeader)) {
            return;
        }

        final long offset = tiffHeader.offsetToFirstIFD;
        final int dirType = TiffDirectoryConstants.DIRECTORY_TYPE_ROOT;

        final List<Number> visited = new ArrayList<>();
        readDirectory(byteSource, offset, dirType, formatCompliance, listener, visited);
    }

    private boolean readDirectory(final ByteSource byteSource, final long directoryOffset, final int dirType, final FormatCompliance formatCompliance,
            final Listener listener, final boolean ignoreNextDirectory, final List<Number> visited) throws ImagingException, IOException {

        if (visited.contains(directoryOffset)) {
            return false;
        }
        visited.add(directoryOffset);

        try (InputStream is = byteSource.getInputStream()) {
            if (directoryOffset >= byteSource.size()) {
                return true;
            }

            BinaryFunctions.skipBytes(is, directoryOffset);

            final List<TiffField> fields = new ArrayList<>();

            long entryCount;
            try {
                if (standardTiff) {
                    entryCount = BinaryFunctions.read2Bytes("DirectoryEntryCount", is, "Not a Valid TIFF File", getByteOrder());
                } else {
                    entryCount = BinaryFunctions.read8Bytes("DirectoryEntryCount", is, "Not a Valid TIFF File", getByteOrder());
                }
            } catch (final IOException e) {
                if (strict) {
                    throw e;
                }
                return true;
            }

            for (int i = 0; i < entryCount; i++) {
                final int tag = BinaryFunctions.read2Bytes("Tag", is, "Not a Valid TIFF File", getByteOrder());
                final int type = BinaryFunctions.read2Bytes("Type", is, "Not a Valid TIFF File", getByteOrder());
                final long count;
                final byte[] offsetBytes;
                final long offset;
                if (standardTiff) {
                    count = 0xFFFFffffL & BinaryFunctions.read4Bytes("Count", is, "Not a Valid TIFF File", getByteOrder());
                    offsetBytes = BinaryFunctions.readBytes("Offset", is, 4, "Not a Valid TIFF File");
                    offset = 0xFFFFffffL & ByteConversions.toInt(offsetBytes, getByteOrder());
                } else {
                    count = BinaryFunctions.read8Bytes("Count", is, "Not a Valid TIFF File", getByteOrder());
                    offsetBytes = BinaryFunctions.readBytes("Offset", is, 8, "Not a Valid TIFF File");
                    offset = ByteConversions.toLong(offsetBytes, getByteOrder());
                }

                if (tag == 0) {
                    // skip invalid fields.
                    // These are seen very rarely, but can have invalid value
                    // lengths,
                    // which can cause OOM problems.
                    continue;
                }

                final AbstractFieldType abstractFieldType;
                try {
                    abstractFieldType = AbstractFieldType.getFieldType(type);
                } catch (final ImagingException imageReadEx) {
                    // skip over unknown fields types, since we
                    // can't calculate their size without
                    // knowing their type
                    continue;
                }
                final long valueLength = count * abstractFieldType.getSize();
                final byte[] value;
                if (valueLength > entryMaxValueLength) {
                    if (offset < 0 || offset + valueLength > byteSource.size()) {
                        if (strict) {
                            throw new IOException("Attempt to read byte range starting from " + offset + " " + "of length " + valueLength + " "
                                    + "which is outside the file's size of " + byteSource.size());
                        }
                        // corrupt field, ignore it
                        continue;
                    }
                    value = byteSource.getByteArray(offset, (int) valueLength);
                } else {
                    value = offsetBytes;
                }

                final TiffField field = new TiffField(tag, dirType, abstractFieldType, count, offset, value, getByteOrder(), i);

                fields.add(field);

                if (!listener.addField(field)) {
                    return true;
                }
            }

            final long nextDirectoryOffset = 0xFFFFffffL & BinaryFunctions.read4Bytes("nextDirectoryOffset", is, "Not a Valid TIFF File", getByteOrder());

            final TiffDirectory directory = new TiffDirectory(dirType, fields, directoryOffset, nextDirectoryOffset, getByteOrder());

            if (listener.readImageData()) {
                if (directory.hasTiffImageData()) {
                    final AbstractTiffImageData rawImageData = getTiffRawImageData(byteSource, directory);
                    directory.setTiffImageData(rawImageData);
                }
                if (directory.hasJpegImageData()) {
                    final JpegImageData rawJpegImageData = getJpegRawImageData(byteSource, directory);
                    directory.setJpegImageData(rawJpegImageData);
                }
            }

            if (!listener.addDirectory(directory)) {
                return true;
            }

            if (listener.readOffsetDirectories()) {
                final TagInfoDirectory[] offsetFields = { ExifTagConstants.EXIF_TAG_EXIF_OFFSET, ExifTagConstants.EXIF_TAG_GPSINFO,
                        ExifTagConstants.EXIF_TAG_INTEROP_OFFSET };
                final int[] directoryTypes = { TiffDirectoryConstants.DIRECTORY_TYPE_EXIF, TiffDirectoryConstants.DIRECTORY_TYPE_GPS,
                        TiffDirectoryConstants.DIRECTORY_TYPE_INTEROPERABILITY };
                for (int i = 0; i < offsetFields.length; i++) {
                    final TagInfoDirectory offsetField = offsetFields[i];
                    final TiffField field = directory.findField(offsetField);
                    if (field != null) {
                        long subDirectoryOffset;
                        int subDirectoryType;
                        boolean subDirectoryRead = false;
                        try {
                            subDirectoryOffset = directory.getFieldValue(offsetField);
                            subDirectoryType = directoryTypes[i];
                            subDirectoryRead = readDirectory(byteSource, subDirectoryOffset, subDirectoryType, formatCompliance, listener, true, visited);

                        } catch (final ImagingException imageReadException) {
                            if (strict) {
                                throw imageReadException;
                            }
                        }
                        if (!subDirectoryRead) {
                            fields.remove(field);
                        }
                    }
                }
            }

            if (!ignoreNextDirectory && directory.getNextDirectoryOffset() > 0) {
                // Debug.debug("next dir", directory.nextDirectoryOffset );
                readDirectory(byteSource, directory.getNextDirectoryOffset(), dirType + 1, formatCompliance, listener, visited);
            }

            return true;
        }
    }

    private boolean readDirectory(final ByteSource byteSource, final long offset, final int dirType, final FormatCompliance formatCompliance,
            final Listener listener, final List<Number> visited) throws ImagingException, IOException {
        final boolean ignoreNextDirectory = false;
        return readDirectory(byteSource, offset, dirType, formatCompliance, listener, ignoreNextDirectory, visited);
    }

    public TiffContents readFirstDirectory(final ByteSource byteSource, final boolean readImageData, final FormatCompliance formatCompliance)
            throws ImagingException, IOException {
        final Collector collector = new FirstDirectoryCollector(readImageData);
        read(byteSource, formatCompliance, collector);
        final TiffContents contents = collector.getContents();
        if (contents.directories.isEmpty()) {
            throw new ImagingException("Image did not contain any directories.");
        }
        return contents;
    }

    private TiffHeader readTiffHeader(final ByteSource byteSource) throws ImagingException, IOException {
        try (InputStream is = byteSource.getInputStream()) {
            return readTiffHeader(is);
        }
    }

    private TiffHeader readTiffHeader(final InputStream is) throws ImagingException, IOException {
        final int byteOrder1 = BinaryFunctions.readByte("BYTE_ORDER_1", is, "Not a Valid TIFF File");
        final int byteOrder2 = BinaryFunctions.readByte("BYTE_ORDER_2", is, "Not a Valid TIFF File");
        if (byteOrder1 != byteOrder2) {
            throw new ImagingException("Byte Order bytes don't match (" + byteOrder1 + ", " + byteOrder2 + ").");
        }

        final ByteOrder byteOrder = getTiffByteOrder(byteOrder1);
        setByteOrder(byteOrder);

        // verify that the file is a supported TIFF format using
        // the numeric indentifier
        // Classic TIFF (32 bit): 42
        // Big TIFF (64 bit): 43
        //
        final long offsetToFirstIFD;
        final int tiffVersion = BinaryFunctions.read2Bytes("tiffVersion", is, "Not a Valid TIFF File", getByteOrder());
        if (tiffVersion == VERSION_STANDARD) {
            bigTiff = false;
            standardTiff = true;
            entryMaxValueLength = ENTRY_MAX_VALUE_LENGTH;
            offsetToFirstIFD = 0xFFFFffffL & BinaryFunctions.read4Bytes("offsetToFirstIFD", is, "Not a Valid TIFF File", getByteOrder());
        } else if (tiffVersion == VERSION_BIG) {
            bigTiff = true;
            standardTiff = false;
            entryMaxValueLength = ENTRY_MAX_VALUE_LENGTH_BIG;
            final int byteSize = BinaryFunctions.read2Bytes("bytesizeOfOffset", is, "Not a Valid TIFF File", getByteOrder());
            final int expectedZero = BinaryFunctions.read2Bytes("expectedZero", is, "Not a Valid TIFF File", getByteOrder());
            if (byteSize != 8 || expectedZero != 0) {
                throw new ImagingException("Misformed Big-TIFF header: " + tiffVersion);
            }
            offsetToFirstIFD = BinaryFunctions.read8Bytes("offsetToFirstIFD", is, "Not a Valid TIFF File", getByteOrder());
        } else {
            throw new ImagingException("Unknown TIFF Version: " + tiffVersion);
        }

        BinaryFunctions.skipBytes(is, offsetToFirstIFD - 8, "Not a Valid TIFF File: couldn't find IFDs");

        return new TiffHeader(byteOrder, tiffVersion, offsetToFirstIFD, bigTiff);
    }
}
