# MindCash — Contexto do Projeto (Visão de Negócio + Contexto Técnico)

## Visão de Negócio (resumida)
MindCash é um sistema web monolítico para gestão financeira pessoal, pensado inicialmente para uso individual, mas concebido desde o início para evoluir para multi‑usuário e potencial produto. O objetivo é transformar registros financeiros em decisões: controle, consciência e crescimento patrimonial. O produto combina dashboard, registro preciso de transações, metas financeiras e acompanhamento de investimentos com insights acionáveis.

## Visão Técnica (resumida)
- **Tipo de aplicação:** Sistema web monolítico escrito em **Java** (recomendação: Spring Boot).
- **Empacotamento:** aplicação empacotada em container Docker.
- **Banco de dados:** **PostgreSQL** — quando rodarmos a aplicação via Docker Compose, um container Postgres deve subir automaticamente e expor volume persistente local.
- **Migrations:** Flyway ou Liquibase para versionamento do schema.
- **ORM:** JPA (Hibernate) com HikariCP como pool de conexões.
- **Autenticação:** Spring Security (session-based inicialmente) com hashing Argon2/Bcrypt; considerar JWT para futuros clientes mobile/SPAs.
- **Testes de integração:** Testcontainers ou ambientes dedicados em CI para garantir qualidade.
- **Observabilidade:** logs estruturados (SLF4J + Logback), métricas (Prometheus) e dashboards (Grafana) como evolução.
- **Segurança:** TLS obrigatório, rate limiting nas rotas críticas, proteção CSRF, validação e sanitização de inputs, secrets via variáveis de ambiente/secret manager.
- **Execução local:** `docker-compose up` sobe a aplicação + Postgres + (opcional) serviços auxiliares como Redis para cache e um serviço de jobs (Quartz ou agendador embutido).

---

## Docker / Docker Compose (visão geral)
Ao executar a stack localmente (`docker-compose up`) o ambiente mínimo deve conter:
- **mindcash-app** (container da aplicação Java)
- **postgres** (container oficial do PostgreSQL)
- **redis** (opcional — cache e filas leves)
- **pgadmin** (opcional — administração do banco)

Boas práticas no compose:
- não hardcodear credenciais no `docker-compose.yml` — usar `.env` ou secrets.
- mapear volume para persistência do Postgres (`./data/postgres:/var/lib/postgresql/data`).
- healthchecks configurados para dependências (app aguarda DB pronto).
- separar profiles: `local`, `dev`, `prod` com variáveis específicas.

Exemplo (visão, não arquivo definitivo):  
```yaml
services:
  postgres:
    image: postgres:15
    environment:
      - POSTGRES_USER=${DB_USER}
      - POSTGRES_PASSWORD=${DB_PASS}
      - POSTGRES_DB=${DB_NAME}
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
    healthcheck: ...
  mindcash:
    build: .
    depends_on:
      - postgres
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${DB_NAME}
      - SPRING_DATASOURCE_USERNAME=${DB_USER}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASS}
```

---

## Estrutura de alto nível do monólito (pacotes sugeridos)
- `com.mindcash.app` (aplicação)
  - `config` (configurações, segurança)
  - `web` (controllers / endpoints)
  - `service` (regras de negócio)
  - `repository` (persistência JPA)
  - `model` (entidades/dominio)
  - `dto` (objetos de transferência)
  - `job` (tarefas agendadas)
  - `infra` (integrações, mailer, filas)
  - `util` (helpers, validações comuns)

Boas práticas: DTOs na borda, serviços finos com responsabilidade única, tratar transações no service layer, repositories apenas para persistência, validação via Bean Validation (JSR-380).

---

## Considerações gerais de arquitetura e qualidade
- **Segregação de responsabilidades:** controllers leves, services testáveis, repositórios sem lógica de negócio.
- **Transações:** delimitar escopo com `@Transactional` em serviços que alteram múltiplas tabelas.
- **Auditoria:** `created_at`, `updated_at`, `created_by`, `updated_by` em entidades importantes; armazenar histórico quando fizer sentido (event sourcing parcial/opcional mais tarde).
- **Soft delete:** flag `deleted` ou `deleted_at` em entidades que precisem de recuperação.
- **Indices e performance:** índices em colunas de busca frequente (user_id, conta_id, data, categoria_id).
- **Cache:** cache de leituras pesadas (ex.: relatórios agregados) usando Redis com expiração.
- **Jobs e recorrência:** tarefas agendadas para criar transações recorrentes e enviar notificações; usar Quartz ou scheduler do Spring Boot.
- **Migrations automáticas:** rodar Flyway no startup para garantir schema compatível (aplicar em ambientes controlados).

