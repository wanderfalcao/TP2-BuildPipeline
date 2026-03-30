package org.sammancoaching;

/**
 * Representa o resultado de uma execução do pipeline, agrupando o status
 * dos testes e do deploy em um único objeto.
 */
public record PipelineResult(boolean testsPassed, boolean deploySuccessful) {}
