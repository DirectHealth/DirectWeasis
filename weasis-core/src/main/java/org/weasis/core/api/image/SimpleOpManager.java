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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.Messages;
import org.weasis.core.api.image.ImageOpNode.Param;
import org.weasis.opencv.data.PlanarImage;

/**
 * Simple implementation of OpManager that manages a pipeline of image operations.
 *
 * <p>This class maintains operations in both a list (for ordered processing) and a map (for
 * name-based lookup). Operations can be added, removed, and processed in sequence.
 *
 * @see OpManager
 * @see ImageOpNode
 */
public class SimpleOpManager implements OpManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOpManager.class);

  private static final String IMAGE_OP_NAME = Messages.getString("SimpleOpManager.img_op");

  /** Position for inserting operations relative to a reference node. */
  public enum Position {
    /** Insert before the reference node. */
    BEFORE,
    /** Insert after the reference node. */
    AFTER
  }

  private final Map<String, ImageOpNode> nodesByName;
  private final List<ImageOpNode> operations;
  private volatile String name;

  /** Creates a new SimpleOpManager with default name. */
  public SimpleOpManager() {
    this(IMAGE_OP_NAME);
  }

  /**
   * Creates a new SimpleOpManager with the specified name.
   *
   * @param name the manager name
   */
  public SimpleOpManager(String name) {
    this.operations = new ArrayList<>();
    this.nodesByName = new HashMap<>();
    setName(name);
  }

  /**
   * Creates a deep copy of another SimpleOpManager.
   *
   * @param other the manager to copy
   */
  public SimpleOpManager(SimpleOpManager other) {
    this(other.name);
    other.operations.forEach(
        node -> {
          var copiedNode = node.copy();
          operations.add(copiedNode);
          nodesByName.put(copiedNode.getName(), copiedNode);
        });
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = Objects.requireNonNullElse(name, IMAGE_OP_NAME);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public List<ImageOpNode> getOperations() {
    return List.copyOf(operations);
  }

  /**
   * Adds an image operation at the end of the pipeline.
   *
   * @param action the operation to add
   */
  public void addImageOperationAction(ImageOpNode action) {
    addImageOperationAction(action, null, null);
  }

  /**
   * Adds an image operation at the specified position relative to a reference node.
   *
   * @param action the operation to add
   * @param pos the position (BEFORE or AFTER)
   * @param positionRef the reference node, or null to add at the end
   */
  public void addImageOperationAction(ImageOpNode action, Position pos, ImageOpNode positionRef) {
    if (action == null) {
      return;
    }
    var uniqueName = ensureUniqueName(action.getName());
    action.setName(uniqueName);
    nodesByName.put(uniqueName, action);

    var index = calculateInsertIndex(pos, positionRef);
    if (index >= 0 && index < operations.size()) {
      operations.add(index, action);
    } else {
      operations.add(action);
    }
  }

  private String ensureUniqueName(String baseName) {
    if (!nodesByName.containsKey(baseName)) {
      return baseName;
    }

    LOGGER.warn("Operation name '{}' already exists, generating unique name", baseName);
    return baseName + " " + findNextAvailableSuffix(baseName);
  }

  private int findNextAvailableSuffix(String baseName) {
    return java.util.stream.IntStream.iterate(2, i -> i + 1)
        .filter(i -> !nodesByName.containsKey("%s %d".formatted(baseName, i)))
        .findFirst()
        .orElseThrow();
  }

  private int calculateInsertIndex(Position pos, ImageOpNode positionRef) {
    if (positionRef == null) {
      return -1;
    }

    var index = operations.indexOf(positionRef);
    return index < 0 ? -1 : (pos == Position.AFTER ? index + 1 : index);
  }

  /**
   * Removes an image operation from the pipeline.
   *
   * @param action the operation to remove
   */
  public void removeImageOperationAction(ImageOpNode action) {
    if (action == null) {
      return;
    }
    operations.remove(action);

    nodesByName.values().removeIf(node -> node == action);
  }

  @Override
  public void removeAllImageOperationAction() {
    clearNodeParams();
    operations.clear();
    nodesByName.clear();
  }

  @Override
  public void clearNodeParams() {
    operations.forEach(ImageOpNode::clearParams);
  }

  @Override
  public void clearNodeIOCache() {
    operations.forEach(ImageOpNode::clearIOCache);
  }

  @Override
  public void setFirstNode(PlanarImage imgSource) {
    getFirstNode().ifPresent(node -> node.setParam(Param.INPUT_IMG, imgSource));
  }

  @Override
  public Optional<PlanarImage> getFirstNodeInputImage() {
    return getFirstNode().map(node -> (PlanarImage) node.getParam(Param.INPUT_IMG));
  }

  @Override
  public Optional<ImageOpNode> getFirstNode() {
    return operations.isEmpty() ? Optional.empty() : Optional.of(operations.getFirst());
  }

  @Override
  public Optional<ImageOpNode> getNode(String opName) {
    return Optional.ofNullable(nodesByName.get(opName));
  }

  @Override
  public Optional<ImageOpNode> getLastNode() {
    return operations.isEmpty() ? Optional.empty() : Optional.of(operations.getLast());
  }

  @Override
  public Optional<PlanarImage> getLastNodeOutputImage() {
    return getLastNode().map(node -> (PlanarImage) node.getParam(Param.OUTPUT_IMG));
  }

  /** Resets the output image of the last node to clear preprocessing cache. */
  public void resetLastNodeOutputImage() {
    getLastNode().ifPresent(node -> node.setParam(Param.OUTPUT_IMG, null));
  }

  @Override
  public Optional<PlanarImage> process() {
    var source = getFirstNodeInputImage().orElse(null);
    if (source == null || source.width() <= 0) {
      clearNodeIOCache();
      return getLastNodeOutputImage();
    }

    for (var i = 0; i < operations.size(); i++) {
      var current = operations.get(i);
      if (i > 0) {
        var previous = operations.get(i - 1);
        current.setParam(Param.INPUT_IMG, previous.getParam(Param.OUTPUT_IMG));
      }
      executeOperation(current);
    }
    return getLastNodeOutputImage();
  }

  private void executeOperation(ImageOpNode operation) {
    try {
      if (operation.isEnabled()) {
        operation.process();
      } else {
        operation.setParam(Param.OUTPUT_IMG, operation.getParam(Param.INPUT_IMG));
      }
    } catch (Exception e) {
      LOGGER.error("Operation '{}' failed", operation.getParam(Param.NAME), e);
      operation.setParam(Param.OUTPUT_IMG, operation.getParam(Param.INPUT_IMG));
    }
  }

  @Override
  public Optional<Object> getParamValue(String opName, String param) {
    return getNode(opName).map(node -> node.getParam(param));
  }

  @Override
  public <T> Optional<T> getParamValue(String opName, String param, Class<T> ignored) {
    return getNode(opName).map(node -> node.getParam(param, ignored));
  }

  @Override
  public boolean setParamValue(String opName, String param, Object value) {
    if (opName == null || param == null) {
      return false;
    }
    return getNode(opName)
        .map(
            node -> {
              node.setParam(param, value);
              return true;
            })
        .orElse(false);
  }

  @Override
  public void removeParam(String opName, String param) {
    if (opName != null && param != null) {
      getNode(opName).ifPresent(node -> node.removeParam(param));
    }
  }

  @Override
  public void handleImageOpEvent(ImageOpEvent event) {
    operations.forEach(node -> node.handleImageOpEvent(event));
  }

  @Override
  public SimpleOpManager copy() {
    return new SimpleOpManager(this);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SimpleOpManager other
        && Objects.equals(name, other.name)
        && Objects.equals(operations, other.operations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, operations);
  }
}