---

## Funcionalidades detalhadas (seguindo boas práticas de negócio)

### 1) Landing Page (página inicial pública)
- Objetivo: apresentar o produto, valor, call to action para cadastro/entrar.
- Conteúdo: resumo do produto, benefícios, screenshots do dashboard (mock), FAQ e link para documentação / contato.
- SEO básico: metatags, open graph, sitemap.
- Boas práticas: tamanho reduzido de assets, SSR (server-side rendering) para SEO se necessário, servir assets via CDN em produção.

### 2) Gestão de Usuários & Identity
- Funcionalidades:
  - cadastro com validação de e‑mail
  - login/logout (session cookie)
  - recuperação e reset de senha (token expira)
  - alteração de perfil, MFA opcional (2FA via TOTP)
  - roles mínimas: `USER`, `ADMIN` (RBAC simples para administração)
- Boas práticas:
  - nunca armazenar senhas em texto (Argon2/Bcrypt)
  - rate limit no endpoint de login/recuperação
  - captchas opcionais em páginas públicas se necessário
  - logs de segurança (tentativas falhas) sem expor dados sensíveis

### 3) Gestão de Contas (onde o dinheiro está)
- Entidades: `Account` com `id, user_id, name, type, currency, balance, metadata`
- Tipos: corrente, poupança, carteira, cartão, corretora, conta compartilhada
- Boas práticas:
  - sempre checar propriedade (user_id) antes de exibir/alterar
  - operações de ajuste de saldo com registro de transação atômico em `@Transactional`
  - conciliação manual (marcar transações conciliadas)
  - soft delete para contas com histórico

### 4) Registro de Transações (núcleo)
- Entidades: `Transaction` com `id, user_id, account_id, category_id, amount, type (DEBIT/CREDIT/TRANSFER), date, description, tags, recurring_id?, metadata`
- Boas práticas:
  - validar valores (não aceitar NaN/inf, limites)
  - usar tipo decimal fixo (`numeric`/BigDecimal) para valores monetários
  - operações financeiras em transações ACID para evitar inconsistência de saldo
  - auditar mudanças (who/when)
  - aceitar importação de extratos (CSV/OFX) com deduplicação inteligente

### 5) Categorias Financeiras
- Permitir categorias hierárquicas (ex.: Alimentação → Restaurante)
- Usuário pode criar categorias personalizadas além de categorias padrão
- Boas práticas:
  - proteger categorias globais vs. categorias do usuário
  - validação para evitar categorias órfãs

### 6) Despesas Recorrentes
- Modelar `RecurringSchedule` com frequência (mensal, semanal, anual, custom cron)
- Job executa criação de transações recorrentes em janela segura (p.ex., próximo mês, depois de fechamento)
- Boas práticas:
  - idempotência: garantir que uma recorrência não gere duplicatas
  - permitir edição retroativa (reaplicar para meses futuros)
  - notificar antes de débito recorrente grande

### 7) Metas Financeiras
- Entidade: `Goal` com `id, user_id, target_amount, saved_amount, deadline, linked_account_id, priority`
- Funcionalidades:
  - vincular aportes manuais/automáticos
  - projeção de alcance baseado em aporte atual e rentabilidade hipotética
- Boas práticas:
  - separar responsabilidades entre cálculo (service) e persistência (repository)
  - exibir risco/tempo estimado (simples, sem promessa de rentabilidade)

### 8) Investimentos (controle simplificado)
- Entidade para posições/ativos: `InvestmentPosition` com `asset_type, symbol, invested_amount, current_amount, created_at`
- Inicialmente manual (registro de aportes, resgates, cotações manuais)
- Boas práticas:
  - armazenar histórico de preços se for oferecer gráficos de evolução
  - integração futura com brokers via APIs (OAuth / keys seguras)
  - considerar limitar operações reais (não executar ordens) — apenas tracking inicialmente

### 9) Dashboard e Relatórios
- Widgets configuráveis: saldo, receitas/despesas, top categorias, metas, evolução patrimonial
- Relatórios exportáveis (CSV/PDF)
- Boas práticas:
  - relatórios devem ser gerados assíncronamente se custosos (jobs) e armazenados cacheados
  - permitir filtros por período, conta, categoria
  - definir SLAs de geração de relatório para UX

