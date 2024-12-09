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
package org.apache.commons.imaging.common;

import java.io.IOException;

import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.bytesource.ByteSource;

/**
 * Implementations support embedding the Extensible Metadata Platform tags.
 *
 * @param <E> The type of {@link XmpImagingParameters}.
 * @see <a href="https://en.wikipedia.org/wiki/Extensible_Metadata_Platform">https://en.wikipedia.org/wiki/Extensible_Metadata_Platform</a>
 */
public interface XmpEmbeddable<E extends XmpImagingParameters<E>> {

    /**
     * Gets a string containing XML conforming to the Extensible Metadata Platform (XMP) standard for representing information about image content. Not all
     * image formats support XMP information and even for those that do, there is no guarantee that such information will be present in an image.
     *
     * @param byteSource A valid reference to a ByteSource.
     * @param params     Optional instructions for special-handling or interpretation of the input data.
     * @return If XMP metadata is present, a valid string; if it is not present, a null.
     * @throws ImagingException In the event that the specified content does not conform to the format of the specific parser implementation.
     * @throws IOException      In the event of unsuccessful read or access operation.
     */
    String getXmpXml(ByteSource byteSource, XmpImagingParameters<E> params) throws ImagingException, IOException;

}
