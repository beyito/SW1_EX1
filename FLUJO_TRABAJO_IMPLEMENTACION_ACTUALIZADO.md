# 5. Flujo de Trabajo: Implementación

## 5.1 Elección de Plataforma de Desarrollo del Software

### 5.1.1 Lenguaje de Programación

El sistema adopta una arquitectura políglota orientada a responsabilidades, donde cada tecnología cumple un rol específico dentro de la plataforma iBPMS:

- **Java 21** como lenguaje principal del backend, implementado con **Spring Boot** para la capa de servicios, seguridad, orquestación de procesos, APIs de negocio, gestión documental, integración con S3, integración con OnlyOffice, predicciones y generación de reportes.
- **TypeScript** para el cliente web, implementado sobre **Angular**, con un enfoque basado en componentes y servicios para administración empresarial, modelado colaborativo BPMN, ejecución operativa, gestión documental, predicciones y reportes dinámicos.
- **Python 3.11** para el microservicio de inteligencia artificial `bpmn-ai-engine`, implementado con **FastAPI** para procesamiento de lenguaje natural, recomendación inteligente de trámites, asistencia al modelado BPMN, análisis predictivo y planificación semántica de reportes.
- **Dart** para el cliente móvil, implementado con **Flutter**, utilizado por los clientes para iniciar trámites, consultar requisitos iniciales, cargar documentos, recibir recomendaciones del agente inteligente y dar seguimiento a tareas o procesos.

La selección de estos lenguajes responde al principio de especialización tecnológica por dominio funcional:

- Backend transaccional robusto y seguro con Java/Spring Boot.
- Frontend web altamente interactivo con Angular/TypeScript.
- Motor IA desacoplado y extensible con Python/FastAPI.
- Cliente móvil multiplataforma con Flutter/Dart.

Esta arquitectura permite evolucionar el sistema hacia un iBPMS modular, manteniendo separación entre la lógica de negocio, la experiencia de usuario, el motor de inteligencia artificial y los servicios externos.

### 5.1.2 Base de Datos

Se utiliza **MongoDB Atlas** como sistema de gestión de base de datos NoSQL orientado a documentos. Esta decisión se justifica por:

- La naturaleza semiestructurada del dominio BPMN, especialmente en campos como `diagramJson`, `lanes`, metadatos de nodos y reglas de transición.
- La necesidad de evolucionar esquemas con baja fricción durante las fases de análisis, diseño e implementación.
- La flexibilidad para almacenar estructuras embebidas como `initialRequirements[]`, `lanes[]` y `messages[]`.
- El soporte administrado de escalabilidad, replicación y alta disponibilidad.
- La facilidad de integración con **Spring Data MongoDB** mediante repositorios declarativos.

El acceso a datos se implementa mediante **MongoRepository**, manteniendo una separación clara entre servicios de dominio y persistencia.

Las colecciones actuales principales son:

- `users`
- `companies`
- `areas`
- `policies`
- `process_instances`
- `task_instances`
- `documents`
- `document_permissions`
- `document_audit_logs`
- `copilot_conversations`

Con la evolución de las 5 fases, el modelo de datos fue ampliado para soportar:

- Requisitos iniciales obligatorios u opcionales en `policies.initialRequirements[]`.
- Registro documental en `documents`.
- Privilegios granulares por documento en `document_permissions`.
- Auditoría documental en `document_audit_logs`.
- Datos históricos de procesos, tareas y documentos para análisis predictivo y reportes dinámicos.

El motor de IA no accede directamente a MongoDB. Spring Boot actúa como gateway de datos, filtra la información por empresa y envía al microservicio IA únicamente el contexto permitido.

### 5.1.3 Sistemas Operativos

La solución es compatible con entornos modernos de **Windows**, **Linux** y **macOS** durante el desarrollo, debido al soporte multiplataforma de Java, Node.js, Python, Flutter y Docker.

En producción, la arquitectura está orientada a un despliegue sobre **Linux**, preferentemente en una instancia **AWS EC2** o infraestructura equivalente, donde los servicios principales se ejecutan mediante contenedores Docker.

El entorno productivo contempla:

