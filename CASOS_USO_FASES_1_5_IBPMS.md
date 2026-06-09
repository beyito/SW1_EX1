# Casos de Uso Consolidados - Fases 1 a 5 iBPMS

Este documento consolida los casos de uso generados durante las fases 1 a 5 del proyecto iBPMS. Algunos casos de uso fueron agrupados para representar mejor el flujo real del sistema y evitar duplicidades.


## CU-01: Completar Requisitos Iniciales antes de Iniciar Trámite

- **Actor principal:** Cliente móvil.
- **Objetivo:** adjuntar los documentos requeridos antes de crear una instancia de trámite.
- **Precondición:** el cliente seleccionó un trámite iniciable que tiene requisitos iniciales configurados.
- **Flujo principal:** la app móvil muestra los requisitos iniciales, el cliente adjunta los archivos correspondientes, el sistema valida los requisitos obligatorios, crea la instancia del trámite y almacena los documentos asociados.
- **Postcondición:** el trámite queda iniciado y los documentos requeridos quedan asociados a la instancia creada.
- **Excepción:** si falta un requisito obligatorio o un archivo no se puede subir, el sistema impide iniciar el trámite y muestra el motivo.

## CU-02: Asignar Privilegios Documentales

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** definir qué funcionarios pueden ver, editar o eliminar documentos del DMS.
- **Precondición:** existe al menos un documento registrado en el sistema documental.
- **Flujo principal:** el administrador abre el panel de privilegios documentales, selecciona un documento, asigna permisos por funcionario y guarda los cambios.
- **Postcondición:** el sistema persiste permisos granulares de acceso documental para controlar acciones de visualización, edición y eliminación.
- **Excepción:** si el documento no existe o el funcionario no pertenece a la empresa, el sistema rechaza la asignación.

## CU-03: Visualizar Documento con Control de Acceso

- **Actor principal:** Funcionario.
- **Objetivo:** abrir un documento permitido sin descargarlo automáticamente.
- **Precondición:** el funcionario tiene permiso de visualización o cuenta con un rol administrativo autorizado.
- **Flujo principal:** el funcionario abre un documento desde la interfaz, el backend valida sus permisos, genera una URL segura de lectura y registra el evento de auditoría.
- **Postcondición:** el documento se visualiza en la interfaz y queda registrado el evento `VIEW`.
- **Excepción:** si el funcionario no tiene permiso de visualización, el sistema bloquea el acceso y muestra un mensaje entendible.

## CU-04: Editar Documento con OnlyOffice

- **Actor principal:** Funcionario autorizado.
- **Objetivo:** editar documentos Word o Excel de forma colaborativa en tiempo real.
- **Precondición:** el funcionario tiene permiso de edición y el documento es compatible con OnlyOffice.
- **Flujo principal:** el funcionario presiona `Iniciar edición`, Angular solicita la configuración al backend, Spring Boot genera la configuración oficial de OnlyOffice con URL prefirmada de S3 y callback de guardado, Angular carga el script de OnlyOffice y crea la sesión de edición colaborativa.
- **Postcondición:** el documento se abre en OnlyOffice y los cambios se guardan mediante callback hacia el backend y S3.
- **Excepción:** si el documento no es compatible, el usuario no tiene permiso de edición o OnlyOffice no está disponible, el sistema informa el problema y no inicia la sesión colaborativa.

## CU-05: Consultar Auditoría Documental

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** revisar quién vio, editó o eliminó documentos.
- **Precondición:** existen eventos de auditoría documental registrados.
- **Flujo principal:** el administrador abre el módulo de auditoría documental, consulta los eventos disponibles y el sistema muestra usuario, acción, documento y fecha del evento.
- **Postcondición:** la empresa cuenta con trazabilidad sobre las acciones realizadas en sus documentos.
- **Excepción:** si no existen eventos registrados, el sistema muestra una lista vacía o un mensaje indicando que no hay auditoría disponible.

## CU-06: Solicitar Recomendación de Trámite en Lenguaje Natural

- **Actor principal:** Cliente móvil.
- **Objetivo:** encontrar trámites candidatos sin conocer el nombre exacto de la policy.
- **Precondición:** el cliente inició sesión y existen trámites iniciables disponibles para su contexto.
- **Flujo principal:** el cliente escribe o dicta una necesidad, la app móvil envía el texto al backend, Spring Boot consulta el catálogo de policies permitidas y solicita al motor IA recomendaciones de trámites candidatos.
- **Postcondición:** el sistema devuelve una lista de trámites candidatos con nivel de confianza y requisitos iniciales asociados.
- **Excepción:** si el texto no permite identificar candidatos, el sistema muestra una respuesta indicando que no se encontraron trámites recomendados.

## CU-07: Consultar Análisis Predictivo Operativo

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** identificar riesgos operativos de su empresa, incluyendo anomalías, cuellos de botella, prioridades de instancias y rutas probables.
- **Precondición:** existen procesos, tareas o documentos históricos asociados a policies de la empresa autenticada.
- **Flujo principal:** el administrador abre el panel de predicción, Spring Boot extrae el event log filtrado por empresa, FastAPI calcula anomalías con TensorFlow o fallback heurístico, estima prioridades, predice rutas probables y detecta cuellos de botella.
- **Postcondición:** el administrador visualiza señales de riesgo y patrones operativos sin acceder a información de otras empresas.
- **Excepción:** si no existe histórico suficiente, el sistema muestra advertencias y puede usar cálculo heurístico o indicar que no hay datos disponibles.

## CU-08: Generar Reporte Dinámico

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** generar y descargar reportes operativos en PDF, Excel o CSV a partir de una solicitud en lenguaje natural.
- **Precondición:** la solicitud incluye datos a reportar, criterios de filtrado y formato de salida.
- **Flujo principal:** el administrador escribe una solicitud de reporte, Spring Boot envía el contexto filtrado por empresa al agente analítico de FastAPI, el agente valida datos, criterios y formato, estructura un plan de reporte y Spring Boot renderiza el archivo solicitado.
- **Postcondición:** el administrador descarga un reporte dinámico generado a partir de los datos de su empresa.
- **Excepción:** si la solicitud no incluye datos, criterios o formato, el agente analítico solicita la información faltante y no genera el archivo.

## Reglas Transversales

- Los clientes solo pueden iniciar trámites desde la aplicación móvil.
- Los administradores de empresa solo consultan datos, documentos, predicciones y reportes de su propia empresa.
- Los funcionarios solo pueden visualizar o editar documentos si tienen privilegios asignados.
- La auditoría documental registra acciones relevantes como `VIEW`, `EDIT` y `DELETE`.
- El motor IA no consulta directamente la base de datos; Spring Boot filtra y envía el contexto permitido.
- Los reportes dinámicos no se generan si falta alguno de estos elementos: datos, criterios o formato.
