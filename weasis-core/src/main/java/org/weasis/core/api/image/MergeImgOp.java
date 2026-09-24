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

import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageTransformer;

/**
 * Image operation that merges two images with configurable opacity values. If no second image or
 * opacity values are provided, returns the source image unchanged.
 */
public class MergeImgOp extends AbstractOp {

  public static final String OP_NAME = "merge.img";

  public static final String INPUT_IMG2 = "op.input.img.2";

  public static final String P_OPACITY_1 = "opacity1";
  public static final String P_OPACITY_2 = "opacity2";

  private static final double DEFAULT_OPACITY = 1.0;

  public MergeImgOp() {
    setName(OP_NAME);
  }

  public MergeImgOp(MergeImgOp op) {
    super(op);
  }

  @Override
  public MergeImgOp copy() {
    return new MergeImgOp(this);
  }

  @Override
  public void process() throws Exception {
    PlanarImage source = getSourceImage();
    PlanarImage result = mergeImages(source);
    params.put(Param.OUTPUT_IMG, result);
  }

  private PlanarImage mergeImages(PlanarImage source) {
    PlanarImage secondImage = getParam(INPUT_IMG2, PlanarImage.class);

    if (secondImage == null) {
      return source;
    }

    double opacity1 = getOpacity(P_OPACITY_1);
    double opacity2 = getOpacity(P_OPACITY_2);

    if (opacity1 == DEFAULT_OPACITY && opacity2 == DEFAULT_OPACITY) {
      return source;
    }

    return ImageTransformer.mergeImages(source.toMat(), secondImage.toMat(), opacity1, opacity2);
  }

  private double getOpacity(String paramKey) {
    return getParam(paramKey, Double.class, DEFAULT_OPACITY);
  }
}
