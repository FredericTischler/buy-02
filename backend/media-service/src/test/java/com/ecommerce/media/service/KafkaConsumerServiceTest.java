package com.ecommerce.media.service;

import com.ecommerce.media.dto.ProductEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    @Mock
    private MediaService mediaService;

    @Test
    void consumeProductEvent_shouldTriggerDeletionOnDeletedEvent() throws IOException {
        ProductEvent event = new ProductEvent("DELETED", "product-1", "Phone", "seller-1", LocalDateTime.now());

        kafkaConsumerService.consumeProductEvent(event);

        verify(mediaService).deleteAllByProductId("product-1");
    }

    @Test
    void consumeProductEvent_shouldIgnoreOtherEvents() {
        ProductEvent event = new ProductEvent("CREATED", "product-1", "Phone", "seller-1", LocalDateTime.now());

        kafkaConsumerService.consumeProductEvent(event);

        verifyNoInteractions(mediaService);
    }

    @Test
    void consumeProductEvent_shouldSwallowIoExceptionsFromService() throws IOException {
        ProductEvent event = new ProductEvent("DELETED", "product-1", "Phone", "seller-1", LocalDateTime.now());
        doThrow(new IOException("boom")).when(mediaService).deleteAllByProductId("product-1");

        assertThatCode(() -> kafkaConsumerService.consumeProductEvent(event)).doesNotThrowAnyException();

        verify(mediaService).deleteAllByProductId("product-1");
    }
}
