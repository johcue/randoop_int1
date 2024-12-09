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
package org.apache.commons.imaging.formats.pnm;

import static org.apache.commons.imaging.common.BinaryFunctions.readByte;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import org.apache.commons.imaging.AbstractImageParser;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.bytesource.ByteSource;
import org.apache.commons.imaging.common.ImageBuilder;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.palette.PaletteFactory;

public class PnmImageParser extends AbstractImageParser<PnmImagingParameters> {

    private static final String TOKEN_ENDHDR = "ENDHDR";
    private static final String TOKEN_TUPLTYPE = "TUPLTYPE";
    private static final String TOKEN_MAXVAL = "MAXVAL";
    private static final String TOKEN_DEPTH = "DEPTH";
    private static final String TOKEN_HEIGHT = "HEIGHT";
    private static final String TOKEN_WIDTH = "WIDTH";

    private static final int DPI = 72;
    private static final ImageFormat[] IMAGE_FORMATS;
    private static final String DEFAULT_EXTENSION = ImageFormats.PNM.getDefaultExtension();
    private static final String[] ACCEPTED_EXTENSIONS;

    static {
        IMAGE_FORMATS = new ImageFormat[] {
                // @formatter:off
                ImageFormats.PAM,
                ImageFormats.PBM,
                ImageFormats.PGM,
                ImageFormats.PNM,
                ImageFormats.PPM
                // @formatter:on
        };
        ACCEPTED_EXTENSIONS = Stream.of(IMAGE_FORMATS).map(ImageFormat::getDefaultExtension).toArray(String[]::new);
    }

    public PnmImageParser() {
        super(ByteOrder.LITTLE_ENDIAN);
    }

    private void check(final boolean value, final String type) throws ImagingException {
        if (!value) {
            throw new ImagingException("PAM header has no " + type + " value");
        }
    }

    private void checkFound(final int value, final String type) throws ImagingException {
        check(value != -1, type);
    }

    private String checkNextTokens(final StringTokenizer tokenizer, final String type) throws ImagingException {
        check(tokenizer.hasMoreTokens(), type);
        return tokenizer.nextToken();
    }

    private int checkNextTokensAsInt(final StringTokenizer tokenizer, final String type) throws ImagingException {
        return Integer.parseInt(checkNextTokens(tokenizer, type));
    }

    @Override
    public boolean dumpImageFile(final PrintWriter pw, final ByteSource byteSource) throws ImagingException, IOException {
        pw.println("pnm.dumpImageFile");

        final ImageInfo imageData = getImageInfo(byteSource);
        if (imageData == null) {
            return false;
        }

        imageData.toString(pw, "");

        pw.println("");

        return true;
    }

    @Override
    protected String[] getAcceptedExtensions() {
        return ACCEPTED_EXTENSIONS.clone();
    }

    @Override
    protected ImageFormat[] getAcceptedTypes() {
        return IMAGE_FORMATS.clone();
    }

    @Override
    public BufferedImage getBufferedImage(final ByteSource byteSource, final PnmImagingParameters params) throws ImagingException, IOException {
        try (InputStream is = byteSource.getInputStream()) {
            final AbstractFileInfo info = readHeader(is);

            final int width = info.width;
            final int height = info.height;

            final boolean hasAlpha = info.hasAlpha();
            final ImageBuilder imageBuilder = new ImageBuilder(width, height, hasAlpha);
            info.readImage(imageBuilder, is);

            return imageBuilder.getBufferedImage();
        }
    }

    @Override
    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    @Override
    public PnmImagingParameters getDefaultParameters() {
        return new PnmImagingParameters();
    }

    @Override
    public byte[] getIccProfileBytes(final ByteSource byteSource, final PnmImagingParameters params) throws ImagingException, IOException {
        return null;
    }

    @Override
    public ImageInfo getImageInfo(final ByteSource byteSource, final PnmImagingParameters params) throws ImagingException, IOException {
        final AbstractFileInfo info = readHeader(byteSource);

        final List<String> comments = new ArrayList<>();

        final int bitsPerPixel = info.getBitDepth() * info.getNumComponents();
        final ImageFormat format = info.getImageType();
        final String formatName = info.getImageTypeDescription();
        final String mimeType = info.getMimeType();
        final int numberOfImages = 1;
        final boolean progressive = false;

        // boolean progressive = (fPNGChunkIHDR.InterlaceMethod != 0);
        //
        final int physicalWidthDpi = DPI;
        final float physicalWidthInch = (float) ((double) info.width / (double) physicalWidthDpi);
        final int physicalHeightDpi = DPI;
        final float physicalHeightInch = (float) ((double) info.height / (double) physicalHeightDpi);

        final String formatDetails = info.getImageTypeDescription();

        final boolean transparent = info.hasAlpha();
        final boolean usesPalette = false;

        final ImageInfo.ColorType colorType = info.getColorType();
        final ImageInfo.CompressionAlgorithm compressionAlgorithm = ImageInfo.CompressionAlgorithm.NONE;

        return new ImageInfo(formatDetails, bitsPerPixel, comments, format, formatName, info.height, mimeType, numberOfImages, physicalHeightDpi,
                physicalHeightInch, physicalWidthDpi, physicalWidthInch, info.width, progressive, transparent, usesPalette, colorType, compressionAlgorithm);
    }

    @Override
    public Dimension getImageSize(final ByteSource byteSource, final PnmImagingParameters params) throws ImagingException, IOException {
        final AbstractFileInfo info = readHeader(byteSource);
        return new Dimension(info.width, info.height);
    }

