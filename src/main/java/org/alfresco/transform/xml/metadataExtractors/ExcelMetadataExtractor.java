package org.alfresco.transform.xml.metadataExtractors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder.Type.EXTRACTOR;

@Component
public class ExcelMetadataExtractor extends AbstractMetadataExtractorEmbedder {

    private static final Logger logger = LoggerFactory.getLogger(ExcelMetadataExtractor.class);
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
    public static final String EXTRACTOR_CONFIGURATION_PROPERTIES = "ExcelMetadataExtractor_configuration.properties";

    private final String sheetName;
    private final String headerAnchor;

    protected ExcelMetadataExtractor() {
        super(EXTRACTOR, logger);
        Properties properties = readProperties();
        sheetName = properties.get("excel.sheet").toString();
        headerAnchor = properties.get("excel.header.anchor").toString();
    }

    private Properties readProperties()
    {
        Properties properties = null;
        try {
            InputStream inputStream =
                    AbstractMetadataExtractorEmbedder.class
                            .getClassLoader()
                            .getResourceAsStream(EXTRACTOR_CONFIGURATION_PROPERTIES);
            if (inputStream != null) {
                properties = new Properties();
                properties.load(inputStream);
            }
        }
        catch (IOException ignore) {}
        return properties;
    }


    @Override
    public String getTransformerName() {
        return "excelextract";
    }

    @Override
    public void embedMetadata(String sourceMimetype, InputStream inputStream, String targetMimetype,
                              OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager)
            throws Exception {
        // No-op: This extractor does not support embedding metadata.
    }

    @Override
    public Map<String, Serializable> extractMetadata(String sourceMimetype,
                                                     InputStream inputStream,
                                                     String targetMimetype,
                                                     OutputStream outputStream,
                                                     Map<String, String> transformOptions,
                                                     TransformManager transformManager) throws Exception {

        ObjectMapper mapper = new ObjectMapper()
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                .enable(SerializationFeature.INDENT_OUTPUT);
        DataFormatter dataFormatter = new DataFormatter(true);

        List<Map<String, Object>> rows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Workbook does not contain required sheet '" + sheetName + "'");
            }

            Row headerRow = null;
            int headerRowIndex = -1;
            for (Row r : sheet) {
                Cell firstCell = r.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (firstCell != null && headerAnchor.equals(firstCell.getStringCellValue().trim())) {
                    headerRow = r;
                    headerRowIndex = r.getRowNum();
                    break;
                }
            }
            if (headerRow == null) {
                throw new IllegalArgumentException("Header row not found: expected first cell value '" + headerAnchor + "'");
            }

            List<String> headers = new ArrayList<>();
            for (Cell c : headerRow) {
                headers.add(dataFormatter.formatCellValue(c).trim().replaceAll("\\s+", " "));
            }

            int lastRow = sheet.getLastRowNum();
            for (int i = headerRowIndex + 1; i <= lastRow; i++) {
                Row r = sheet.getRow(i);
                if (r == null) {
                    break;
                }
                boolean allBlank = true;
                for (Cell c : r) {
                    if (c != null && c.getCellType() != CellType.BLANK) {
                        allBlank = false;
                        break;
                    }
                }
                if (allBlank) {
                    break;
                }

                Map<String, Object> jsonRow = new LinkedHashMap<>();
                for (int col = 0; col < headers.size(); col++) {
                    String header = headers.get(col);
                    Cell c = r.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    Object value = null;
                    if (c != null) {
                        switch (c.getCellType()) {
                            case STRING -> value = c.getStringCellValue();
                            case NUMERIC -> value = DateUtil.isCellDateFormatted(c)
                                    ? DATE_FMT.format(c.getDateCellValue())
                                    : dataFormatter.formatCellValue(c);
                            case BOOLEAN -> value = c.getBooleanCellValue();
                            case FORMULA -> value = dataFormatter.formatCellValue(c);
                            default -> {
                            }
                        }
                    }
                    jsonRow.put(header, value);
                }
                rows.add(jsonRow);
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rows", rows);

        mapper.writeValue(outputStream, body);
        outputStream.flush();

        return new HashMap<>();
    }

}