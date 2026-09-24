/*
 * Copyright (c) 2025 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.explorer.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.api.service.WProperties;
import org.weasis.core.ui.tp.raven.spinner.SpinnerProgress;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.codec.DicomSeries;

public class DicomTaskManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(DicomTaskManager.class);

  // Configuration constants
  private static final int DEFAULT_CORE_POOL_SIZE = 2;
  private static final int DEFAULT_MAX_POOL_SIZE = 8;
  private static final int DEFAULT_THUMBNAIL_POOL_SIZE = 4;
  private static final int DEFAULT_LOADING_POOL_SIZE = 6;
  private static final long KEEP_ALIVE_TIME = 60L;
  private static final int PROGRESS_UPDATE_DELAY_MS = 100;

  // Thread pools
  private final ExecutorService generalExecutor;
  private final ExecutorService thumbnailExecutor;
  private final ExecutorService loadingExecutor;
  private final ScheduledExecutorService scheduledExecutor;

  // Task tracking
  private final ConcurrentHashMap<String, DicomTask> activeTasks = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, TaskGroup> taskGroups = new ConcurrentHashMap<>();
  private final AtomicLong taskIdCounter = new AtomicLong(0);

  // Progress tracking
  private final SpinnerProgress globalProgressBar;
  private final AtomicInteger activeThumbnailTasks = new AtomicInteger(0);
  private final AtomicInteger activeLoadingTasks = new AtomicInteger(0);

  // Dependencies
  private final DicomExplorer explorer;

  // State
  private volatile boolean shutdownRequested = false;

  public DicomTaskManager(DicomExplorer explorer) {
    this.explorer = explorer;

    // Initialize thread pools
    this.generalExecutor = createGeneralExecutor();
    this.thumbnailExecutor = createThumbnailExecutor();
    this.loadingExecutor = createLoadingExecutor();
    this.scheduledExecutor =
        Executors.newScheduledThreadPool(2, new DicomThreadFactory("DicomScheduled"));

    // Initialize progress bar
    this.globalProgressBar = new SpinnerProgress();
    setupGlobalProgressBar();

    // Start progress update task
    startProgressUpdateTask();

    LOGGER.info(
        "DicomTaskManager initialized with pools: general={}, thumbnail={}, loading={}",
        getPoolSize("general"),
        getPoolSize("thumbnail"),
        getPoolSize("loading"));
  }

  // ========== Executor Creation ==========

  private ExecutorService createGeneralExecutor() {
    int corePoolSize =
        getConfiguredPoolSize("weasis.dicom.general.pool.core", DEFAULT_CORE_POOL_SIZE);
    int maxPoolSize = getConfiguredPoolSize("weasis.dicom.general.pool.max", DEFAULT_MAX_POOL_SIZE);

    return Executors.newFixedThreadPool(maxPoolSize, new DicomThreadFactory("DicomGeneral"));
  }

  private ExecutorService createThumbnailExecutor() {
    int poolSize =
        getConfiguredPoolSize("weasis.dicom.thumbnail.pool.size", DEFAULT_THUMBNAIL_POOL_SIZE);

    return Executors.newFixedThreadPool(poolSize, new DicomThreadFactory("DicomThumbnail"));
  }

  private ExecutorService createLoadingExecutor() {
    int poolSize =
        getConfiguredPoolSize("weasis.dicom.loading.pool.size", DEFAULT_LOADING_POOL_SIZE);

    return Executors.newFixedThreadPool(poolSize, new DicomThreadFactory("DicomLoading"));
  }

  private int getConfiguredPoolSize(String property, int defaultValue) {
    try {
      WProperties preferences = GuiUtils.getUICore().getSystemPreferences();
      return preferences.getIntProperty(property, defaultValue);
    } catch (Exception e) {
      LOGGER.warn(
          "Failed to get configured pool size for {}, using default: {}", property, defaultValue);
      return defaultValue;
    }
  }

  private int getPoolSize(String poolType) {
    return switch (poolType) {
      case "thumbnail" -> DEFAULT_THUMBNAIL_POOL_SIZE;
      case "loading" -> DEFAULT_LOADING_POOL_SIZE;
      default -> DEFAULT_MAX_POOL_SIZE;
    };
  }

  // ========== Task Submission ==========

  public CompletableFuture<Void> executeGeneralTask(String taskName, Runnable task) {
    return executeTask(taskName, task, generalExecutor, TaskType.GENERAL);
  }

  public CompletableFuture<Void> executeThumbnailTask(String taskName, Runnable task) {
    return executeTask(taskName, task, thumbnailExecutor, TaskType.THUMBNAIL);
  }

  public CompletableFuture<Void> executeLoadingTask(String taskName, Runnable task) {
    return executeTask(taskName, task, loadingExecutor, TaskType.LOADING);
  }

  public <T> CompletableFuture<T> executeGeneralTask(
      String taskName, java.util.concurrent.Callable<T> task) {
    return executeTask(taskName, task, generalExecutor, TaskType.GENERAL);
  }

  public <T> CompletableFuture<T> executeThumbnailTask(
      String taskName, java.util.concurrent.Callable<T> task) {
    return executeTask(taskName, task, thumbnailExecutor, TaskType.THUMBNAIL);
  }

  public <T> CompletableFuture<T> executeLoadingTask(
      String taskName, java.util.concurrent.Callable<T> task) {
    return executeTask(taskName, task, loadingExecutor, TaskType.LOADING);
  }

  private CompletableFuture<Void> executeTask(
      String taskName, Runnable task, ExecutorService executor, TaskType type) {
    if (shutdownRequested) {
      return CompletableFuture.completedFuture(null);
    }

    String taskId = generateTaskId();
    DicomTask dicomTask = new DicomTask(taskId, taskName, type);

    CompletableFuture<Void> future =
        CompletableFuture.runAsync(
            () -> {
              executeWithTracking(dicomTask, task);
            },
            executor);

    dicomTask.setFuture(future);
    activeTasks.put(taskId, dicomTask);

    // Handle completion
    future.whenComplete(
        (result, throwable) -> {
          handleTaskCompletion(taskId, throwable);
        });

    updateTaskCounters(type, 1);
    updateGlobalProgress();

    LOGGER.debug("Submitted {} task: {} [{}]", type, taskName, taskId);
    return future;
  }

  private <T> CompletableFuture<T> executeTask(
      String taskName,
      java.util.concurrent.Callable<T> task,
      ExecutorService executor,
      TaskType type) {
    if (shutdownRequested) {
      return CompletableFuture.completedFuture(null);
    }

    String taskId = generateTaskId();
    DicomTask dicomTask = new DicomTask(taskId, taskName, type);

    CompletableFuture<T> future =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                return executeWithTracking(dicomTask, task);
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            },
            executor);

    dicomTask.setFuture(future);
    activeTasks.put(taskId, dicomTask);

    // Handle completion
    future.whenComplete(
        (result, throwable) -> {
          handleTaskCompletion(taskId, throwable);
        });

    updateTaskCounters(type, 1);
    updateGlobalProgress();

    LOGGER.debug("Submitted {} task: {} [{}]", type, taskName, taskId);
    return future;
  }

  // ========== Task Execution Tracking ==========

  private void executeWithTracking(DicomTask task, Runnable runnable) {
    try {
      task.markStarted();
      runnable.run();
      task.markCompleted();
    } catch (Exception e) {
      task.markFailed(e);
      throw e;
    }
  }

  private <T> T executeWithTracking(DicomTask task, java.util.concurrent.Callable<T> callable)
      throws Exception {
    try {
      task.markStarted();
      T result = callable.call();
      task.markCompleted();
      return result;
    } catch (Exception e) {
      task.markFailed(e);
      throw e;
    }
  }

  private void handleTaskCompletion(String taskId, Throwable throwable) {
    DicomTask task = activeTasks.remove(taskId);
    if (task != null) {
      updateTaskCounters(task.getType(), -1);
      updateGlobalProgress();

      if (throwable != null && !(throwable instanceof CancellationException)) {
        LOGGER.warn("Task failed: {} [{}]", task.getName(), taskId, throwable);
      } else {
        LOGGER.debug("Task completed: {} [{}]", task.getName(), taskId);
      }
    }
  }

  private void updateTaskCounters(TaskType type, int delta) {
    switch (type) {
      case THUMBNAIL -> activeThumbnailTasks.addAndGet(delta);
      case LOADING -> activeLoadingTasks.addAndGet(delta);
      case GENERAL -> {
        // General tasks don't have a separate counter
      }
    }
  }

  // ========== Series-Specific Task Management ==========

  public CompletableFuture<Void> loadSeriesInBackground(DicomSeries series) {
    String taskName = "Load Series: " + getSeriesDisplayName(series);

    return executeLoadingTask(
        taskName,
        () -> {
          try {
            LOGGER.debug("Loading series in background: {}", getSeriesDisplayName(series));
            series.getMedia(0, null, null); // Trigger loading

            SwingUtilities.invokeLater(
                () -> {
                  explorer.updateSplitSeries(series);
                });

          } catch (Exception e) {
            LOGGER.error("Failed to load series: {}", getSeriesDisplayName(series), e);
          }
        });
  }

  public CompletableFuture<Void> buildThumbnailInBackground(DicomSeries series) {
    String taskName = "Build Thumbnail: " + getSeriesDisplayName(series);

    return executeThumbnailTask(
        taskName,
        () -> {
          try {
            LOGGER.debug("Building thumbnail in background: {}", getSeriesDisplayName(series));
            // Thumbnail building logic would go here

          } catch (Exception e) {
            LOGGER.error(
                "Failed to build thumbnail for series: {}", getSeriesDisplayName(series), e);
          }
        });
  }

  public CompletableFuture<Void> refreshSeriesInBackground(DicomSeries series) {
    String taskName = "Refresh Series: " + getSeriesDisplayName(series);

    return executeGeneralTask(
        taskName,
        () -> {
          try {
            LOGGER.debug("Refreshing series in background: {}", getSeriesDisplayName(series));

            SwingUtilities.invokeLater(
                () -> {
                  explorer.updateSplitSeries(series);
                });

          } catch (Exception e) {
            LOGGER.error("Failed to refresh series: {}", getSeriesDisplayName(series), e);
          }
        });
  }

  // ========== Task Group Management ==========

  public TaskGroup createTaskGroup(String groupName) {
    String groupId = generateTaskId();
    TaskGroup group = new TaskGroup(groupId, groupName);
    taskGroups.put(groupId, group);

    LOGGER.debug("Created task group: {} [{}]", groupName, groupId);
    return group;
  }

  public CompletableFuture<Void> executeTaskGroup(TaskGroup group) {
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (TaskGroupItem item : group.getTasks()) {
      CompletableFuture<Void> future =
          switch (item.getType()) {
            case GENERAL -> executeGeneralTask(item.getName(), item.getRunnable());
            case THUMBNAIL -> executeThumbnailTask(item.getName(), item.getRunnable());
            case LOADING -> executeLoadingTask(item.getName(), item.getRunnable());
          };
      futures.add(future);
    }

    CompletableFuture<Void> groupFuture =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

    groupFuture.whenComplete(
        (result, throwable) -> {
          taskGroups.remove(group.getId());
          if (throwable != null) {
            LOGGER.warn("Task group failed: {} [{}]", group.getName(), group.getId(), throwable);
          } else {
            LOGGER.debug("Task group completed: {} [{}]", group.getName(), group.getId());
          }
        });

    return groupFuture;
  }

  // ========== Task Cancellation ==========

  public boolean cancelTask(String taskId) {
    DicomTask task = activeTasks.get(taskId);
    if (task != null && task.getFuture() != null) {
      boolean cancelled = task.getFuture().cancel(true);
      if (cancelled) {
        activeTasks.remove(taskId);
        updateTaskCounters(task.getType(), -1);
        updateGlobalProgress();
        LOGGER.debug("Cancelled task: {} [{}]", task.getName(), taskId);
      }
      return cancelled;
    }
    return false;
  }

  public void cancelAllTasks() {
    LOGGER.info("Cancelling all active tasks ({} tasks)", activeTasks.size());

    List<String> taskIds = new ArrayList<>(activeTasks.keySet());
    for (String taskId : taskIds) {
      cancelTask(taskId);
    }
  }

  public void cancelTasksByType(TaskType type) {
    List<String> taskIds =
        activeTasks.values().stream()
            .filter(task -> task.getType() == type)
            .map(DicomTask::getId)
            .toList();

    LOGGER.debug("Cancelling {} tasks of type {}", taskIds.size(), type);
    taskIds.forEach(this::cancelTask);
  }

  // ========== Progress Management ==========

  private void setupGlobalProgressBar() {
    globalProgressBar.setStringPainted(false);
    globalProgressBar.setVisible(false);
    globalProgressBar.setIndeterminate(true);
  }

  private void startProgressUpdateTask() {
    scheduledExecutor.scheduleWithFixedDelay(
        this::updateGlobalProgress,
        PROGRESS_UPDATE_DELAY_MS,
        PROGRESS_UPDATE_DELAY_MS,
        TimeUnit.MILLISECONDS);
  }

  private void updateGlobalProgress() {
    if (shutdownRequested) {
      return;
    }

    SwingUtilities.invokeLater(
        () -> {
          int totalActiveTasks = activeTasks.size();
          boolean hasActiveTasks = totalActiveTasks > 0;

          globalProgressBar.setVisible(hasActiveTasks);

          if (hasActiveTasks) {
            String tooltip = buildProgressTooltip();
            globalProgressBar.setToolTipText(tooltip);
          }
        });
  }

  private String buildProgressTooltip() {
    int thumbnailTasks = activeThumbnailTasks.get();
    int loadingTasks = activeLoadingTasks.get();
    int generalTasks = activeTasks.size() - thumbnailTasks - loadingTasks;

    StringBuilder sb = new StringBuilder("<html>");
    sb.append("<b>Active Tasks:</b><br>");

    if (generalTasks > 0) {
      sb.append("General: ").append(generalTasks).append("<br>");
    }
    if (thumbnailTasks > 0) {
      sb.append("Thumbnails: ").append(thumbnailTasks).append("<br>");
    }
    if (loadingTasks > 0) {
      sb.append("Loading: ").append(loadingTasks).append("<br>");
    }

    sb.append("</html>");
    return sb.toString();
  }

  public SpinnerProgress getGlobalProgressBar() {
    return globalProgressBar;
  }

  // ========== Utility Methods ==========

  private String generateTaskId() {
    return "task-" + taskIdCounter.incrementAndGet();
  }

  private String getSeriesDisplayName(Series<?> series) {
    if (series == null) {
      return "Unknown Series";
    }

    String description = series.toString();
    return StringUtil.hasText(description) ? description : "Series " + series.getSeriesNumber();
  }

  // ========== Statistics and Monitoring ==========

  public List<DicomTask> getActiveTasks() {
    return new ArrayList<>(activeTasks.values());
  }

  public List<DicomTask> getActiveTasksByType(TaskType type) {
    return activeTasks.values().stream().filter(task -> task.getType() == type).toList();
  }

  public boolean hasActiveTasks() {
    return !activeTasks.isEmpty();
  }

  public boolean hasActiveTasksOfType(TaskType type) {
    return activeTasks.values().stream().anyMatch(task -> task.getType() == type);
  }

  // ========== Shutdown Management ==========

  public void shutdown() {
    LOGGER.info("Shutting down DicomTaskManager...");
    shutdownRequested = true;

    // Cancel all active tasks
    cancelAllTasks();

    // Shutdown executors
    shutdownExecutor(generalExecutor, "General");
    shutdownExecutor(thumbnailExecutor, "Thumbnail");
    shutdownExecutor(loadingExecutor, "Loading");
    shutdownExecutor(scheduledExecutor, "Scheduled");

    // Clear tracking data
    activeTasks.clear();
    taskGroups.clear();

    LOGGER.info("DicomTaskManager shutdown completed");
  }

  private void shutdownExecutor(ExecutorService executor, String name) {
    try {
      executor.shutdown();
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        LOGGER.warn("{} executor did not terminate gracefully, forcing shutdown", name);
        executor.shutdownNow();
        if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
          LOGGER.error("{} executor did not terminate after forced shutdown", name);
        }
      }
      LOGGER.debug("{} executor shutdown completed", name);
    } catch (InterruptedException e) {
      LOGGER.warn("{} executor shutdown interrupted", name);
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  // ========== Inner Classes ==========

  /** Custom thread factory for DICOM tasks */
  private static class DicomThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    DicomThreadFactory(String namePrefix) {
      this.namePrefix = namePrefix + "-";
    }

    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
      t.setDaemon(true);
      t.setPriority(Thread.NORM_PRIORITY);
      return t;
    }
  }

  /** Represents a DICOM task with metadata */
  public static class DicomTask {
    private final String id;
    private final String name;
    private final TaskType type;
    private final long creationTime;
    private long startTime;
    private long completionTime;
    private TaskStatus status;
    private Exception exception;
    private Future<?> future;

    public DicomTask(String id, String name, TaskType type) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.creationTime = System.currentTimeMillis();
      this.status = TaskStatus.PENDING;
    }

    public void markStarted() {
      this.startTime = System.currentTimeMillis();
      this.status = TaskStatus.RUNNING;
    }

    public void markCompleted() {
      this.completionTime = System.currentTimeMillis();
      this.status = TaskStatus.COMPLETED;
    }

    public void markFailed(Exception exception) {
      this.completionTime = System.currentTimeMillis();
      this.status = TaskStatus.FAILED;
      this.exception = exception;
    }

    // Getters
    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public TaskType getType() {
      return type;
    }

    public long getCreationTime() {
      return creationTime;
    }

    public long getStartTime() {
      return startTime;
    }

    public long getCompletionTime() {
      return completionTime;
    }

    public TaskStatus getStatus() {
      return status;
    }

    public Exception getException() {
      return exception;
    }

    public Future<?> getFuture() {
      return future;
    }

    public void setFuture(Future<?> future) {
      this.future = future;
    }

    public long getDuration() {
      if (startTime == 0) return 0;
      long endTime = completionTime > 0 ? completionTime : System.currentTimeMillis();
      return endTime - startTime;
    }

    @Override
    public String toString() {
      return String.format("DicomTask[id=%s, name=%s, type=%s, status=%s]", id, name, type, status);
    }
  }

  /** Task group for related operations */
  public static class TaskGroup {
    private final String id;
    private final String name;
    private final List<TaskGroupItem> tasks = new ArrayList<>();
    private final long creationTime;

    public TaskGroup(String id, String name) {
      this.id = id;
      this.name = name;
      this.creationTime = System.currentTimeMillis();
    }

    public TaskGroup addTask(String taskName, Runnable task, TaskType type) {
      tasks.add(new TaskGroupItem(taskName, task, type));
      return this;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public List<TaskGroupItem> getTasks() {
      return Collections.unmodifiableList(tasks);
    }

    public long getCreationTime() {
      return creationTime;
    }

    public int getTaskCount() {
      return tasks.size();
    }
  }

  /** Item within a task group */
  public static class TaskGroupItem {
    private final String name;
    private final Runnable runnable;
    private final TaskType type;

    public TaskGroupItem(String name, Runnable runnable, TaskType type) {
      this.name = name;
      this.runnable = runnable;
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public Runnable getRunnable() {
      return runnable;
    }

    public TaskType getType() {
      return type;
    }
  }

  /** Task types */
  public enum TaskType {
    GENERAL,
    THUMBNAIL,
    LOADING
  }

  /** Task status */
  public enum TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
  }
}
