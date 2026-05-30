# QAdmin

A plug-and-play Java library built with the Quarkus framework,
enabling entity data management through an Angular-based graphical user interface.

## Key features
- **Automatic Entity Detection**: Seamlessly scans and identifies JPA entities within your application.
- **Graphical User Interface**: Provides an Angular-based UI for data display.
- **CRUD Operations**: (Planned) Modify entities without writing dedicated REST endpoints.
- **Natural Language Querying**: (Planned) Enables users to query and manipulate entity data using natural language, powered by AI integration.

## Usage
Build using `mvn clean install` and add QAdmin as a dependency to your project:

```xml
<dependency>
    <groupId>pl.maliniak</groupId>
    <artifactId>qadmin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

UI is available by default under:
```
http://localhost:8080/qadmin
```



## Architecture

Below is a high-level architecture diagram illustrating how qadmin integrates with a host application.

Note: AI component not yet integrated. WIP
```mermaid
flowchart LR
    %% Styles
    classDef person fill:#0d47a1,stroke:#000,stroke-width:2px,color:#fff;
    classDef internal fill:#e3f2fd,stroke:#1565c0,stroke-width:2px,color:#000;
    classDef external fill:#eeeeee,stroke:#9e9e9e,stroke-width:2px,color:#000;
    classDef qadmin fill:#fff9c4,stroke:#fbc02d,stroke-width:2px,stroke-dasharray: 5 5,color:#000;

    %% Nodes
    User((User)):::person
    
    subgraph Host_App [Host Application]
        direction TB
        
        subgraph QAdmin_Lib [QAdmin Library]
            direction TB
            UI[Angular UI<br/>]:::internal
            Core[Core Service<br/>]:::internal
            AIService[AI Service<br/>]:::internal
        end
        
        AppLogic[Host Business Logic]:::internal
    end

    Database[(Host Database<br/>JPA Entities)]:::external
    LLM[External LLM<br/>]:::external

    %% Relationships
    User -- "Views Data / Asks Query" --> UI
    
    UI -- "Fetch Entities" --> Core
    UI -- "Natural Language Request" --> AIService
    
    Core -- "Reads (JPA)" --> Database
    AIService -- "Generate Query Context" --> LLM
    
    %% Optional: Connection showing AI uses Core or DB to fulfill request
    AIService -.-> Core
    
    %% Styling for subgraph (make QAdmin distinct)
    style QAdmin_Lib fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style Host_App fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
```

## Development Workflow

For the best developer experience with instant hot-reloads across the entire stack, use two separate terminal windows:

### 1. Backend (Quarkus Dev Mode)
From the root directory, run:
```bash
mvn -pl sample-app quarkus:dev -am
```
*Note: Hot-reload for the extension is enabled via the `<quarkus.extension.working-directory>true</quarkus.extension.working-directory>` property in `sample-app/pom.xml`.*

### 2. Frontend (Angular Dev Server)
From the `webui` directory, run:
```bash
npm start
```
*This starts the Angular dev server on `http://localhost:4200` with Hot Module Replacement (HMR). API calls to `/q/qadmin/api` are seamlessly proxied to your Quarkus dev server on port `8080`.*