package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChannelMetricsTest {

    @Test
    @DisplayName("Create ChannelMetrics stores all input fields")
    void createWithValidData() {
        ChannelMetrics metrics = new ChannelMetrics(200, 180, 10, 90, 30, 0.0, 0.0, 0.0);

        assertEquals(200, metrics.totalSent());
        assertEquals(180, metrics.delivered());
        assertEquals(10, metrics.failed());
        assertEquals(90, metrics.opened());
        assertEquals(30, metrics.clicked());
    }

    @Test
    @DisplayName("Delivery rate is correctly computed when totalSent > 0")
    void deliveryRateComputedCorrectly() {
        ChannelMetrics metrics = new ChannelMetrics(100, 80, 0, 0, 0, 0.0, 0.0, 0.0);
        assertEquals(80.0, metrics.deliveryRate());
    }

    @Test
    @DisplayName("Delivery rate is zero when totalSent is zero")
    void deliveryRateIsZeroWhenTotalSentIsZero() {
        ChannelMetrics metrics = new ChannelMetrics(0, 0, 0, 0, 0, 0.0, 0.0, 0.0);
        assertEquals(0.0, metrics.deliveryRate());
    }

    @Test
    @DisplayName("Open rate is correctly computed when delivered > 0")
    void openRateComputedCorrectly() {
        ChannelMetrics metrics = new ChannelMetrics(100, 100, 0, 50, 0, 0.0, 0.0, 0.0);
        assertEquals(50.0, metrics.openRate());
    }

    @Test
    @DisplayName("Open rate is zero when delivered is zero")
    void openRateIsZeroWhenDeliveredIsZero() {
        ChannelMetrics metrics = new ChannelMetrics(0, 0, 0, 0, 0, 0.0, 0.0, 0.0);
        assertEquals(0.0, metrics.openRate());
    }

    @Test
    @DisplayName("Click rate is correctly computed when opened > 0")
    void clickRateComputedCorrectly() {
        ChannelMetrics metrics = new ChannelMetrics(100, 100, 0, 100, 25, 0.0, 0.0, 0.0);
        assertEquals(25.0, metrics.clickRate());
    }

    @Test
    @DisplayName("Click rate is zero when opened is zero")
    void clickRateIsZeroWhenOpenedIsZero() {
        ChannelMetrics metrics = new ChannelMetrics(100, 80, 0, 0, 0, 0.0, 0.0, 0.0);
        assertEquals(0.0, metrics.clickRate());
    }

    @Test
    @DisplayName("Delivery rate is rounded to two decimal places")
    void deliveryRateRoundedToTwoDecimals() {
        // 1/3 ≈ 33.33%
        ChannelMetrics metrics = new ChannelMetrics(3, 1, 0, 0, 0, 0.0, 0.0, 0.0);
        assertEquals(33.33, metrics.deliveryRate());
    }

    @Test
    @DisplayName("Full pipeline: 100 sent, 80 delivered, 40 opened, 20 clicked")
    void fullPipelineRates() {
        ChannelMetrics metrics = new ChannelMetrics(100, 80, 20, 40, 20, 0.0, 0.0, 0.0);
        assertEquals(80.0, metrics.deliveryRate());
        assertEquals(50.0, metrics.openRate());
        assertEquals(50.0, metrics.clickRate());
    }
}
