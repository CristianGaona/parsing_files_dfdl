package com.crisda24.neoris.parsingfiles.service;

import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.ProcessorFactory;
import org.apache.daffodil.japi.ParseResult;
import org.apache.daffodil.japi.io.InputSourceDataInputStream;
import org.apache.daffodil.japi.infoset.JDOMInfosetOutputter;
import org.jdom2.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;

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
            try (FileInputStream fis = new FileInputStream(new File(filePath))) {

                String fileContent = new String(fis.readAllBytes());
                System.out.println("Contenido del archivo recibido:");
                System.out.println(fileContent); // Imprimir el contenido como texto

                InputSourceDataInputStream inputStream = new InputSourceDataInputStream(fis);

                DataProcessor processor = determineSchema(filePath);
                JDOMInfosetOutputter outputter = new JDOMInfosetOutputter();

                ParseResult parseResult = processor.parse(inputStream, outputter);

                if (parseResult.isError()) {
                    sink.error(new RuntimeException("Errores durante el parseo: " + parseResult.getDiagnostics()));
                } else {
                    // Obtener el resultado en formato XML
                    System.out.println("LEER "+ outputter.getResult());
                    Document xmlDocument = outputter.getResult();

                    // Imprimir el XML resultante
                    System.out.println("Resultado del parseo:");
                    System.out.println(xmlDocument.toString());

                    sink.success();
                }
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    private DataProcessor determineSchema(String filePath) {
        if (filePath.endsWith(".schema2")) {
            return schema2Processor;
        }
        return schema1Processor;
    }
}
