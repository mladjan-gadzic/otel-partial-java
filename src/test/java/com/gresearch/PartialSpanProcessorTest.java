package com.gresearch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartialSpanProcessorTest {

  private LogRecordExporter logRecordExporter;
  private PartialSpanProcessor partialSpanProcessor;
  private ReadWriteSpan readWriteSpan;
  private ReadableSpan readableSpan;

  @BeforeEach
  void setUp() {
    logRecordExporter = mock(LogRecordExporter.class);
    partialSpanProcessor = new PartialSpanProcessor(logRecordExporter, 1000);

    readWriteSpan = mock(ReadWriteSpan.class);
    readableSpan = mock(ReadableSpan.class);

    SpanContext spanContext = mock(SpanContext.class);
    when(spanContext.getSpanId()).thenReturn("span-id");
    when(readWriteSpan.getSpanContext()).thenReturn(spanContext);
    when(readableSpan.getSpanContext()).thenReturn(spanContext);

    when(readWriteSpan.getAttributes()).thenReturn(Attributes.empty());
    when(readableSpan.getAttributes()).thenReturn(Attributes.empty());
  }

  @Test
  void testOnStart() {
    partialSpanProcessor.onStart(Context.root(), readWriteSpan);

    verify(logRecordExporter, times(1)).export(any());
  }

  @Test
  void testOnEnd() {
    partialSpanProcessor.onEnd(readableSpan);

    verify(logRecordExporter, times(1)).export(any());
  }

  @Test
  void testHeartbeatProcessesActiveSpans() {
    partialSpanProcessor.onStart(Context.root(), readWriteSpan);

    partialSpanProcessor.heartbeat();

    verify(logRecordExporter, times(2)).export(any());
  }
}