- Contenedor del frontend Angular publicado con Nginx.
- Contenedor del backend Spring Boot.
- Contenedor del motor IA FastAPI.
- Contenedor de OnlyOffice Document Server para edición colaborativa.
- Contenedor RabbitMQ para STOMP relay cuando se habilita colaboración BPMN distribuida.
- Integración externa con MongoDB Atlas y AWS S3.

Docker Compose permite ejecutar de forma coordinada los servicios necesarios para pruebas locales y despliegues controlados.

### 5.1.4 Otros Componentes Tecnológicos

La implementación incorpora componentes complementarios críticos para la seguridad, colaboración, gestión documental, inteligencia artificial y operación del sistema:

- **Spring Boot:** framework principal del backend para APIs REST, GraphQL, seguridad, servicios de dominio, ejecución de procesos, DMS, predicción y reportes.
- **Spring Security + JWT Bearer:** autenticación y autorización basada en roles como `SOFTWARE_ADMIN`, `COMPANY_ADMIN`, `FUNCTIONARY`, `FUNCIONARIO` y `CLIENT`.
- **Spring Data MongoDB:** acceso a persistencia mediante repositorios declarativos.
- **GraphQL (Spring GraphQL):** capa de consulta y mutación para operaciones que requieren estructuras flexibles.
- **WebSocket + STOMP:** sincronización colaborativa del diseño BPMN en tiempo real.
- **RabbitMQ STOMP Relay:** broker de mensajería opcional para distribuir eventos de colaboración entre clientes concurrentes.
- **Angular + JointJS Plus:** interfaz web para modelado BPMN colaborativo con carriles, nodos, enlaces, propiedades y requisitos iniciales.
- **Flutter:** aplicación móvil para clientes, inicio de trámites, carga de requisitos iniciales, recepción inteligente y seguimiento operativo.
- **FastAPI:** microservicio IA desacoplado para copilot BPMN, agente de recepción inteligente, predicción y agente analítico de reportes.
- **LLM externo:** proveedor de lenguaje natural usado por el motor IA para interpretar solicitudes, recomendar trámites y planificar reportes.
- **TensorFlow/Keras o fallback heurístico:** soporte para análisis predictivo, detección de anomalías, prioridades, rutas probables y cuellos de botella.
- **AWS S3:** almacenamiento seguro de documentos, evidencias y requisitos iniciales.
- **Presigned URLs de S3:** acceso temporal y controlado a documentos para lectura, visualización y edición colaborativa.
- **OnlyOffice Document Server:** motor externo para edición colaborativa en tiempo real de documentos Word, Excel y formatos compatibles.
- **Firebase Cloud Messaging (FCM):** notificaciones push para clientes móviles o funcionarios cuando se asignan tareas.
- **Apache POI / generación interna de archivos:** generación de reportes descargables en Excel, CSV y PDF.
- **Docker / Docker Compose:** empaquetado y orquestación de servicios locales y productivos.
- **Nginx:** servidor web para publicar Angular y actuar como proxy inverso hacia backend, GraphQL y WebSocket.
- **Git:** control de versiones y colaboración del código fuente.

## 5.2 Arquitectura Implementada

El proyecto adopta una arquitectura en capas y servicios desacoplados, con responsabilidades estrictamente diferenciadas.

### 5.2.1 Capa de Presentación

Compuesta por:

- **Angular Web:** utilizada por administradores de software, administradores de empresa y funcionarios.
- **Flutter Mobile:** utilizada por clientes para iniciar trámites y completar requisitos iniciales.

La capa de presentación no accede directamente a la base de datos ni a S3. Todas las operaciones pasan por el backend Spring Boot.

### 5.2.2 Capa de Aplicación y Dominio

Implementada en **Spring Boot**, concentra la lógica de negocio:

- Autenticación y autorización.
- Administración de empresas, áreas y usuarios.
- Gestión y diseño de políticas BPMN.
- Ejecución de procesos y tareas.
- Validación de requisitos iniciales.
- Gestión documental DMS.
- Control de privilegios documentales.
- Auditoría documental.
- Integración con S3.
- Integración con OnlyOffice.
- Integración con el motor IA.
- Generación de reportes.

