package org.corfudb.performancetest;

import lombok.extern.slf4j.Slf4j;
import org.corfudb.runtime.CorfuRuntime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class PerformanceTest {
    protected String endPoint;
    protected int metricsPort;
    protected static final Properties PROPERTIES = new Properties();

    public PerformanceTest() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream input = classLoader.getResourceAsStream("PerformanceTest.properties");

        try {
            PROPERTIES.load(input);
        } catch (IOException e) {
            log.error(e.toString());
        }
        metricsPort = Integer.parseInt(PROPERTIES.getProperty("sequencerMetricsPort", "1000"));
        endPoint = PROPERTIES.getProperty("endPoint", "localhost:9000");
    }

    protected CorfuRuntime initRuntime() {
        CorfuRuntime.CorfuRuntimeParameters parameters = CorfuRuntime.CorfuRuntimeParameters.builder().build();
        parameters.setPrometheusMetricsPort(metricsPort);
        CorfuRuntime corfuRuntime = CorfuRuntime.fromParameters(parameters);
        corfuRuntime.addLayoutServer(endPoint);
        corfuRuntime.connect();
        return corfuRuntime;
    }
}
