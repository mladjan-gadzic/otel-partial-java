package com.gresearch;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.time.Instant;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PartialSpanProcessor implements SpanProcessor {

  private final LogRecordExporter logRecordExporter;
  private final ConcurrentMap<String, Span> activeSpans;
  private final Deque<ReadableSpan> endedSpans;
  private final int scheduledDelayMs;
  private final ScheduledExecutorService scheduledExecutorService;

  public PartialSpanProcessor(LogRecordExporter logRecordExporter, int scheduledDelayMs) {
    this.logRecordExporter = logRecordExporter;
    activeSpans = new ConcurrentHashMap<>();
    endedSpans = new ConcurrentLinkedDeque<>();

    if (scheduledDelayMs <= 0) {
      throw new IllegalArgumentException("scheduledDelayMillis must be greater than 0");
    }
    this.scheduledDelayMs = scheduledDelayMs;
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    startWorkerThread();
  }

  private void startWorkerThread() {
    scheduledExecutorService.scheduleAtFixedRate(this::heartbeat, scheduledDelayMs,
        scheduledDelayMs, TimeUnit.MILLISECONDS);
  }

  private void heartbeat() {
    processEndedSpans();

    processActiveSpans();
  }

  private void processActiveSpans() {
    for (Map.Entry<String, Span> entry : activeSpans.entrySet()) {
      ReadWriteSpan readWriteSpan = (ReadWriteSpan) entry.getValue();
      PartialLogRecordData partialLogRecordData = getPartialLogRecordData(readWriteSpan,
          getHeartbeatLogRecordDataAttributes());
      logRecordExporter.export(Collections.singleton(partialLogRecordData));
      activeSpans.remove(entry.getKey(), entry.getValue());
    }
  }

  private void processEndedSpans() {
    ReadableSpan readableSpan;
    while ((readableSpan = endedSpans.pollLast()) != null) {
      activeSpans.remove(readableSpan.getSpanContext().getSpanId());
    }
  }

  @Override
  public void onStart(Context context, ReadWriteSpan readWriteSpan) {
    PartialLogRecordData partialLogRecordData = getPartialLogRecordData(readWriteSpan,
        getHeartbeatLogRecordDataAttributes());
    logRecordExporter.export(Collections.singleton(partialLogRecordData));
    activeSpans.put(readWriteSpan.getSpanContext().getSpanId(), readWriteSpan);
  }

  private Map<String, String> getHeartbeatLogRecordDataAttributes() {
    return Map.of("partial.event", "heartbeat", "partial.frequency", scheduledDelayMs + "ms");
  }

  private static Map<String, String> getLogRecordDataAttributes() {
    return Map.of("telemetry.logs.cluster", "partial", "telemetry.logs.project", "span");
  }

  private static Map<String, String> getStopLogRecordDataAttributes() {
    return Map.of("partial.event", "stop");
  }

  private static PartialLogRecordData getPartialLogRecordData(ReadableSpan readableSpan,
      Map<String, String> logRecordDataAttributes) {
    PartialLogRecordData partialLogRecordData = new PartialLogRecordData();
    partialLogRecordData.setResource(Resource.create(readableSpan.getAttributes()));
    partialLogRecordData.setInstrumentationScopeInfo(readableSpan.getInstrumentationScopeInfo());
    partialLogRecordData.setSpanContext(readableSpan.getSpanContext());

    // TODO add proto serialization
    partialLogRecordData.setBody("body");

    AttributesBuilder attributesBuilder = Attributes.builder();
    attributesBuilder.putAll(readableSpan.getAttributes());
    getLogRecordDataAttributes().forEach(attributesBuilder::put);
    logRecordDataAttributes.forEach(attributesBuilder::put);

    partialLogRecordData.setAttributes(attributesBuilder.build());
    long nanoTime = Instant.now().toEpochMilli() * 1_000_000;
    partialLogRecordData.setObservedTimestampEpochNanos(nanoTime);
    partialLogRecordData.setTimestampEpochNanos(nanoTime);
    return partialLogRecordData;
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan readableSpan) {
    PartialLogRecordData partialLogRecordData = getPartialLogRecordData(readableSpan,
        getStopLogRecordDataAttributes());
    logRecordExporter.export(Collections.singleton(partialLogRecordData));
    endedSpans.add(readableSpan);
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }

  @Override
  public CompletableResultCode shutdown() {
    scheduledExecutorService.shutdown();
    return SpanProcessor.super.shutdown();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return SpanProcessor.super.forceFlush();
  }

  @Override
  public void close() {
    SpanProcessor.super.close();
  }

}
