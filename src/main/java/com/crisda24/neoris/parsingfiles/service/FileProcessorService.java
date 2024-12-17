package com.crisda24.neoris.parsingfiles.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileProcessorService {

    public Mono<Void> processFile(String filePath);
}
