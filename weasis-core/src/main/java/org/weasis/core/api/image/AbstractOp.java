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

import java.util.HashMap;
import java.util.Map;
import org.weasis.core.util.LangUtil;
import org.weasis.opencv.data.PlanarImage;

/**
 * Base implementation for image operations providing parameter management. Thread-safe
 * implementation using ConcurrentHashMap.
 */
public abstract class AbstractOp implements ImageOpNode {

  protected static final String INPUT_PREFIX = "op.input";
  protected static final String OUTPUT_PREFIX = "op.output";

  protected final Map<String, Object> params;

  protected AbstractOp() {
    this.params = new HashMap<>();
  }

  protected AbstractOp(AbstractOp op) {
    this.params = new HashMap<>(op.params);
    clearIOCache();
  }

  @Override
  public void clearParams() {
    params.clear();
  }

  @Override
  public void clearIOCache() {
    params.keySet().removeIf(this::isIOCacheKey);
  }

  private boolean isIOCacheKey(String key) {
    return key.startsWith(INPUT_PREFIX) || key.startsWith(OUTPUT_PREFIX);
  }

  @Override
  public Object getParam(String key) {
    return params.get(key);
  }

  @Override
  public void setParam(String key, Object value) {
    if (key != null) {
      params.put(key, value);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getParam(String key, Class<T> ignored) {
    return (T) params.get(key);
  }

  protected <T> T getParam(String key, Class<T> type, T defaultValue) {
    T value = getParam(key, type);
    return value != null ? value : defaultValue;
  }

  @Override
  public void setAllParameters(Map<String, Object> map) {
    if (map != null) {
      params.putAll(map);
    }
  }

  @Override
  public void removeParam(String key) {
    params.remove(key);
  }

  @Override
  public boolean isEnabled() {
    return LangUtil.nullToTrue((Boolean) params.get(Param.ENABLE));
  }

  @Override
  public void setEnabled(boolean enabled) {
    params.put(Param.ENABLE, enabled);
  }

  @Override
  public String getName() {
    return (String) params.get(Param.NAME);
  }

  @Override
  public void setName(String name) {
    if (name != null) {
      params.put(Param.NAME, name);
    }
  }

  @Override
  public void handleImageOpEvent(ImageOpEvent event) {
    // Default implementation does nothing
  }

  /**
   * Retrieves the source image from parameters.
   *
   * @return the source PlanarImage
   * @throws IllegalArgumentException if INPUT_IMG is not a PlanarImage
   */
  protected PlanarImage getSourceImage() {
    return switch (params.get(Param.INPUT_IMG)) {
      case PlanarImage img -> img;
      case null -> throw new IllegalArgumentException("INPUT_IMG parameter is missing");
      default -> throw new IllegalArgumentException("INPUT_IMG parameter must be a PlanarImage");
    };
  }
}
