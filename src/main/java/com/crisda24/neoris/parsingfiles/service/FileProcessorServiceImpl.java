package com.crisda24.neoris.parsingfiles.service;

import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.ProcessorFactory;
import org.apache.daffodil.japi.ParseResult;
import org.apache.daffodil.japi.io.InputSourceDataInputStream;
import org.apache.daffodil.japi.infoset.JDOMInfosetOutputter;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class FileProcessorServiceImpl implements FileProcessorService {

    private final DataProcessor schema1Processor;
    private final DataProcessor schema2Processor;

    public FileProcessorServiceImpl() throws Exception {
        ProcessorFactory pf1 = Daffodil.compiler().compileSource(new File("src/main/resources/schema1.dfdl.xsd").toURI());
        ProcessorFactory pf2 = Daffodil.compiler().compileSource(new File("src/main/resources/schema2.dfdl.xsd").toURI());

        // Validación de compilación de esquemas
        if (pf1.isError()) {
            pf1.getDiagnostics().forEach(diag -> System.err.println("ERROR -> " + diag.getMessage()));
            throw new Exception("Error al compilar schema1.dfdl.xsd");
        }
        if (pf2.isError()) {
            pf2.getDiagnostics().forEach(diag -> System.err.println(diag.getMessage()));
            throw new Exception("Error al compilar schema2.dfdl.xsd");
        }

        this.schema1Processor = pf1.onPath("/");
        this.schema2Processor = pf2.onPath("/");
    }

    @Override
    public Mono<Void> processFile(String filePath) {
        System.out.println("archivo -> " + filePath);
        return Mono.create(sink -> {
            try {
                // Fuerza los saltos de línea a LF (\n)
                String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
                content = content.replace("\r\n", "\n"); // Normaliza CRLF a LF
                Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));

                // Crear InputStream después de forzar el formato del archivo
                try (FileInputStream fis = new FileInputStream(new File(filePath))) {

                    InputSourceDataInputStream inputStream = new InputSourceDataInputStream(fis);
                    DataProcessor processor = determineSchema(filePath);
                    JDOMInfosetOutputter outputter = new JDOMInfosetOutputter();

                    ParseResult parseResult = processor.parse(inputStream, outputter);

                    if (parseResult.isError()) {
                        sink.error(new RuntimeException("Errores durante el parseo: " + parseResult.getDiagnostics()));
                    } else {
                        // Obtener el resultado del parseo en formato XML (JDOM Document)
                        Document xmlDocument = outputter.getResult();

                        // Guardar el XML resultante en un archivo
                        saveOutputToFile(xmlDocument, "output.xml");
                        System.out.println("El resultado del parseo se ha guardado en 'output.xml'");

                        sink.success();
                    }
                }
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    private void saveOutputToFile(Document xmlDocument, String outputPath) {
        try {
            // Utiliza XMLOutputter para escribir el XML en un archivo con formato bonito
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            Files.writeString(
                    Paths.get(outputPath),
                    xmlOutputter.outputString(xmlDocument),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            System.err.println("Error al guardar el archivo de salida XML: " + e.getMessage());
            e.printStackTrace();
        }}

    private DataProcessor determineSchema(String filePath) {
        if (filePath.endsWith(".schema2")) {
            return schema2Processor;
        }
        return schema1Processor;
    }
}