### 10) Insights & Recomendações (camada de valor agregado)
- Regras simples inicialmente (regras heurísticas) ex.: aumento de gastos em categorias, sugestões de economia
- Ex.: "Você gastou 25% a mais em delivery este mês" ou "alocar R$X para meta Y reduz prazo para Z meses"
- Boas práticas:
  - deixar claro que são recomendações, não conselhos financeiros formais
  - mecanismo de regras configurável (rule engine simples) para evoluir com novas heurísticas

### 11) Alertas e Notificações
- Tipos: por e‑mail, in‑app, (opcional) push
- Exemplos: saldo baixo, recorrência próxima, meta atingida, suspeita de gasto anormal
- Boas práticas:
  - preferir notificações importantes por e‑mail/in‑app; evitar spam
  - filas para envio (RabbitMQ/Redis Streams) se volume aumentar

### 12) Planejamento Mensal / Orçamento
- Permitir criar orçamento mensal com categorias e comparar `planejado x real`
- Mostrar burn‑rate e projeções
- Boas práticas:
  - interface simples para ajustar orçamento
  - validação e warning quando estourar limite

### 13) Contas Compartilhadas (futuro)
- Modelo `account_members` para linkar múltiplos usuários a uma conta
- Regras de permissões (owner, editor, viewer)
- Boas práticas:
  - transações em contas compartilhadas devem registrar autor da ação
  - auditoria forte e logs conciliáveis

---

## Qualidade de Software e Segurança (requisitos não funcionais)
- **Criptografia em trânsito e em repouso** (TLS, banco de dados com disco criptografado em produção).
- **Secrets management:** usar vault/secret manager em produção.
- **Rate limiting & WAF:** proteção contra abuso.
- **Validação de entrada e sanitização** para evitar injection/XXS.
- **Monitoramento de performance e erros:** APM (NewRelic/Elastic APM) e alertas.
- **Backups e restore:** rotina diária de dumps seguros do Postgres e testes de restore.
- **RPO/RTO:** definir objetivos básicos (p.ex. RPO 24h, RTO 4h) e evoluir conforme necessidade.

---

## UX / Design (paleta de cores e tipografia)
- **Fontes:** `Inter, system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial`
  - Títulos: Inter 600/700
  - Texto: Inter 400
  - Fallback: system fonts para performance
- **Paleta de cores (sugestão):**
  - `--primary: #1A237E` (azul vibrante — ações, CTAs principais)
  - `--primary-600: #084ED6`
  - `--secondary: #00B88A` (verde — sucesso, confirmação)
  - `--accent: #FFB020` (amarelo/laranja — destaque)
  - `--neutral-900: #0F1724` (texto principal)
  - `--neutral-700: #334155` (subtexto)
  - `--neutral-200: #E6EEF8` (background leve)
  - `--danger: #E02424` (erros / alertas críticos)
  - `--surface: #FFFFFF` (cartões, painéis)
- **Tokens de UI básicos:**
  - espaçamentos (4/8/16/24/32)
  - radius padrão: `8px`
  - sombra leve para cards: `0 2px 8px rgba(2,6,23,0.06)`
- **Observação:** definir variações de cor para acessibilidade (contraste) e testar com ferramentas (axe, Lighthouse).

---

## Observações finais e próximos passos práticos
1. **MVP mínimo recomendado (versão inicial):**
   - autenticação (registro/login)
   - contas (criar/editar/excluir)
   - transações (CRUD, transferência entre contas)
   - categorias
   - dashboard simples (saldo, gastos mês)
   - docker-compose com Postgres
   - testes unitários básicos e CI

2. **Iteração 2 (após validação):**
   - recorrências automáticas
   - metas
   - relatórios básicos
   - importador CSV/OFX

3. **Métricas para validar o produto:**
   - tempo médio para registrar uma transação
   - número de transações por usuário / mês
   - taxa de uso do dashboard / sessões ativas
   - metas criadas vs metas atingidas
---

> Este documento oferece contexto técnico e de negócio suficiente para iniciar o desenvolvimento do MindCash como um monólito Java, com operações locais orquestradas por Docker e um Postgres levantado automaticamente. Mantive o foco em práticas de produção e em como cada funcionalidade deve se comportar do ponto de vista do negócio e qualidade de software, sem entrar profundamente em implementações específicas.