El backend sigue una organización basada en el patrón:

```text
Controller -> Service -> Repository
```

Esta estructura permite separar la exposición de APIs, la lógica de negocio y el acceso a datos.

### 5.2.3 Capa de Persistencia

Implementada mediante **MongoDB Atlas** y **Spring Data MongoDB**.

Los repositorios permiten consultas declarativas sobre entidades como:

- `User`
- `Company`
- `Area`
- `Policy`
- `ProcessInstance`
- `TaskInstance`
- `DocumentRecord`
- `DocumentPermission`
- `DocumentAuditLog`
- `CopilotConversation`

La persistencia documental se complementa con **AWS S3**, donde se almacenan físicamente los archivos. MongoDB conserva los metadatos, permisos, auditoría y referencias mediante `s3Key`.

### 5.2.4 Capa de Inteligencia Artificial

Implementada como microservicio independiente en **FastAPI**.

Sus responsabilidades principales son:

- Asistencia inteligente para el modelado BPMN.
- Recomendación de trámites a partir de lenguaje natural.
- Extracción de información desde texto o voz.
- Análisis predictivo de procesos, tareas y documentos.
- Planificación semántica de reportes dinámicos.

Spring Boot actúa como gateway de seguridad y datos, enviando al motor IA únicamente la información necesaria y filtrada por empresa.

### 5.2.5 Capa de Servicios Externos

Incluye:

- **MongoDB Atlas** para persistencia NoSQL.
- **AWS S3** para almacenamiento documental.
- **OnlyOffice Document Server** para edición colaborativa.
- **RabbitMQ** para mensajería STOMP.
- **Firebase Cloud Messaging** para notificaciones push.
- **Proveedor LLM** para procesamiento de lenguaje natural.

## 5.3 Funcionalidades Principales del Sistema Actual

La plataforma iBPMS integra modelado BPMN, ejecución de trámites, gestión documental, inteligencia artificial, predicción y reportes dinámicos dentro de un flujo operativo unificado.

### 5.3.1 Gestión de Políticas BPMN y Requisitos Iniciales

Las políticas BPMN representan los trámites que puede ejecutar la organización. Cada política almacena su diagrama en `diagramJson`, sus carriles en `lanes[]` y sus requisitos iniciales en `initialRequirements[]`.

Los requisitos iniciales permiten definir documentos obligatorios u opcionales que el cliente debe cargar antes de iniciar un trámite desde la aplicación móvil. Cada requisito puede tener nombre, descripción, obligatoriedad y extensiones permitidas.

El modelador Angular permite al administrador de empresa definir estos requisitos junto con el flujo BPMN. La aplicación móvil consume esa información y muestra al cliente los archivos requeridos antes de crear la instancia del trámite.

Los documentos asociados al inicio del trámite se almacenan físicamente en AWS S3 siguiendo la estructura:

```text
clientes/{clientId}/tramites/{processInstanceId}/{documentId}_{fileName}
```

### 5.3.2 Ejecución Operativa de Procesos

El motor de ejecución en Spring Boot permite iniciar procesos a partir de una política BPMN, crear instancias de tareas, asignarlas por carril o usuario y avanzar el flujo de acuerdo con las transiciones definidas en el diagrama.

Los funcionarios trabajan desde la interfaz web, donde consultan bandejas de tareas, toman actividades, completan formularios y visualizan el estado de los procesos activos. Los clientes utilizan la aplicación móvil para iniciar trámites, cargar requisitos iniciales y consultar tareas que les correspondan.

### 5.3.3 Gestión Documental DMS

El sistema cuenta con un módulo de gestión documental que registra metadatos en MongoDB y almacena archivos en AWS S3.

Las colecciones principales del DMS son:

- `documents`: metadatos del documento, relación con trámite, política, cliente y ruta S3.
- `document_permissions`: privilegios granulares por usuario y documento.
- `document_audit_logs`: trazabilidad de acciones documentales.

Los privilegios documentales controlan si un funcionario puede visualizar, editar o eliminar un documento mediante permisos `VIEW`, `EDIT` y `DELETE`.

