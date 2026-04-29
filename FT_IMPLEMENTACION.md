# 5.1 Elección de Plataforma de Desarrollo del Software

## 5.1.1 Lenguaje de Programación
El sistema adopta una arquitectura poliglota orientada a responsabilidades:

- **Java 21** como lenguaje principal del backend, implementado con Spring Boot para la capa de servicios, seguridad, orquestacion de procesos y APIs de negocio.
- **TypeScript** para el cliente web, implementado sobre Angular, con un enfoque basado en componentes y servicios para el modelado colaborativo del lienzo BPMN.
- **Python 3.11** para el microservicio de inteligencia artificial (`bpmn-ai-engine`), implementado con FastAPI para el procesamiento de instrucciones en lenguaje natural y transformacion de diagramas.
- **Dart** para el cliente movil (Flutter), utilizado en la ejecucion operativa de tareas y seguimiento de tramites.

La seleccion de estos lenguajes responde al principio de especializacion tecnologica por dominio funcional: backend transaccional robusto, frontend altamente interactivo, servicios IA desacoplados y cliente movil multiplataforma.

## 5.1.2 Base de Datos
La plataforma utiliza **MongoDB Atlas** como sistema de gestion de base de datos NoSQL orientado a documentos.  
Esta decision se justifica por:

1. La naturaleza semiestructurada del dominio BPMN (`diagramJson`, `lanes`, metadatos de nodos y enlaces).
2. La necesidad de evolucionar esquemas con baja friccion durante iteraciones de analisis y diseno.
3. El soporte nativo para escalabilidad horizontal, replicacion y alta disponibilidad administrada.

El acceso a datos se implementa mediante **Spring Data MongoDB** (`MongoRepository`), permitiendo consultas declarativas y mantenimiento consistente de colecciones como `users`, `policies`, `process_instances`, `task_instances`, `areas`, `companies` y `copilot_conversations`.

## 5.1.3 Sistemas Operativos
La solucion es compatible con entornos modernos de **Windows, Linux y macOS** en fase de desarrollo, debido al soporte multiplataforma de Java, Node.js (tooling frontend), Python y Docker.

En produccion, la arquitectura esta orientada a despliegue sobre **Linux en instancia AWS EC2**, donde los servicios principales se ejecutan en contenedores Docker.

## 5.1.4 Otros Componentes Tecnologicos
La implementacion incorpora componentes complementarios para seguridad, colaboracion y operacion:

- **Spring Boot**: framework principal del backend para APIs REST/GraphQL, seguridad y servicios de dominio.
- **Spring Security + JWT Bearer**: control de autenticacion y autorizacion por roles (`SOFTWARE_ADMIN`, `COMPANY_ADMIN`, `FUNCTIONARY`, `CLIENT`).
- **GraphQL (Spring GraphQL)**: capa de consulta/mutacion para operaciones de politicas y tareas.
- **WebSocket + STOMP**: sincronizacion colaborativa del diseno BPMN en tiempo real.
- **RabbitMQ (STOMP Relay)**: broker de mensajeria para distribucion de eventos de colaboracion entre clientes concurrentes.
- **Angular + JointJS**: interfaz web para modelado BPMN colaborativo con carriles, nodos y enlaces.
- **FastAPI (microservicio IA)**: integracion de asistentes de edicion y aplicacion automatica de cambios en diagramas.
- **AWS S3**: almacenamiento de adjuntos de formularios de tareas.
- **Firebase Cloud Messaging (FCM)**: notificaciones push para asignacion de tareas en cliente movil.
- **Docker / Docker Compose**: empaquetado y orquestacion local/productiva de frontend, backend, IA y mensajeria.
- **Nginx**: publicacion del frontend y proxy inverso hacia backend y WebSocket.
- **Git**: control de versiones y colaboracion de codigo fuente.

## 5.1.5 Estilo Arquitectonico y Organizacion del Codigo
El proyecto adopta una arquitectura en capas con responsabilidades diferenciadas:

1. **Capa de Presentacion** (Angular Web / Flutter Mobile).
2. **Capa de Aplicacion y Dominio** (Spring Boot Services, Workflow Engine, Copilot Gateway).
3. **Capa de Persistencia** (MongoRepository sobre MongoDB Atlas).

Adicionalmente, el sistema aplica **inyeccion de dependencias** como mecanismo estructural central (Spring/Angular), promoviendo bajo acoplamiento, extensibilidad y testabilidad.  
En la practica, el flujo de control sigue una organizacion tipo **Controller-Service-Repository**, alineada al principio de separacion de responsabilidades.

