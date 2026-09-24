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

import org.weasis.core.Messages;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.image.op.ByteLutCollection;
import org.weasis.core.util.LangUtil;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageTransformer;
import org.weasis.opencv.op.lut.ByteLut;

/**
 * Operation that applies pseudo-coloring to grayscale images using lookup tables (LUTs). Supports
 * optional LUT inversion for negative color mapping.
 */
public final class PseudoColorOp extends AbstractOp {

  public static final String OP_NAME = Messages.getString("PseudoColorOperation.title");

  /** Parameter name for the LUT used in pseudo-coloring. (ByteLut, Required) */
  public static final String P_LUT = ActionW.LUT.cmd();

  /** Parameter name for inverting the LUT in pseudo-coloring. (Boolean, Optional) */
  public static final String P_LUT_INVERSE = ActionW.INVERT_LUT.cmd();

  public PseudoColorOp() {
    setName(OP_NAME);
  }

  public PseudoColorOp(PseudoColorOp op) {
    super(op);
  }

  @Override
  public PseudoColorOp copy() {
    return new PseudoColorOp(this);
  }

  @Override
  public void process() throws Exception {
    PlanarImage source = getSourceImage();
    ByteLut lutTable = (ByteLut) params.get(P_LUT);
    boolean invertLut = LangUtil.nullToFalse((Boolean) params.get(P_LUT_INVERSE));

    PlanarImage result = applyPseudoColor(source, lutTable, invertLut);
    params.put(Param.OUTPUT_IMG, result);
  }

  private PlanarImage applyPseudoColor(PlanarImage source, ByteLut lutTable, boolean invertLut) {

    if (lutTable == null) {
      return source;
    }

    byte[][] lut = lutTable.lutTable();
    if (lut == null) {
      return invertLut ? ImageTransformer.invertLUT(source.toImageCV()) : source;
    }

    byte[][] finalLut = invertLut ? ByteLutCollection.invert(lut) : lut;
    return ImageTransformer.applyLUT(source.toMat(), finalLut);
  }
}
