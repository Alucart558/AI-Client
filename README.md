# AI Client Desktop

**100% Local and Private AI Desktop Application**

A fully offline AI client for text conversations and image generation, built with Hexagonal Architecture and SOLID principles.

## Features

- 🗨️ **Text Chat**: Conversational AI powered by local LLMs (Ollama)
- 🎨 **Image Generation**: Create images from text prompts (Stable Diffusion)
- 🔒 **100% Private**: No data leaves your machine
- 📦 **Model Management**: Auto-discover local AI models
- 📜 **History**: Browse and manage conversation history
- 🗑️ **Hard Delete**: Permanently remove data (no soft deletes)

## Technology Stack

- **Java 21** (LTS)
- **JavaFX 21** for modern UI
- **LangChain4j** for LLM integration
- **SQLite** for local storage
- **Ollama** for text generation
- **Stable Diffusion** for image generation

## Prerequisites

1. **Java 21** installed
2. **Ollama** installed and accessible via command line
3. **Stable Diffusion WebUI** with API enabled (optional for image generation)
4. At least one LLM model downloaded in Ollama

## Project Structure

```
src/main/java/com/aiclient/
├── domain/              # Core business logic
│   ├── model/           # Domain entities
│   ├── port/input/      # Use cases (what the app does)
│   ├── port/output/     # Infrastructure interfaces
│   └── service/         # Business logic implementation
├── adapter/             # Infrastructure implementations
│   ├── input/ui/        # JavaFX interface
│   └── output/          # AI, database, process management
└── application/         # Application configuration and startup
```

## Building the Project

```bash
mvn clean install
```

## Running Tests

```bash
mvn test
```

## Current Status

✅ **Phase 1 Complete**: Architecture definition and setup
- Hexagonal architecture structure defined
- All domain models created
- Port interfaces (contracts) established
- Maven configuration complete

✅ **Phase 2 Complete**: Process management infrastructure
- ProcessManagementAdapter with full lifecycle control
- Configuration service for application properties
- Process launcher and wrapper abstractions
- 31 comprehensive unit tests (100% pass rate)
- Windows-compatible command parsing
- Graceful shutdown with forced kill fallback

🚧 **Phase 3 Next**: AI Integration (Adapters)

## Development Roadmap

- [x] Phase 1: Architecture & Setup
- [x] Phase 2: Infrastructure & Process Management
- [ ] Phase 3: AI Integration (Adapters)
- [ ] Phase 4: Data & Storage
- [ ] Phase 5: UI & JavaFX

## License

MIT License - See LICENSE file for details

## Privacy Statement

This application:
- ✅ Runs 100% offline
- ✅ Stores all data locally
- ✅ Never sends data to external servers
- ✅ Provides hard delete functionality
- ✅ No telemetry or analytics

---

**Built with Hexagonal Architecture for maximum flexibility and testability.**
