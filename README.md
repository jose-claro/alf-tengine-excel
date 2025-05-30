# Alfresco Metadata Extract Transform Engine for Excel

This project provides a *sample* Metadata Extract Transform Engine for Excel, designed to be used with Alfresco Transform Service 3.0.0+.

## Features

- Extracts metadata from Excel files and maps it to Alfresco residual property `row`
- Compatible with Alfresco Community and Enterprise versions (25.x)
- Provides a test HTML interface for local validation
- Supports Docker deployment for easy integration

---

## Local Testing

### Requirements

Ensure you have the following dependencies installed:

- **Java** 17+
- **Maven** 3.5+

### Building the Application

To build the JAR package, run:

```bash
mvn clean package
```

### Running the Application

Once built, execute the following command:

```bash
java -jar target/alf-tengine-excel-1.0.0.jar
```

### Testing with the HTML Interface

After starting the service, open the test application at [http://localhost:8090](http://localhost:8090). Use the following input values:

- **file**: Upload an Excel file following the format specified in [`sample.xlsx`](src/main/resources/sample.xlsx).
- **sourceMimetype**: `application/vnd.ms-excel` (Alternatively, other Excel mimetypes are accepted).
- **targetMimetype**: `alfresco-metadata-extract`.

Click the **Transform** button to process the Excel file. The extracted metadata will be returned as a JSON response.

```json
{
  "rows" : [ {
    "Invoice No" : "SampleInvoice1",
    "Invoice Date" : "2020-06-15",
    ...
  }, { ... } 
  ]
}
```

### Additional configuration

This Transform Engine includes a set of configuration files located in `src/main/resources` that control its behavior:

* `application-default.yaml`: Defines core service settings, including the name of the ActiveMQ queue (`excel-engine-queue`) used when deployed with Alfresco Transform Service (ATS), and the engine’s compatibility version (`5.1.7`), ensuring correct registration with the ATS infrastructure
* `excel_engine_config.json`: Describes the media types the engine supports for metadata extraction from Excel documents. It supports various Microsoft Excel formats, including `.xls`, `.xlsb`, `.xlsm`, and `.xlsx`, all transformed to the `alfresco-metadata-extract` format.
* `ExcelMetadataExtractor_configuration.properties`: Document-specific extraction rules. In this configuration, it targets a worksheet named `"Sample Template"` and uses `"Invoice No"` as an anchor header to locate metadata in the Excel file.

---

## Building the Docker Image

### Requirements

- **Docker** 4.30+

## Building the Image

### With the GitHub action `build-and-push-to-ECR.yml`

This project includes a GitHub Action workflow (`build-and-push-to-ECR.yml`) that automatically builds and pushes the Docker image to AWS ECR (Elastic Container Registry).

To use it:
- create the repository in ECR (Elastic Container Registry) if it does not exist.
- fork the repository to your GitHub account.
- set up the necessary secrets in your repository settings:
  - `AWS_ECR_IMAGE_URI`: for example, 123456789012.dkr.ecr.us-east-1.amazonaws.com/your-image-name:tag
  - `AWS_ACCESS_KEY_ID`: your AWS access key ID
  - `AWS_SECRET_ACCESS_KEY`: your AWS secret access key
  - `AWS_REGION`: for example, us-east-1
  - Manually run this workflow in the GitHub Actions tab.

### Manually

Run the following command:

```bash
docker build . -t alfresco-tengine-excel
```

This will create a Docker image named `alfresco-tengine-excel:latest` in your local Docker repository.

---


## Deploying with Alfresco Community 25.x

Ensure your `compose.yaml` file includes the following configuration:

```yaml
services:
  alfresco:
    environment:
      JAVA_OPTS : >-
        -DlocalTransform.core-aio.url=http://transform-core-aio:8090/
        -DlocalTransform.excel.url=http://transform-excel:8090/

  transform-core-aio:
    image: alfresco/alfresco-transform-core-aio:5.1.7

  transform-excel:
    image: alfresco-tengine-excel:latest
```

Key Configuration Updates:
- Add `localTransform.excel.url` to the Alfresco service (`http://transform-excel:8090/` by default).
- Define the **transform-excel** service using the custom-built image.

*Ensure you have built the Docker image (`alfresco-tengine-excel`) before running Docker Compose.*

---

## Deploying with Alfresco Enterprise 25.x

Ensure your `compose.yaml` file includes the following configuration:

```yaml
services:
  alfresco:
    environment:
      JAVA_OPTS : >-
        -Dtransform.service.enabled=true
        -Dtransform.service.url=http://transform-router:8095
        -Dsfs.url=http://shared-file-store:8099/

  transform-router:
    image: quay.io/alfresco/alfresco-transform-router:4.1.7
    environment:
      CORE_AIO_URL: "http://transform-core-aio:8090"
      TRANSFORMER_URL_EXCEL: "http://transform-excel:8090"
      TRANSFORMER_QUEUE_EXCEL: "excel-engine-queue"

  transform-excel:
    image: alfresco-tengine-excel:latest
    environment:
      ACTIVEMQ_URL: "nio://activemq:61616"
      FILE_STORE_URL: >-
        http://shared-file-store:8099/alfresco/api/-default-/private/sfs/versions/1/file
```

Key Configuration Updates:
- Register the Excel transformer with **transform-router**
    - URL: `http://transform-excel:8090/` (default)
    - Queue Name: `excel-engine-queue` (defined in `application-default.yaml`)
- Define the **transform-excel** service and link it to ActiveMQ and Shared File Store services.

*Ensure you have built the Docker image (`alfresco-tengine-excel`) before running Docker Compose.*

---

## Defining a Folder Rule for Excel files in Alfresco Share

To automatically extract metadata from Excel files uploaded to a specific folder, set up a folder rule in Alfresco Share:

1. Open [http://localhost:8080/share/](http://localhost:8080/share/) in your browser.
2. Navigate to the desired folder.
3. Click **Manage Rules**.
4. Create a new rule with the following settings:
    - **When:** Items are created or enter this folder.
    - **If all criteria are met:** Mimetype is `Microsoft Excel`.
    - **Perform Action:** Extract common metadata fields.
5. Save the rule.

From now on, any Excel file uploaded to the folder will be analyzed, and the extracted metadata will be mapped according to residual property `rows`.

---

## How to use a residual value from Server Side JavaScript

A residual property (such as `rows`) can be accessed in JavaScript by iterating over the rows as follows:

```javascript
var rows = document.properties["{}rows"];

for (var i = 0; i < rows.length; i++) {
    var row = rows[i];
    logger.log("Row " + (i + 1));

    for (var key in row) {
        var value = row[key];
        if (value === null || value === undefined) {
            value = "";
        }
        logger.log("  " + key + ": " + value);
    }
}
```

---

## Contributing

Contributions are welcome! To contribute:
1. Fork this repository.
2. Create a new branch (`feature-branch-name`).
3. Commit your changes.
4. Submit a pull request.

For major changes, please open an issue first to discuss your proposal.

---

## Support

For issues and feature requests, please open a GitHub issue in this repository.