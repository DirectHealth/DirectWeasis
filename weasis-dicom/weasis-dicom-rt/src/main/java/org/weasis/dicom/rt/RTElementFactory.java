/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.rt;

import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.DicomSpecialElement;
import org.weasis.dicom.codec.DicomSpecialElementFactory;

/**
 * @author Tomas Skripcak
 * @author Nicolas Roduit
 */
@org.osgi.service.component.annotations.Component(service = DicomSpecialElementFactory.class)
public class RTElementFactory implements DicomSpecialElementFactory {

  public static final String SERIES_RT_MIMETYPE = "rt/dicom"; // NON-NLS

  private static final String[] modalities = {"RTSTRUCT", "RTPLAN", "RTDOSE"};

  @Override
  public String getSeriesMimeType() {
    return SERIES_RT_MIMETYPE;
  }

  @Override
  public String[] getModalities() {
    return modalities;
  }

  @Override
  public DicomSpecialElement buildDicomSpecialElement(DicomMediaIO mediaIO) {
    return new RtSpecialElement(mediaIO);
  }
}
