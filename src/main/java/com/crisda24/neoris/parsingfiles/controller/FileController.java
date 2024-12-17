package com.crisda24.neoris.parsingfiles.controller;

import com.crisda24.neoris.parsingfiles.service.FileProcessorService;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.File;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileProcessorService fileProcessorService;

    public FileController(FileProcessorService fileProcessorService) {
        this.fileProcessorService = fileProcessorService;
    }

    @PostMapping("/upload")
    public Flux<Void> uploadAndProcessFile(@RequestPart("file") FilePart filePart) {
        return filePart.transferTo(new File("uploaded.csv"))
                .thenMany(fileProcessorService.processFile("uploaded.csv"));
    }
}