La auditoría registra acciones relevantes sobre documentos, incluyendo usuario, acción, documento, método HTTP, ruta y fecha.

### 5.3.4 Edición Colaborativa de Documentos

La edición colaborativa se realiza mediante OnlyOffice Document Server. Angular solicita al backend la configuración del editor, Spring Boot valida permisos, genera URLs prefirmadas de S3 y entrega la configuración necesaria para abrir el documento.

OnlyOffice descarga el archivo original mediante una URL prefirmada y, cuando existen cambios guardables, envía un callback al backend. Spring Boot procesa el callback, descarga la versión editada y reemplaza el archivo original en S3.

Esta integración permite edición colaborativa real en documentos Word, Excel y formatos compatibles, manteniendo el control de acceso y la auditoría dentro del backend.

### 5.3.5 Agente de Recepción Inteligente

La aplicación móvil permite que el cliente describa una necesidad en lenguaje natural. Spring Boot recibe el texto, filtra las políticas iniciables y envía el contexto permitido al motor IA FastAPI.

El motor IA compara la intención del usuario con el catálogo de políticas y devuelve una lista de trámites candidatos con nivel de confianza y requisitos faltantes. El cliente selecciona el trámite más conveniente y continúa por el mismo flujo normal de inicio de trámite, completando los requisitos iniciales obligatorios antes de instanciar el proceso.

### 5.3.6 Predicción y Análisis de Procesos

Los administradores de empresa cuentan con un panel de predicción basado en el histórico de procesos, tareas y documentos de su propia empresa.

Spring Boot construye un event log con datos filtrados por empresa y lo envía al motor IA. El análisis permite:

- Detectar anomalías en tiempos, rutas o carga documental.
- Estimar prioridades de instancias.
- Predecir rutas probables.
- Identificar posibles cuellos de botella.

El análisis puede apoyarse en TensorFlow/Keras cuando el entorno lo permite, o utilizar un mecanismo heurístico de respaldo para mantener disponibilidad funcional.

### 5.3.7 Reportes Dinámicos

El módulo de reportes dinámicos permite que el administrador de empresa solicite reportes en lenguaje natural.

La solicitud debe contener:

- Datos.
- Criterios.
- Formato.

Si falta alguno de estos elementos, el agente solicita una aclaración antes de generar el archivo.

Angular captura la solicitud, Spring Boot recopila datos filtrados por empresa, FastAPI interpreta el pedido y estructura un plan de reporte. Finalmente, Spring Boot genera el archivo en PDF, Excel o CSV y lo devuelve al usuario para descarga.

## 5.4 Principios de Diseño Aplicados

El sistema mantiene los siguientes principios:

- **Separación de responsabilidades:** cada capa cumple una función clara.
- **Inyección de dependencias:** aplicada mediante Spring y Angular.
- **Bajo acoplamiento:** el motor IA, S3, OnlyOffice y notificaciones se integran mediante servicios especializados.
- **Alta cohesión:** cada servicio concentra reglas de un dominio concreto.
- **Programación defensiva:** validación de usuarios, permisos, formatos, callbacks y errores externos.
- **Seguridad por roles:** acceso restringido según perfil.
- **Aislamiento multiempresa:** los administradores de empresa solo acceden a datos de su propia empresa.
- **Trazabilidad:** las acciones documentales quedan auditadas.
- **Extensibilidad:** nuevas predicciones, reportes o motores IA pueden integrarse sin reescribir el core transaccional.

## 5.5 Flujo General de Control

El flujo de control principal queda organizado de la siguiente manera:

```text
Angular / Flutter
        ↓
Controller Spring Boot
        ↓
Service Spring Boot
        ↓
Repository MongoDB / S3Service / AI Gateway / OnlyOffice Gateway
        ↓
MongoDB Atlas / AWS S3 / FastAPI / OnlyOffice / Firebase
```

Esta organización permite que Spring Boot permanezca como núcleo transaccional y de seguridad, mientras que FastAPI, OnlyOffice, S3 y Firebase actúan como servicios especializados integrados mediante interfaces controladas.
