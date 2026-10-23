# Developer Content Creator — Roadmap

## Vision

AI-powered platform generating technical content from GitHub repositories using specialized AI agents.

## Sprint 1 — Foundation & Industrialisation

### 1. Project Foundation
- [ ] Maven multi-module architecture
- [ ] Centralized dependency management (BOM)
- [ ] Java 21 / Spring Boot
- [ ] Shared development conventions

### 2. Industrialisation

#### Versioning
- [ ] Conventional Commits
- [ ] Semantic Versioning
- [ ] Independent microservice releases
- [ ] Maven CI-friendly versions (`${revision}`)
- [ ] Git tags: `<service>-v<version>`

#### CI/CD
- [ ] Backend verification workflow
- [ ] Unit tests and JaCoCo coverage
- [ ] Detect modified microservices
- [ ] Automated semantic releases
- [ ] Docker image build
- [ ] Security scanning
- [ ] Harbor publishing
- [ ] Scheduled base-image rebuild

#### Architecture Documentation
- [ ] C4 Model
- [ ] Structurizr DSL
- [ ] Automatic diagram generation in CI

#### GitOps
- [ ] Helm chart structure
- [ ] Environment-specific configuration
- [ ] Argo CD integration

**Sprint exit criteria:** reproducible builds, automated verification, independently versioned services and immutable release artifacts.

## Sprint 2 — GitHub Integration

- GitHub OAuth
- Repository registration
- Repository metadata extraction
- Webhook integration
- Commit and release analysis

## Sprint 3 — AI Agent Foundation

- Agent orchestrator
- Repository Analysis Agent
- Topic Discovery Agent
- Technical Fact Checking Agent
- Shared agent execution context

## Sprint 4 — Content Generation

- Technical Writer Agent
- LinkedIn Content Agent
- Facebook Content Agent
- Blog Article Agent
- Newsletter Agent
- Release Notes Agent

## Sprint 5 — Editorial Workflow

- Content review
- Draft management
- Human approval
- Content history
- Publication scheduling

## Sprint 6 — Production Readiness

- Kubernetes deployment
- Argo CD GitOps
- Observability
- Security hardening
- Performance testing