# Dashboard de Prazos e Demandas

Local dashboard developed to manage deadlines and demands from a Real Estate Registry Office.

The application imports Excel reports, processes the information locally and displays indicators, charts, filters and critical demands in a web dashboard.

The project follows a local-first architecture so sensitive data can remain on the user's computer.

## Tech Stack

### Backend

- Java 17
- Spring Boot
- Spring Data JPA
- Hibernate
- Maven
- Apache POI
- SQLite

### Frontend

- React
- JavaScript
- Vite
- Recharts
- CSS

### Desktop / Distribution

- jpackage
- WiX Toolset
- Embedded Java Runtime
- Windows installer

## Main Features

- Excel file import
- Automatic Excel header detection
- Data preview before import
- Configurable column mapping
- Duplicate demand detection
- Import history
- Local SQLite persistence
- Dashboard indicators
- Critical demand monitoring
- Deadline status calculation
- Demand filtering
- Distribution charts
- Local browser interface
- Automatic browser opening
- Windows installer
- Embedded Java Runtime
- Detection of an already running instance

## Dashboard Indicators

The dashboard displays information such as:

- Total demands
- Overdue demands
- Demands close to deadline
- Demands within deadline
- Completed demands
- Demands without deadline

It also contains charts for:

- Status distribution
- Sector distribution
- Demand type distribution
- Delay ranges

## Architecture

The application follows a layered architecture:

```text
Excel Report
     |
     v
Apache POI
     |
     v
Spring Boot
     |
     v
Services
     |
     v
JPA / Hibernate
     |
     v
SQLite
     |
     v
REST API
     |
     v
React Dashboard
```

The React production build is included inside the Spring Boot application and served locally by the backend.

Because of this, the final user does not need to install Node.js or run a separate frontend server.

## Local-First Approach

The application does not connect to the external system that generates the original reports.

There is:

- No external system integration
- No external API
- No cloud database
- No automatic data transmission

Excel reports are manually selected by the user and processed locally.

The SQLite database is stored in the Windows user directory:

```text
AppData/Local/DashboardDemandas/data/dashboard.db
```

This keeps application data outside the installation directory and allows local persistence without requiring a separate database server.

## Excel Import

The import module was designed to support different report structures through configurable column mapping.

The import workflow is:

```text
Select Excel file
        |
        v
Read headers
        |
        v
Preview data
        |
        v
Map columns
        |
        v
Validate records
        |
        v
Import demands
        |
        v
Store in SQLite
        |
        v
Update dashboard
```

Apache POI is used to read Excel files.

The system can also identify duplicate records and store information about previous imports.

## Windows Application

The application can be distributed as a Windows desktop application.

The installer includes its own Java Runtime, so the final user does not need to install:

- Java
- Node.js
- npm
- Maven
- IntelliJ IDEA

The installed application starts the Spring Boot server locally and automatically opens the dashboard in the default browser.

```text
DashboardDemandas.exe
        |
        v
Embedded Java Runtime
        |
        v
Spring Boot
        |
        v
SQLite
        |
        v
http://localhost:8080
```

If the application is already running and the user opens it again, the system detects the existing instance and opens the dashboard instead of trying to start another server.

## Data Persistence

Application data is stored locally using SQLite.

The database is not stored inside the installation directory.

On Windows, the database is created in:

```text
AppData/Local/DashboardDemandas/data/dashboard.db
```

This allows the data to remain available after:

- Refreshing the browser
- Closing the browser
- Opening the application again
- Restarting the application

## Current Version

```text
1.0.1
```

The current version includes:

- Functional Windows installer
- Embedded Java Runtime
- SQLite persistence
- Excel import
- Import history
- Dashboard charts and indicators
- Automatic browser opening
- Single-instance detection
- Local data processing

## Project Status

The application is currently functional with test data.

The next stage is validation using a real exported Excel report from the registry office.

This validation will be used to evaluate possible differences in:

- Column names
- Date formats
- Available fields
- Report structure
- Data used by dashboard indicators

Because the import module uses configurable column mapping, the application is not limited to a single fixed Excel layout.

## Data Privacy

No real registry office data is stored in this repository.

The following files are excluded from version control:

- SQLite databases
- Imported reports
- Build artifacts
- Node.js dependencies
- Generated frontend builds
- Windows installers
- Packaging files

The repository contains only the application source code and fictitious/test structures.

## Repository Structure

```text
dashboard-demandas/
|
|-- frontend/
|   |-- src/
|   |-- public/
|   |-- package.json
|   `-- vite.config.js
|
|-- src/
|   |-- main/
|   |   |-- java/
|   |   `-- resources/
|   |
|   `-- test/
|
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
|-- .gitignore
`-- README.md
```

## Purpose

This project was developed as part of a Software Engineering academic project and also serves as a practical portfolio project involving:

- Java backend development
- REST APIs
- Database persistence
- Excel processing
- React frontend development
- Desktop packaging
- Local application architecture

## Author

Júlio Körbes da Silva

Software Engineering student focused on Java Backend and Full Stack development.