    @Override
    public ImageMetadata getMetadata(final ByteSource byteSource, final PnmImagingParameters params) throws ImagingException, IOException {
        return null;
    }

    @Override
    public String getName() {
        return "Pbm-Custom";
    }

    private AbstractFileInfo readHeader(final ByteSource byteSource) throws ImagingException, IOException {
        try (InputStream is = byteSource.getInputStream()) {
            return readHeader(is);
        }
    }

    private AbstractFileInfo readHeader(final InputStream inputStream) throws ImagingException, IOException {
        final byte identifier1 = readByte("Identifier1", inputStream, "Not a Valid PNM File");
        final byte identifier2 = readByte("Identifier2", inputStream, "Not a Valid PNM File");

        if (identifier1 != PnmConstants.PNM_PREFIX_BYTE) {
            throw new ImagingException("PNM file has invalid prefix byte 1");
        }

        final WhiteSpaceReader wsReader = new WhiteSpaceReader(inputStream);

        if (identifier2 == PnmConstants.PBM_TEXT_CODE || identifier2 == PnmConstants.PBM_RAW_CODE || identifier2 == PnmConstants.PGM_TEXT_CODE
                || identifier2 == PnmConstants.PGM_RAW_CODE || identifier2 == PnmConstants.PPM_TEXT_CODE || identifier2 == PnmConstants.PPM_RAW_CODE) {

            final int width;
            try {
                width = Integer.parseInt(wsReader.readtoWhiteSpace());
            } catch (final NumberFormatException e) {
                throw new ImagingException("Invalid width specified.", e);
            }
            final int height;
            try {
                height = Integer.parseInt(wsReader.readtoWhiteSpace());
            } catch (final NumberFormatException e) {
                throw new ImagingException("Invalid height specified.", e);
            }

            switch (identifier2) {
            case PnmConstants.PBM_TEXT_CODE:
                return new PbmFileInfo(width, height, false);
            case PnmConstants.PBM_RAW_CODE:
                return new PbmFileInfo(width, height, true);
            case PnmConstants.PGM_TEXT_CODE: {
                final int maxgray = Integer.parseInt(wsReader.readtoWhiteSpace());
                return new PgmFileInfo(width, height, false, maxgray);
            }
            case PnmConstants.PGM_RAW_CODE: {
                final int maxgray = Integer.parseInt(wsReader.readtoWhiteSpace());
                return new PgmFileInfo(width, height, true, maxgray);
            }
            case PnmConstants.PPM_TEXT_CODE: {
                final int max = Integer.parseInt(wsReader.readtoWhiteSpace());
                return new PpmFileInfo(width, height, false, max);
            }
            case PnmConstants.PPM_RAW_CODE: {
                final int max = Integer.parseInt(wsReader.readtoWhiteSpace());
                return new PpmFileInfo(width, height, true, max);
            }
            default:
                break;
            }
        } else if (identifier2 == PnmConstants.PAM_RAW_CODE) {
            int width = -1;
            int height = -1;
            int depth = -1;
            int maxVal = -1;
            final StringBuilder tupleType = new StringBuilder();

            // Advance to next line
            wsReader.readLine();
            String line;
            while ((line = wsReader.readLine()) != null) {
                line = line.trim();
                if (line.charAt(0) == '#') {
                    continue;
                }
                final StringTokenizer tokenizer = new StringTokenizer(line, " ", false);
                final String type = tokenizer.nextToken();
                switch (type) {
                case TOKEN_WIDTH:
                    width = checkNextTokensAsInt(tokenizer, type);
                    break;
                case TOKEN_HEIGHT:
                    height = checkNextTokensAsInt(tokenizer, type);
                    break;
                case TOKEN_DEPTH:
                    depth = checkNextTokensAsInt(tokenizer, type);
                    break;
                case TOKEN_MAXVAL:
                    maxVal = checkNextTokensAsInt(tokenizer, type);
                    break;
                case TOKEN_TUPLTYPE:
                    tupleType.append(checkNextTokens(tokenizer, type));
                    break;
                case TOKEN_ENDHDR:
                    // consumed & noop
                    break;
                default:
                    throw new ImagingException("Invalid PAM file header type " + type);
                }
                if (TOKEN_ENDHDR.equals(type)) {
                    break;
                }
            }
            checkFound(width, TOKEN_WIDTH);
            checkFound(height, TOKEN_HEIGHT);
            checkFound(depth, TOKEN_DEPTH);
            checkFound(maxVal, TOKEN_MAXVAL);
            check(tupleType.length() > 0, TOKEN_TUPLTYPE);
            return new PamFileInfo(width, height, depth, maxVal, tupleType.toString());
        }
        throw new ImagingException("PNM file has invalid prefix byte 2");
    }

    @Override
    public void writeImage(final BufferedImage src, final OutputStream os, final PnmImagingParameters params) throws ImagingException, IOException {
        PnmWriter writer = null;
        boolean useRawbits = true;

        if (params != null) {
            useRawbits = params.isRawBits();

            final ImageFormats subtype = params.getSubtype();
            if (subtype != null) {
                switch (subtype) {
                case PBM:
                    writer = new PbmWriter(useRawbits);
                    break;
                case PGM:
                    writer = new PgmWriter(useRawbits);
                    break;
                case PPM:
                    writer = new PpmWriter(useRawbits);
                    break;
                case PAM:
                    writer = new PamWriter();
                    break;
                default:
                    // see null-check below
                    break;
                }
            }
        }

        if (writer == null) {
            writer = new PaletteFactory().hasTransparency(src) ? new PamWriter() : new PpmWriter(useRawbits);
        }

        writer.writeImage(src, os, params);
    }
}
