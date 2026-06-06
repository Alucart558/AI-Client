# AI Client - Architecture Documentation

## Overview

This project implements a **100% local and private AI desktop client** using **Hexagonal Architecture (Ports and Adapters)** with strict adherence to **SOLID principles**.

## Technology Stack

- **Language**: Java 21 (LTS)
- **UI Framework**: JavaFX 21
- **AI Integration**: LangChain4j (Ollama for text), HTTP Client (Stable Diffusion for images)
- **Database**: SQLite
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, AssertJ

## Architectural Principles

### Hexagonal Architecture

The application is divided into three main layers:

```
┌─────────────────────────────────────────────────────────┐
│                      ADAPTERS (Input)                    │
│                     JavaFX UI Layer                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                    DOMAIN (Core)                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │              INPUT PORTS (Use Cases)             │   │
│  │  - ChatUseCase                                   │   │
│  │  - ImageGenerationUseCase                        │   │
│  │  - ModelManagementUseCase                        │   │
│  └─────────────────────────────────────────────────┘   │
│                                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │              DOMAIN MODELS                       │   │
│  │  - ChatSession, ChatMessage, MessageRole         │   │
│  │  - GeneratedImage                                │   │
│  │  - AIModel, ModelType                            │   │
│  └─────────────────────────────────────────────────┘   │
│                                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │            OUTPUT PORTS (Interfaces)             │   │
│  │  - TextAIPort, ImageAIPort                       │   │
│  │  - ChatPersistencePort, ImagePersistencePort     │   │
│  │  - ProcessManagementPort                         │   │
│  │  - ModelScannerPort                              │   │
│  └─────────────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   ADAPTERS (Output)                      │
│  - AI Integration (LangChain4j, SD API)                 │
│  - Database (SQLite)                                     │
│  - Process Management (ProcessBuilder)                  │
│  - File System (WatchService)                           │
└─────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.aiclient/
├── domain/                         # Core business logic (zero dependencies on frameworks)
│   ├── model/                      # Domain entities
│   │   ├── ChatSession.java
│   │   ├── ChatMessage.java
│   │   ├── MessageRole.java
│   │   ├── GeneratedImage.java
│   │   ├── AIModel.java
│   │   └── ModelType.java
│   ├── port/
│   │   ├── input/                  # Driving ports (use cases)
│   │   │   ├── ChatUseCase.java
│   │   │   ├── ImageGenerationUseCase.java
│   │   │   └── ModelManagementUseCase.java
│   │   └── output/                 # Driven ports (dependencies)
│   │       ├── TextAIPort.java
│   │       ├── ImageAIPort.java
│   │       ├── ChatPersistencePort.java
│   │       ├── ImagePersistencePort.java
│   │       ├── ProcessManagementPort.java
│   │       └── ModelScannerPort.java
│   └── service/                    # Business logic implementation (coming in Phase 2+)
│
├── adapter/                        # Implementations of ports
│   ├── input/
│   │   └── ui/                     # JavaFX UI (Phase 5)
│   └── output/
│       ├── ai/                     # LangChain4j + SD integration (Phase 3)
│       ├── persistence/            # SQLite implementation (Phase 4)
│       ├── process/                # ProcessBuilder management (Phase 2)
│       └── filesystem/             # WatchService for models (Phase 4)
│
└── application/                    # Application configuration and startup
    └── AIClientApplication.java    # Main class
```

## SOLID Principles Applied

### Single Responsibility Principle (SRP)
- Each port has a single, well-defined responsibility
- Domain models are pure data structures with validation
- Services will handle one specific business capability

### Open/Closed Principle (OCP)
- Ports (interfaces) are stable contracts
- New AI engines can be added by creating new adapters without changing domain logic

### Liskov Substitution Principle (LSP)
- All adapters implement their ports completely
- Substituting one adapter for another doesn't break the application

### Interface Segregation Principle (ISP)
- Ports are fine-grained and focused
- Clients depend only on the methods they actually use

### Dependency Inversion Principle (DIP)
- Domain layer depends only on abstractions (ports)
- Infrastructure (adapters) depends on domain, never the reverse
- Dependency flow: UI → Domain ← Infrastructure

## Design Decisions

### Why Hexagonal Architecture?
1. **Testability**: Business logic can be tested without UI or database
2. **Flexibility**: Easy to swap AI engines (Ollama → LocalAI, SD → ComfyUI)
3. **Maintainability**: Clear boundaries between layers
4. **Future-proofing**: Adding new features doesn't cascade changes

### Why Java 21?
- Latest LTS version with modern language features
- Excellent ecosystem for desktop applications
- Strong typing helps prevent runtime errors
- Virtual threads for efficient process management

### Why SQLite?
- Zero configuration, embedded database
- Perfect for local-only applications
- ACID compliance for data integrity
- No external database server needed

### Why JavaFX?
- Native look and feel
- Modern UI toolkit with CSS styling
- Rich controls for desktop applications
- Active community and good documentation

## Privacy & Security

- **No Network Calls**: All AI processing happens locally
- **Data Sovereignty**: All data stored on user's machine
- **Hard Deletes**: Physical file deletion + database row removal
- **No Telemetry**: Zero analytics or tracking

## Development Phases

1. ✅ **Phase 1**: Architecture definition, package structure, Maven setup
2. **Phase 2**: Process management for Ollama and Stable Diffusion
3. **Phase 3**: AI integration adapters
4. **Phase 4**: Database and file system operations
5. **Phase 5**: JavaFX user interface

## Next Steps (Phase 2)

Implement `ProcessManagementPort` adapter to:
- Start Ollama server on application launch
- Start Stable Diffusion API server
- Monitor process health
- Gracefully shutdown processes on application exit
- Handle process failures and restarts
