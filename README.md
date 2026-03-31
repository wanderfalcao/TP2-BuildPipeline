# TP2 — Refatoração de Sistema Legado: BuildPipeline

Trabalho prático baseado no kata [BuildPipeline-Refactoring-Kata](https://github.com/emilybache/BuildPipeline-Refactoring-Kata) de Emily Bache.

## O projecto

O sistema simula um pipeline de integração contínua. A classe `Pipeline` recebe um projecto e executa uma sequência de etapas: testes unitários, deploy em staging, smoke tests, deploy em produção e notificação por email. O código original concentrava toda essa lógica num único método `run()` com quase 60 linhas, aninhamento em quatro níveis e strings literais hardcoded para comparar resultados de status.

## Como executar

```bash
cd java
mvn test
```

Ou via Gradle (requer Java 25):

```bash
cd java
./gradlew test
```

O Maven é o build principal. O Gradle foi actualizado de 6.2.2 para 8.10 para compatibilidade com Java 25.

---

## Estado Original vs Estado Refatorado

| Aspecto                            | Antes                                               | Depois                                                             |
|------------------------------------|-----------------------------------------------------|--------------------------------------------------------------------|
| **Estrutura do método principal**  | `run()` com ~58 linhas e 4 níveis de aninhamento    | `run()` com 2 linhas; lógica distribuída em 5 métodos focados      |
| **Strings mágicas**                | `"success"` comparado directamente em 4 pontos      | Constante `SUCCESS` declarada uma vez (`Pipeline.java:11`)         |
| **Estado do pipeline**             | Dois booleans soltos passados entre blocos          | `PipelineResult` record imutável com semântica explícita           |
| **Staging e smoke tests**          | Infra existia no modelo mas nunca era chamada       | Integrados ao fluxo com falha explícita se não definidos           |
| **Cobertura de testes**            | `PipelineTest.java` com apenas um comentário `TODO` | 15 testes cobrindo todos os caminhos de execução                   |
| **Dependências**                   | Parâmetros no construtor (já correcto)              | Mantidas; interfaces `Config`, `Logger`, `Emailer` sem alteração   |
| **Build**                          | Gradle 6.2.2 incompatível com Java 25               | Gradle 8.10; Maven e Gradle alinhados em `sourceCompatibility`     |

---

## Estrutura de pacotes

```
java/src/main/java/org/sammancoaching/
├── Pipeline.java              # orquestrador do pipeline
├── PipelineResult.java        # value object com estado da execução
└── dependencies/
    ├── Project.java            # modelo de projecto + Builder
    ├── Config.java             # interface de configuração
    ├── Logger.java             # interface de logging
    ├── Emailer.java            # interface de notificação
    ├── DeploymentEnvironment.java  # enum STAGING / PRODUCTION
    └── TestStatus.java         # enum NO_TESTS / PASSING_TESTS / FAILING_TESTS

java/src/test/java/org/sammancoaching/
├── PipelineTest.java          # 15 testes de integração
└── CapturingLogger.java       # test double para verificar logs
```

---

## Principais decisões de refatoração

**Extração de métodos (SLAP)** — O `run()` original misturava abstracções: comparação de string bruta, decisão de fluxo e chamadas a infra no mesmo nível. Cada responsabilidade foi extraída para um método com nome declarativo. O método público ficou como índice do fluxo; os privados tratam os detalhes.

**`PipelineResult` como value object** — Os dois booleans que representavam o estado do pipeline perdiam contexto ao ser passados implicitamente entre blocos. Agrupá-los num `record` imutável torna o vínculo explícito e elimina parâmetros soltos.

**Constante `SUCCESS`** — Não é só evitar repetição. A string `"success"` tem significado de protocolo — é a resposta que `Project.deploy()` e `Project.runTests()` devolvem para indicar sucesso. Dar-lhe um nome deixa isso claro.

**Smoke tests e staging** — A infra já existia no modelo (`DeploymentEnvironment.STAGING`, `Project.runSmokeTests()`). Integrá-la completou o comportamento esperado de um pipeline de CI/CD real, sem alterar as interfaces existentes.
