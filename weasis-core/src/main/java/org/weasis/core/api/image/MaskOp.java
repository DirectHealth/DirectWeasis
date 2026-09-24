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

import java.awt.Rectangle;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageTransformer;

/**
 * Image operation for applying masks with configurable alpha transparency.
 *
 * <p>Supports cropping images with a defined rectangular area and alpha blending. If the mask area
 * covers the entire image, no masking is applied.
 */
public class MaskOp extends AbstractOp {

  public static final String OP_NAME = "Mask"; // NON-NLS
  public static final String P_SHOW = "show"; // NON-NLS
  public static final String P_SHAPE = "shape"; // NON-NLS
  public static final String P_ALPHA = "img.alpha"; // NON-NLS

  private static final double DEFAULT_ALPHA = 0.7;
  private static final double MIN_ALPHA = 0.0;
  private static final double MAX_ALPHA = 1.0;

  public MaskOp() {
    setName(OP_NAME);
  }

  public MaskOp(MaskOp op) {
    super(op);
  }

  @Override
  public MaskOp copy() {
    return new MaskOp(this);
  }

  @Override
  public void process() throws Exception {
    PlanarImage source = getSourceImage();
    PlanarImage result = shouldApplyMask() ? processWithMask(source) : source;
    params.put(Param.OUTPUT_IMG, result);
  }

  private PlanarImage processWithMask(PlanarImage source) throws Exception {
    return getMaskArea(source).map(area -> applyMask(source, area, getAlphaValue())).orElse(source);
  }

  private boolean shouldApplyMask() {
    return params.get(P_SHOW) instanceof Boolean show && show;
  }

  private java.util.Optional<Rectangle> getMaskArea(PlanarImage source) {
    if (!(params.get(P_SHAPE) instanceof Rectangle area)) {
      return java.util.Optional.empty();
    }

    Rectangle imageBounds = new Rectangle(0, 0, source.width(), source.height());

    if (area.equals(imageBounds)) {
      return java.util.Optional.empty();
    }

    validateMaskArea(area, imageBounds);
    return java.util.Optional.of(area);
  }

  private void validateMaskArea(Rectangle maskArea, Rectangle imageBounds) {
    if (maskArea.x < 0
        || maskArea.y < 0
        || maskArea.x + maskArea.width > imageBounds.width
        || maskArea.y + maskArea.height > imageBounds.height) {
      throw new IllegalArgumentException(
          "Mask area %s is outside image bounds %s".formatted(maskArea, imageBounds));
    }
  }

  private double getAlphaValue() {
    return params.get(P_ALPHA) instanceof Number num
        ? Math.clamp(num.doubleValue(), MIN_ALPHA, MAX_ALPHA)
        : DEFAULT_ALPHA;
  }

  private PlanarImage applyMask(PlanarImage source, Rectangle area, double alpha) {
    try {
      return ImageTransformer.applyCropMask(source.toMat(), area, alpha);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to apply mask: " + e.getMessage(), e);
    }
  }
}
