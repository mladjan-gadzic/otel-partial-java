package com.gresearch;

import com.google.protobuf.ByteString;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Span.Event;
import io.opentelemetry.proto.trace.v1.Span.Link;
import io.opentelemetry.proto.trace.v1.Span.SpanKind;
import io.opentelemetry.proto.trace.v1.Status;
import io.opentelemetry.proto.trace.v1.TracesData;
import io.opentelemetry.proto.trace.v1.TracesData.Builder;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.List;
import java.util.function.Consumer;

public class ProtoHelper {

  private ProtoHelper() {

  }

  public static TracesData toTracesData(ReadableSpan readableSpan) {
    SpanData spanData = readableSpan.toSpanData();

    Builder tracesDataBuilder = TracesData.newBuilder();
    tracesDataBuilder.addResourceSpans(toResourceSpans(spanData));

    return tracesDataBuilder.build();
  }

  public static ResourceSpans toResourceSpans(SpanData spanData) {
    return ResourceSpans.newBuilder()
        .setResource(toResource(spanData.getResource()))
        .addScopeSpans(toScopeSpans(spanData))
        .build();
  }

  public static ScopeSpans toScopeSpans(SpanData spanData) {
    ScopeSpans.Builder scopeSpansBuilder = ScopeSpans.newBuilder();

    scopeSpansBuilder
        .setScope(toScope(spanData.getInstrumentationScopeInfo()))
        .addSpans(toSpan(spanData));

    setIfNotNull(spanData.getInstrumentationScopeInfo().getSchemaUrl(),
        scopeSpansBuilder::setSchemaUrl);

    return scopeSpansBuilder.build();
  }

  public static io.opentelemetry.proto.trace.v1.Span toSpan(SpanData spanData) {
    Span.Builder spanBuilder = Span.newBuilder();

    // unmapped fields
    // traceState
    // droppedAttributesCount
    // droppedEventsCount
    // droppedLinksCount

    setIfNotNull(getByteString(spanData.getSpanContext().getTraceIdBytes()),
        spanBuilder::setTraceId);
    setIfNotNull(getByteString(spanData.getSpanContext().getSpanIdBytes()),
        spanBuilder::setSpanId);
    setIfNotNull(getByteString(spanData.getParentSpanContext().getSpanIdBytes()),
        spanBuilder::setParentSpanId);
    spanBuilder.setFlags(spanData.getSpanContext().getTraceFlags().asByte());
    setIfNotNull(spanData.getName(), spanBuilder::setName);
    setIfNotNull(SpanKind.forNumber(spanData.getKind().ordinal()), spanBuilder::setKind);
    setIfNotNull(spanData.getStartEpochNanos(), spanBuilder::setStartTimeUnixNano);
    setIfNotNull(spanData.getEndEpochNanos(), spanBuilder::setEndTimeUnixNano);

    spanBuilder
        .addAllAttributes(toAllAttributes(spanData.getAttributes()))
        .addAllEvents(toAllEvents(spanData.getEvents()))
        .addAllLinks(toAllLinks(spanData.getLinks()));

    spanBuilder.setStatus(toStatus(spanData.getStatus()));

    return spanBuilder.build();
  }


  private static Status toStatus(StatusData statusData) {
    Status.Builder statusBuilder = Status.newBuilder();

    setIfNotNull(statusData.getDescription(), statusBuilder::setMessage);
    setIfNotNull(statusData.getStatusCode().ordinal(), statusBuilder::setCodeValue);

    return statusBuilder.build();
  }

  public static List<Link> toAllLinks(List<LinkData> linksData) {
    // unmapped fields
    // .setTraceState()
    // .setDroppedAttributesCount()

    return linksData.stream()
        .map(link -> Link.newBuilder()
            .setTraceId(getByteString(link.getSpanContext().getTraceIdBytes()))
            .setSpanId(getByteString(link.getSpanContext().getSpanIdBytes()))
            .addAllAttributes(
                link.getAttributes().asMap().entrySet().stream()
                    .map(
                        entry -> KeyValue.newBuilder()
                            .setKey(entry.getKey().getKey())
                            .setValue(
                                io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                                    .setStringValue(entry.getValue().toString())
                                    .build())
                            .build())
                    .toList())
            .setFlags(link.getSpanContext().getTraceFlags().asByte())
            .build())
        .toList();
  }

  public static List<KeyValue> toAllAttributes(Attributes spanData) {
    return spanData.asMap().entrySet().stream()
        .map(entry -> KeyValue.newBuilder()
            .setKey(entry.getKey().getKey())
            .setValue(io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                .setStringValue(entry.getValue().toString())
                .build())
            .build())
        .toList();
  }

  public static List<Event> toAllEvents(List<EventData> eventsData) {
    // unmapped fields
    // .setDroppedAttributesCount()

    return eventsData.stream()
        .map(event -> Event.newBuilder()
            .setName(event.getName())
            .setTimeUnixNano(event.getEpochNanos())
            .addAllAttributes(
                event.getAttributes().asMap().entrySet().stream()
                    .map(
                        entry -> io.opentelemetry.proto.common.v1.KeyValue.newBuilder()
                            .setKey(entry.getKey().getKey())
                            .setValue(
                                io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                                    .setStringValue(entry.getValue().toString())
                                    .build())
                            .build())
                    .toList())
            .build())
        .toList();
  }

  public static InstrumentationScope toScope(InstrumentationScopeInfo instrumentationScopeInfo) {
    InstrumentationScope.Builder instrumentationScopeBuilder = InstrumentationScope.newBuilder();

    setIfNotNull(instrumentationScopeInfo.getName(), instrumentationScopeBuilder::setName);
    setIfNotNull(instrumentationScopeInfo.getVersion(), instrumentationScopeBuilder::setVersion);

    return instrumentationScopeBuilder.build();
  }

  public static io.opentelemetry.proto.resource.v1.Resource toResource(Resource resource) {
    // unmapped fields
    // .setDroppedAttributesCount()
    // .addAllEntityRefs()

    return io.opentelemetry.proto.resource.v1.Resource.newBuilder()
        .addAllAttributes(
            resource.getAttributes().asMap().entrySet().stream()
                .map(entry -> io.opentelemetry.proto.common.v1.KeyValue.newBuilder()
                    .setKey(entry.getKey().getKey())
                    .setValue(io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                        .setStringValue(entry.getValue().toString())
                        .build())
                    .build())
                .toList())
        .build();
  }

  public static ByteString getByteString(byte[] bytes) {
    return ByteString.copyFrom(bytes);
  }

  private static <T> void setIfNotNull(T value, Consumer<T> setter) {
    if (value != null) {
      setter.accept(value);
    }
  }

}
