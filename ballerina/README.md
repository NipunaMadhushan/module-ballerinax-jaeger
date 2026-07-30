## Overview

The Jaeger Observability Extension provides an implementation for tracing and publishing traces to a [Jaeger](https://www.jaegertracing.io/) Agent.

### Key Features

- Publish distributed traces to a Jaeger Agent via OpenTelemetry
- Configurable sampler type and parameters
- Support for trace logging to console and file
- Configurable reporter flush interval and buffer size

### Compatibility

This module's `TracerProvider` implementation must return `Tracer`/`ContextPropagators` instances
of the exact `opentelemetry-api`/`opentelemetry-context` classes embedded in the Ballerina
runtime's `ballerina-rt.jar` (currently 1.32.0), so those two artifacts are never bundled by this
module - they always come from the runtime. Every other OpenTelemetry dependency this module
bundles (`sdk-trace`, `sdk-common`, `semconv`, `exporter-otlp-trace`/`exporter-otlp-common`) is
therefore pinned to the same 1.32.0-compatible versions, and shaded into a private package
namespace so they can never collide with whatever (possibly different) OpenTelemetry jars other
Ballerina modules, such as `ballerina/observe`, bundle on a given distribution.

`opentelemetry-extension-trace-propagators` is the exception: it's bumped to 1.63.0 to fix
CVE-2026-45292, since a review of its compiled classes confirmed it calls no `opentelemetry-api`
surface beyond what 1.32.0 already provides (only `TraceFlags.getDefault`/`getSampled`,
`Baggage.builder`, `SpanContext.createFromRemoteParent`), so it remains binary-compatible with the
embedded runtime API. Note this only patches the copy of the vulnerable code that this module
bundles; `opentelemetry-api`'s own copy of the vulnerable baggage-propagation code is embedded
directly in `ballerina-rt.jar` and can only be fixed by updating `ballerina-lang` itself.

If bumping any of the pinned versions in the future, watch for Gradle's default dependency
resolution silently overriding them via transitive constraints (`opentelemetry-semconv` pulls in
`opentelemetry-bom`, whose version constraints can bump `opentelemetry-exporter-otlp-common`,
`opentelemetry-api`, etc. above the declared versions) - `native/build.gradle` forces every
OpenTelemetry artifact to its intended version for exactly this reason.

## Enabling Jaeger Extension

To package the Jaeger extension into the Jar, follow the following steps.
1. Add the following import to your program.
```ballerina
import ballerinax/jaeger as _;
```

2. Add the following to the `Ballerina.toml` when building your program.
```toml
[package]
org = "my_org"
name = "my_package"
version = "1.0.0"

[build-options]
observabilityIncluded=true
```

To enable the extension and publish traces to Jaeger, add the following to the `Config.toml` when running your program.
```toml
[ballerina.observe]
tracingEnabled=true
tracingProvider="jaeger"

[ballerinax.jaeger]
agentHostname="127.0.0.1"       # Optional Configuration. Default value is localhost
agentPort=4317                  # Optional Configuration. Default value is 55680
samplerType="const"             # Optional Configuration. Default value is const
samplerParam=1                  # Optional Configuration. Default value is 1
reporterFlushInterval=1000      # Optional Configuration. Default value is 1000
reporterBufferSize=10000        # Optional Configuration. Default value is 10000
traceLogConsole = false         # Optional Configuration. Default value is false
traceLogFile = ""               # Optional Configuration. Default value is empty string
traceLogLevel = "info"          # Optional Configuration. Default value is info. Possible values are debug, info, warn, error
```
