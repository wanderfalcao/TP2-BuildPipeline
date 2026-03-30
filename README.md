# TP2 — Refatoração de Sistema Legado: BuildPipeline

Trabalho prático baseado no kata [BuildPipeline-Refactoring-Kata](https://github.com/emilybache/BuildPipeline-Refactoring-Kata) de Emily Bache.

## Sobre o projeto original

O sistema simula um pipeline de integração contínua. A classe `Pipeline` recebe um projeto e executa três etapas em sequência: roda os testes, faz o deploy e envia um resumo por email. O código original concentrava toda essa lógica em um único método `run()` com quase 60 linhas, aninhamento de condicionais em quatro níveis e uso de strings literais para comparar resultados de status.

## Como executar

```bash
cd java
mvn test
```

Requer Java 25 e Maven instalados.

## Problemas identificados no código legado

**Método monolítico:** `Pipeline.run()` misturava três responsabilidades distintas — execução de testes, deploy e notificação por email — tornando o código difícil de ler e de testar isoladamente.

**Strings mágicas:** O resultado de `project.runTests()` e `project.deploy()` era comparado diretamente com a string literal `"success"` em múltiplos pontos, criando acoplamento frágil e risco de erros silenciosos.

**Ausência de testes:** O arquivo `PipelineTest.java` existia mas continha apenas um comentário `TODO`, deixando o código sem qualquer proteção durante mudanças.

**Feature incompleta:** A infraestrutura para deploy em staging e smoke tests já existia no modelo (`Project.runSmokeTests()`, `DeploymentEnvironment.STAGING`), mas nunca era utilizada pelo pipeline.

## Refatorações aplicadas

### Extração de métodos

O método `run()` foi dividido em métodos menores, cada um com uma responsabilidade clara: `runTests`, `deployToEnvironment`, `runSmokeTests` e `sendEmailNotification`. O método público ficou reduzido a duas linhas de delegação, funcionando como um índice do fluxo.

Essa mudança facilita a leitura, o teste unitário de cada etapa e a extensão futura do pipeline sem mexer na estrutura geral.

### Constante para status de sucesso

A string `"success"` foi extraída para uma constante `SUCCESS` na classe `Pipeline`. Além de eliminar a repetição, deixa explícito que o valor tem significado semântico e deve ser tratado com cuidado.

### Value object `PipelineResult`

Os dois booleans que representavam o estado do pipeline (`testsPassed` e `deploySuccessful`) foram agrupados em um record `PipelineResult`. Isso elimina parâmetros soltos passados entre métodos e torna o estado do pipeline explícito como um objeto com identidade própria.

### Implementação dos smoke tests

O pipeline foi estendido para incluir as etapas de staging antes do deploy em produção:

1. Testes unitários
2. Deploy em staging
3. Smoke tests em staging
4. Deploy em produção

Se os smoke tests não estiverem definidos no projeto, o pipeline falha com uma mensagem de erro clara. Essa mudança foi coberta por quatro novos testes automatizados.

## Testes

O projeto conta com 13 testes cobrindo os principais cenários do pipeline: testes passando, testes falhando, projetos sem testes, falha de deploy, falha de smoke tests, smoke tests não definidos, falha no staging e envio de email desabilitado.
