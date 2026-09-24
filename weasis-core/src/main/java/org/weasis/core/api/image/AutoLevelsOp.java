/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.image;

import java.awt.image.DataBuffer;
import org.opencv.core.Core.MinMaxLocResult;
import org.weasis.core.Messages;
import org.weasis.core.util.LangUtil;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageAnalyzer;
import org.weasis.opencv.op.ImageConversion;
import org.weasis.opencv.op.ImageTransformer;

/**
 * Automatic levels adjustment operation that normalizes image intensity values. Rescales pixel
 * values to maximize contrast using the full dynamic range.
 */
public class AutoLevelsOp extends AbstractOp {

  public static final String OP_NAME = Messages.getString("AutoLevelsOp.auto_ct");

  /** Parameter key for enabling/disabling auto levels operation. */
  public static final String P_AUTO_LEVEL = "auto.level";

  private static final double MAX_BYTE_VALUE = 255.0;
  private static final double MIN_RANGE_THRESHOLD = 1.0;

  public AutoLevelsOp() {
    setName(OP_NAME);
  }

  public AutoLevelsOp(AutoLevelsOp op) {
    super(op);
  }

  @Override
  public AutoLevelsOp copy() {
    return new AutoLevelsOp(this);
  }

  @Override
  public void process() throws Exception {
    PlanarImage source = getSourceImage();
    PlanarImage result = isAutoLevelEnabled() ? applyAutoLevels(source) : source;
    params.put(Param.OUTPUT_IMG, result);
  }

  private boolean isAutoLevelEnabled() {
    return LangUtil.nullToFalse((Boolean) params.get(P_AUTO_LEVEL));
  }

  private PlanarImage applyAutoLevels(PlanarImage source) {
    MinMaxLocResult minMax = ImageAnalyzer.findMinMaxValues(source.toMat());
    double range = computeRange(minMax, source);
    double slope = MAX_BYTE_VALUE / range;
    double intercept = -slope * minMax.minVal;

    return ImageTransformer.rescaleToByte(source.toImageCV(), slope, intercept);
  }

  private double computeRange(MinMaxLocResult minMax, PlanarImage source) {
    double range = minMax.maxVal - minMax.minVal;
    return (range < MIN_RANGE_THRESHOLD && isIntegerType(source)) ? MIN_RANGE_THRESHOLD : range;
  }

  private boolean isIntegerType(PlanarImage source) {
    return ImageConversion.convertToDataType(source.type()) == DataBuffer.TYPE_INT;
  }
}
