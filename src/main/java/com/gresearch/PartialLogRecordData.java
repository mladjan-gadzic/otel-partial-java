package com.gresearch;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.common.ValueType;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;

public class PartialLogRecordData implements LogRecordData {

  private Resource resource;
  private InstrumentationScopeInfo instrumentationScopeInfo;
  private long timestampEpochNanos;
  private long observedTimestampEpochNanos;
  private SpanContext spanContext;
  private Severity severity = Severity.INFO;
  private String severityText = "INFO";
  private Attributes attributes;
  private String body;

  @Override
  public Resource getResource() {
    return resource;
  }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return instrumentationScopeInfo;
  }

  @Override
  public long getTimestampEpochNanos() {
    return timestampEpochNanos;
  }

  @Override
  public long getObservedTimestampEpochNanos() {
    return observedTimestampEpochNanos;
  }

  @Override
  public SpanContext getSpanContext() {
    return spanContext;
  }

  @Override
  public Severity getSeverity() {
    return severity;
  }

  @Override
  public String getSeverityText() {
    return severityText;
  }

  @Override
  public Body getBody() {
    return new Body() {
      @Override
      public String asString() {
        return body;
      }

      @Override
      public Type getType() {
        return Type.STRING;
      }
    };
  }

  @Override
  public Value<?> getBodyValue() {
    return new Value<Object>() {
      @Override
      public ValueType getType() {
        return ValueType.STRING;
      }

      @Override
      public Object getValue() {
        return body;
      }

      @Override
      public String asString() {
        return body;
      }
    };
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public int getTotalAttributeCount() {
    return attributes.size();
  }

  public void setResource(Resource resource) {
    this.resource = resource;
  }

  public void setInstrumentationScopeInfo(
      InstrumentationScopeInfo instrumentationScopeInfo) {
    this.instrumentationScopeInfo = instrumentationScopeInfo;
  }

  public void setTimestampEpochNanos(long timestampEpochNanos) {
    this.timestampEpochNanos = timestampEpochNanos;
  }

  public void setObservedTimestampEpochNanos(long observedTimestampEpochNanos) {
    this.observedTimestampEpochNanos = observedTimestampEpochNanos;
  }

  public void setSpanContext(SpanContext spanContext) {
    this.spanContext = spanContext;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  public void setSeverityText(String severityText) {
    this.severityText = severityText;
  }

  public void setAttributes(Attributes attributes) {
    this.attributes = attributes;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
