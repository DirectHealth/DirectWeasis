/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.viewer2d.mpr;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import javax.swing.JProgressBar;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.weasis.core.util.MathUtil;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;

public class VolumeByte extends Volume<Byte> {
  private final boolean signed;
  private byte[][][] data;

  public VolumeByte(int sizeX, int sizeY, int sizeZ, boolean signed, JProgressBar progressBar) {
    super(sizeX, sizeY, sizeZ, progressBar);
    this.signed = signed;
  }

  public VolumeByte(OriginalStack stack, boolean signed, JProgressBar progressBar) {
    super(stack, progressBar);
    this.signed = signed;
  }

  @Override
  protected void createDataArray(int sizeX, int sizeY, int sizeZ) {
    this.data = new byte[sizeX][sizeY][sizeZ];
  }

  @Override
  public void removeData() {
    this.data = null;
    super.removeData();
  }

  @Override
  protected void copyFrom(PlanarImage image, int sliceIndex, Matrix3d transform) {
    int width = image.width();
    int height = image.height();

    byte[] pixelData = new byte[width * height];
    image.get(0, 0, pixelData);

    copyPixels(
        width, height, (x, y) -> setValue(x, y, sliceIndex, pixelData[y * width + x], transform));
  }

  @Override
  protected void setValue(int x, int y, int z, Byte value, Matrix3d transform) {
    if (transform != null) {
      Vector3i sliceCoord = transformPoint(x, y, z, transform);
      x = sliceCoord.x;
      y = sliceCoord.y;
      z = sliceCoord.z;
    }
    if (isOutside(x, y, z)) {
      return;
    }
    if (data == null) {
      int index = x * size.y * size.z + y * size.z + z;
      mappedBuffer.put(index, value);
    } else {
      data[x][y][z] = value;
    }
  }

  @Override
  public Byte getValue(int x, int y, int z) {
    if (isOutside(x, y, z)) {
      return null;
    }

    if (data == null) {
      int index = x * size.y * size.z + y * size.z + z;
      return mappedBuffer.get(index);
    } else {
      return data[x][y][z];
    }
  }

  @Override
  protected double interpolate(Byte v0, Byte v1, double factor) {
    if (signed) {
      return super.interpolate(v0, v1, factor);
    }
    return (v0 == null ? 0 : Byte.toUnsignedInt(v0)) * (1 - factor)
        + (v1 == null ? 0 : Byte.toUnsignedInt(v1)) * factor;
  }

  public PlanarImage getVolumeSlice(MprAxis mprAxis, Vector3d volumeCenter) {
    if (mprAxis == null) {
      return null;
    }
    int sliceImageSize = getSliceSize();
    Vector3d voxelRatio = getVoxelRatio();
    Quaterniond mprRotation = mprAxis.getMprView().mprController.getRotation(mprAxis.getPlane());
    Matrix4d combinedTransform = mprAxis.getRealVolumeTransformation(mprRotation, volumeCenter);
    mprAxis.getTransformation().set(combinedTransform);

    byte[] raster = new byte[sliceImageSize * sliceImageSize];
    double value = getPhotometricMinValue();
    if (MathUtil.isDifferentFromZero(value)) {
      Arrays.fill(raster, (byte) value);
    }

    try (ForkJoinPool pool = new ForkJoinPool()) {
      pool.invoke(
          new VolumeSliceTask(
              0, raster.length, sliceImageSize, combinedTransform, voxelRatio, raster));
    }

    ImageCV imageCV = new ImageCV(sliceImageSize, sliceImageSize, getCVType());
    imageCV.put(0, 0, raster);
    return imageCV;
  }

  private class VolumeSliceTask extends RecursiveAction {
    private final int start;
    private final int end;
    private final int width;
    private final Matrix4d combinedTransform;
    private final Vector3d voxelRatio;
    private final byte[] raster;

    VolumeSliceTask(
        int start,
        int end,
        int width,
        Matrix4d combinedTransform,
        Vector3d voxelRatio,
        byte[] raster) {
      this.start = start;
      this.end = end;
      this.width = width;
      this.combinedTransform = combinedTransform;
      this.voxelRatio = voxelRatio;
      this.raster = raster;
    }

    @Override
    protected void compute() {
      if (end - start <= width) {
        for (int i = start; i < end; i++) {
          int x = i % width;
          int y = i / width;
          Vector3d sliceCoord = new Vector3d(x, y, 0);
          combinedTransform.transformPosition(sliceCoord);

          Double val = interpolateVolume(sliceCoord, voxelRatio);
          if (val != null) {
            raster[y * width + x] = val.byteValue();
          }
        }
      } else {
        int mid = (start + end) / 2;
        VolumeSliceTask leftTask =
            new VolumeSliceTask(start, mid, width, combinedTransform, voxelRatio, raster);
        VolumeSliceTask rightTask =
            new VolumeSliceTask(mid, end, width, combinedTransform, voxelRatio, raster);
        invokeAll(leftTask, rightTask);
      }
    }
  }
}
