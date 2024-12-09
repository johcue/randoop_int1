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
package org.apache.commons.imaging.formats.jpeg.iptc;

import java.util.Objects;

/**
 * Represents an IPTC block, a set of key-value pairs of Photoshop IPTC data.
 * <p>
 * The class is used in public API parameter types.
 * </p>
 */
public class IptcBlock {

    private final int blockType;
    private final byte[] blockNameBytes;
    private final byte[] blockData;

    public IptcBlock(final int blockType, final byte[] blockNameBytes, final byte[] blockData) {
        this.blockData = Objects.requireNonNull(blockData, "blockData");
        this.blockNameBytes = Objects.requireNonNull(blockNameBytes, "blockNameBytes");
        this.blockType = blockType;
    }

    public byte[] getBlockData() {
        return blockData.clone();
    }

    public byte[] getBlockNameBytes() {
        return blockNameBytes.clone();
    }

    public int getBlockType() {
        return blockType;
    }

    public boolean isIptcBlock() {
        return blockType == IptcConstants.IMAGE_RESOURCE_BLOCK_IPTC_DATA;
    }
}
