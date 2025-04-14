package com.gresearch;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

public class Example {

  public static void main(String[] args) {
    // Set up OTLP exporter
    OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint("http://localhost:4317") // OTLP gRPC endpoint of the Collector
        .build();

    OtlpHttpLogRecordExporter logExporter = OtlpHttpLogRecordExporter.builder()
        .setEndpoint("http://localhost:4318/v1/logs") // OTLP HTTP endpoint of the Collector
        .build();

    // Create Tracer Provider
    Resource resource = Resource.create(
        Attributes.of(AttributeKey.stringKey("service.name"), "my-service"));
    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(new PartialSpanProcessor(logExporter, 5000))
        .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
        .setResource(Resource.getDefault()
            .merge(resource))
        .build();

    OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .buildAndRegisterGlobal();

    // Use the tracer
    Tracer tracer = GlobalOpenTelemetry.getTracer("example-tracer");

    Span span = tracer.spanBuilder("example-span").startSpan();
    span.addEvent("doing some work...");
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    span.end();

    // Give it some time to export
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    tracerProvider.close();
  }
}