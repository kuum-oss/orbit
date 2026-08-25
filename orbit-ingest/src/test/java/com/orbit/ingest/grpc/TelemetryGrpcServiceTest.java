package com.orbit.ingest.grpc;

import com.orbit.ingest.domain.DeviceTelemetry;
import com.orbit.ingest.service.TelemetryService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryGrpcServiceTest {

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private StreamObserver<ProcessingAck> responseObserver;

    private TelemetryGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new TelemetryGrpcService(telemetryService);
    }

    @Test
    void testSendTelemetry() {
        TelemetryPoint point = TelemetryPoint.newBuilder()
                .setDeviceId("device-1")
                .setTimestamp(1000L)
                .setMetricType("temperature")
                .setValue(25.5)
                .build();

        DeviceTelemetry saved = DeviceTelemetry.builder()
                .id(UUID.randomUUID())
                .deviceId("device-1")
                .timestamp(1000L)
                .metricType("temperature")
                .value(25.5)
                .build();

        when(telemetryService.processTelemetry(any(DeviceTelemetry.class))).thenReturn(Mono.just(saved));

        grpcService.sendTelemetry(point, responseObserver);

        ArgumentCaptor<ProcessingAck> ackCaptor = ArgumentCaptor.forClass(ProcessingAck.class);
        verify(responseObserver).onNext(ackCaptor.capture());
        verify(responseObserver).onCompleted();

        ProcessingAck ack = ackCaptor.getValue();
        assertEquals("device-1", ack.getDeviceId());
        assertEquals(1000L, ack.getTimestamp());
        assertTrue(ack.getAccepted());
        assertEquals("Processed", ack.getMessage());
    }

    @Test
    void testStreamTelemetry() {
        TelemetryPoint p1 = TelemetryPoint.newBuilder()
                .setDeviceId("device-stream-1")
                .setTimestamp(2000L)
                .setMetricType("cpu_usage")
                .setValue(45.0)
                .build();

        DeviceTelemetry saved = DeviceTelemetry.builder()
                .id(UUID.randomUUID())
                .deviceId("device-stream-1")
                .timestamp(2000L)
                .metricType("cpu_usage")
                .value(45.0)
                .build();

        when(telemetryService.processTelemetry(any(DeviceTelemetry.class))).thenReturn(Mono.just(saved));

        StreamObserver<TelemetryPoint> requestObserver = grpcService.streamTelemetry(responseObserver);
        requestObserver.onNext(p1);
        requestObserver.onCompleted();

        ArgumentCaptor<ProcessingAck> ackCaptor = ArgumentCaptor.forClass(ProcessingAck.class);
        verify(responseObserver).onNext(ackCaptor.capture());
        verify(responseObserver).onCompleted();

        ProcessingAck ack = ackCaptor.getValue();
        assertEquals("device-stream-1", ack.getDeviceId());
        assertTrue(ack.getAccepted());
    }
}
