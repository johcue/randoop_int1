/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.apache.commons.imaging.formats.jpeg.decoder;

final class YCbCrConverter {
    private static final int[] REDS = new int[256 * 256];
    private static final int[] BLUES = new int[256 * 256];
    private static final int[] GREENS1 = new int[256 * 256];
    private static final int[] GREENS2 = new int[256 * 512];

    static {
        /*
         * Why use (Cr << 8) | Y and not (Y << 8) | Cr as the index? Y changes often, while Cb and Cr is usually subsampled less often and repeats itself
         * between adjacent pixels, so using it as the high order byte gives higher locality of reference.
         */
        for (int y = 0; y < 256; y++) {
            for (int cr = 0; cr < 256; cr++) {
                int r = y + fastRound(1.402f * (cr - 128));
                if (r < 0) {
                    r = 0;
                }
                if (r > 255) {
                    r = 255;
                }
                REDS[cr << 8 | y] = r << 16;
            }
        }
        for (int y = 0; y < 256; y++) {
            for (int cb = 0; cb < 256; cb++) {
                int b = y + fastRound(1.772f * (cb - 128));
                if (b < 0) {
                    b = 0;
                }
                if (b > 255) {
                    b = 255;
                }
                BLUES[cb << 8 | y] = b;
            }
        }
        // green is the hardest
        // Math.round((float) (Y - 0.34414*(Cb-128) - 0.71414*(Cr-128)))
        // but Y is integral
        // = Y - Math.round((float) (0.34414*(Cb-128) + 0.71414*(Cr-128)))
        // = Y - Math.round(f(Cb, Cr))
        // where
        // f(Cb, Cr) = 0.34414*(Cb-128) + 0.71414*(Cr-128)
        // Cb and Cr terms each vary from 255-128 = 127 to 0-128 = -128
        // Linear function, so only examine endpoints:
        // Cb term Cr term Result
        // 127 127 134.4
        // -128 -128 -135.4
        // 127 -128 -47.7
        // -128 127 46.6
        // Thus with -135 being the minimum and 134 the maximum,
        // there is a range of 269 values,
        // and 135 needs to be added to make it zero-based.

        // As for Y - f(Cb, Cr)
        // the range becomes:
        // Y f(Cb, Cr)
        // 255 -135
        // 255 134
        // 0 -135
        // 0 134
        // thus the range is [-134,390] and has 524 values
        // but is clamped to [0, 255]
        for (int cb = 0; cb < 256; cb++) {
            for (int cr = 0; cr < 256; cr++) {
                final int value = fastRound(0.34414f * (cb - 128) + 0.71414f * (cr - 128));
                GREENS1[cb << 8 | cr] = value + 135;
            }
        }
        for (int y = 0; y < 256; y++) {
            for (int value = 0; value < 270; value++) {
                int green = y - (value - 135);
                if (green < 0) {
                    green = 0;
                } else if (green > 255) {
                    green = 255;
                }
                GREENS2[value << 8 | y] = green << 8;
            }
        }
    }

    public static int convertYCbCrToRgb(final int y, final int cb, final int cr) {
        final int r = REDS[cr << 8 | y];
        final int g1 = GREENS1[cb << 8 | cr];
        final int g = GREENS2[g1 << 8 | y];
        final int b = BLUES[cb << 8 | y];
        return r | g | b;
    }

    private static int fastRound(final float x) {
        // Math.round() is very slow
        return (int) (x + 0.5f);
    }

    private YCbCrConverter() {
    }
}
