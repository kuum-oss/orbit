package com.orbit.ingest.grpc;

import com.orbit.ingest.domain.DeviceTelemetry;
import com.orbit.ingest.service.TelemetryService;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.devh.boot.grpc.server.service.GrpcService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@GrpcService
public class TelemetryGrpcService extends TelemetryProcessorGrpc.TelemetryProcessorImplBase {

    private static final Logger log = LoggerFactory.getLogger(TelemetryGrpcService.class);
    private final TelemetryService telemetryService;

    public TelemetryGrpcService(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @Override
    public StreamObserver<TelemetryPoint> streamTelemetry(StreamObserver<ProcessingAck> responseObserver) {
        return new StreamObserver<TelemetryPoint>() {
            @Override
            public void onNext(TelemetryPoint point) {
                processAndAck(point).subscribe(
                        responseObserver::onNext,
                        error -> {
                            log.error("Error processing stream item", error);
                            responseObserver.onError(error);
                        }
                );
            }

            @Override
            public void onError(Throwable t) {
                log.error("Client stream error", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void sendTelemetry(TelemetryPoint request, StreamObserver<ProcessingAck> responseObserver) {
        processAndAck(request).subscribe(
                ack -> {
                    responseObserver.onNext(ack);
                    responseObserver.onCompleted();
                },
                error -> {
                    log.error("Error processing unary request", error);
                    responseObserver.onError(error);
                }
        );
    }

    private Mono<ProcessingAck> processAndAck(TelemetryPoint point) {
        DeviceTelemetry telemetry = DeviceTelemetry.builder()
                .deviceId(point.getDeviceId())
                .timestamp(point.getTimestamp())
                .metricType(point.getMetricType())
                .value(point.getValue())
                .latitude(point.getLatitude())
                .longitude(point.getLongitude())
                .build();

        return telemetryService.processTelemetry(telemetry)
                .map(saved -> ProcessingAck.newBuilder()
                        .setDeviceId(saved.getDeviceId())
                        .setTimestamp(saved.getTimestamp())
                        .setAccepted(true)
                        .setMessage("Processed")
                        .build())
                .onErrorResume(e -> Mono.just(ProcessingAck.newBuilder()
                        .setDeviceId(point.getDeviceId())
                        .setTimestamp(point.getTimestamp())
                        .setAccepted(false)
                        .setMessage(e.getMessage())
                        .build()));
    }
}
