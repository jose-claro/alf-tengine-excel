package org.alfresco.transform.xml;

import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.reader.TransformConfigResourceReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExcelTransformEngine implements TransformEngine {

    private static final String ENGINE_NAME = "excel";
    private static final String CONFIG_PATH = "classpath:excel_engine_config.json";

    private final TransformConfigResourceReader transformConfigResourceReader;

    @Autowired
    public ExcelTransformEngine(TransformConfigResourceReader transformConfigResourceReader) {
        this.transformConfigResourceReader = transformConfigResourceReader;
    }

    @Override
    public String getTransformEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public String getStartupMessage() {
        return String.format("Startup %s\nNo 3rd party licenses", ENGINE_NAME);
    }

    @Override
    public TransformConfig getTransformConfig() {
        return transformConfigResourceReader.read(CONFIG_PATH);
    }

    /**
     * Provides a probe transformation for testing and validation.
     *
     * @return a ProbeTransform configured for XML metadata extraction testing.
     */
    @Override
    public ProbeTransform getProbeTransform() {

        return new ProbeTransform(
                "sample.xml", "application/vnd.ms-excel", "alfresco-metadata-extract", Map.of(),
                2384, 16, 400, 10240,
                (60 * 30) + 1, (60 * 15) + 20
        );
    }
}