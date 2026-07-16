/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.observe.trace.jaeger.logging;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility class for serializing {@link SpanData} collections to JSON format for trace logging.
 */
public final class SpanDataJsonSerializer {

    private SpanDataJsonSerializer() {
    }

    /**
     * Converts a collection of {@link SpanData} to a JSON array string.
     *
     * @param spans the collection of span data to serialize
     * @return a JSON string representation of the spans
     */
    public static String toJson(Collection<SpanData> spans) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        Iterator<SpanData> iterator = spans.iterator();
        while (iterator.hasNext()) {
            appendSpan(sb, iterator.next());
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private static void appendSpan(StringBuilder sb, SpanData span) {
        sb.append('{');
        appendJsonString(sb, "traceId", span.getTraceId());
        sb.append(',');
        appendJsonString(sb, "spanId", span.getSpanId());
        sb.append(',');
        appendJsonString(sb, "parentSpanId", span.getParentSpanContext().getSpanId());
        sb.append(',');
        appendJsonString(sb, "name", span.getName());
        sb.append(',');
        appendJsonString(sb, "kind", span.getKind().name());
        sb.append(',');
        appendJsonNumber(sb, "startEpochNanos", span.getStartEpochNanos());
        sb.append(',');
        appendJsonNumber(sb, "endEpochNanos", span.getEndEpochNanos());
        sb.append(',');
        appendStatus(sb, span.getStatus());
        sb.append(',');
        appendAttributes(sb, "attributes", span.getAttributes());
        sb.append(',');
        appendEvents(sb, span.getEvents());
        sb.append(',');
        appendLinks(sb, span.getLinks());
        sb.append('}');
    }

    private static void appendStatus(StringBuilder sb, StatusData status) {
        sb.append("\"status\":{");
        appendJsonString(sb, "code", status.getStatusCode().name());
        if (status.getStatusCode() != StatusCode.UNSET) {
            sb.append(',');
            appendJsonString(sb, "description", status.getDescription());
        }
        sb.append('}');
    }

    private static void appendAttributes(StringBuilder sb, String key, Attributes attributes) {
        sb.append('"').append(key).append("\":{");
        Iterator<Map.Entry<AttributeKey<?>, Object>> iterator =
                attributes.asMap().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<AttributeKey<?>, Object> entry = iterator.next();
            sb.append('"').append(escapeJson(entry.getKey().getKey())).append("\":");
            appendAttributeValue(sb, entry.getValue());
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        sb.append('}');
    }

    private static void appendAttributeValue(StringBuilder sb, Object value) {
        if (value instanceof String) {
            sb.append('"').append(escapeJson((String) value)).append('"');
        } else if (value instanceof Boolean || value instanceof Long || value instanceof Double) {
            sb.append(value);
        } else if (value instanceof List) {
            sb.append('[');
            Iterator<?> iterator = ((List<?>) value).iterator();
            while (iterator.hasNext()) {
                appendAttributeValue(sb, iterator.next());
                if (iterator.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append(']');
        } else {
            sb.append('"').append(escapeJson(String.valueOf(value))).append('"');
        }
    }

    private static void appendEvents(StringBuilder sb, List<EventData> events) {
        sb.append("\"events\":[");
        Iterator<EventData> iterator = events.iterator();
        while (iterator.hasNext()) {
            EventData event = iterator.next();
            sb.append('{');
            appendJsonString(sb, "name", event.getName());
            sb.append(',');
            appendJsonNumber(sb, "epochNanos", event.getEpochNanos());
            sb.append(',');
            appendAttributes(sb, "attributes", event.getAttributes());
            sb.append('}');
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(']');
    }

    private static void appendLinks(StringBuilder sb, List<LinkData> links) {
        sb.append("\"links\":[");
        Iterator<LinkData> iterator = links.iterator();
        while (iterator.hasNext()) {
            LinkData link = iterator.next();
            sb.append('{');
            appendJsonString(sb, "traceId", link.getSpanContext().getTraceId());
            sb.append(',');
            appendJsonString(sb, "spanId", link.getSpanContext().getSpanId());
            sb.append(',');
            appendAttributes(sb, "attributes", link.getAttributes());
            sb.append('}');
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(']');
    }

    private static void appendJsonString(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append("\":\"").append(escapeJson(value)).append('"');
    }

    private static void appendJsonNumber(StringBuilder sb, String key, long value) {
        sb.append('"').append(key).append("\":").append(value);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = null;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            String replacement = null;
            switch (ch) {
                case '"':
                    replacement = "\\\"";
                    break;
                case '\\':
                    replacement = "\\\\";
                    break;
                case '\n':
                    replacement = "\\n";
                    break;
                case '\r':
                    replacement = "\\r";
                    break;
                case '\t':
                    replacement = "\\t";
                    break;
                case '\b':
                    replacement = "\\b";
                    break;
                case '\f':
                    replacement = "\\f";
                    break;
                default:
                    if (ch < 0x20) {
                        replacement = String.format("\\u%04x", (int) ch);
                    }
                    break;
            }
            if (replacement != null) {
                if (escaped == null) {
                    escaped = new StringBuilder(value.length() + 16);
                    escaped.append(value, 0, i);
                }
                escaped.append(replacement);
            } else if (escaped != null) {
                escaped.append(ch);
            }
        }
        return escaped != null ? escaped.toString() : value;
    }
}
