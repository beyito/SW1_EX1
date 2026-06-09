# 6.2 Prueba de Caja Negra

Este documento define pruebas de caja negra para los casos de uso consolidados de las fases 1 a 5 del sistema iBPMS.

Las pruebas se enfocan en validar el comportamiento observable del sistema desde la perspectiva del usuario, sin considerar la implementación interna.

---

## CP-01

| Atributo | Descripción |
|---|---|
| **ID de Prueba** | CP-01 |
| **Caso de Uso** | CU-01: Completar Requisitos Iniciales antes de Iniciar Trámite |
| **Objetivo** | Verificar que el cliente no pueda iniciar un trámite si falta un requisito inicial obligatorio. |
| **Precondiciones** | El cliente móvil inició sesión. Existe un trámite iniciable con al menos un requisito inicial obligatorio configurado. |
| **Pasos de Ejecución** | 1. Ingresar a la aplicación móvil. <br> 2. Seleccionar un trámite iniciable con requisitos obligatorios. <br> 3. Dejar sin adjuntar uno de los documentos obligatorios. <br> 4. Presionar el botón para iniciar el trámite. |
| **Resultado Esperado** | El sistema bloquea el inicio del trámite y muestra un mensaje indicando que falta completar un requisito obligatorio. |
| **Resultado Obtenido** | Pendiente de ejecución. |
| **Estado** | Pendiente. |

---

## CP-02

| Atributo | Descripción |
|---|---|
| **ID de Prueba** | CP-02 |
| **Caso de Uso** | CU-02: Asignar Privilegios Documentales |
| **Objetivo** | Verificar que el administrador de empresa pueda asignar permisos de visualización, edición y eliminación sobre un documento. |
| **Precondiciones** | El administrador de empresa inició sesión. Existe al menos un documento registrado en el DMS y existe al menos un funcionario de la misma empresa. |
| **Pasos de Ejecución** | 1. Ingresar al panel de administración de empresa. <br> 2. Abrir el apartado de privilegios documentales. <br> 3. Seleccionar un documento. <br> 4. Seleccionar un funcionario. <br> 5. Marcar permisos de ver, editar o eliminar. <br> 6. Guardar los cambios. |
| **Resultado Esperado** | El sistema guarda los permisos seleccionados y muestra confirmación de actualización. Al consultar nuevamente el documento, los permisos asignados permanecen registrados. |
| **Resultado Obtenido** | Pendiente de ejecución. |
| **Estado** | Pendiente. |

---

## CP-03

| Atributo | Descripción |
|---|---|
| **ID de Prueba** | CP-03 |
| **Caso de Uso** | CU-03: Visualizar Documento con Control de Acceso |
| **Objetivo** | Verificar que un funcionario sin permiso de visualización no pueda abrir un documento protegido. |
| **Precondiciones** | El funcionario inició sesión. Existe un documento registrado en el DMS. El funcionario no tiene permiso `VIEW` sobre dicho documento. |
| **Pasos de Ejecución** | 1. Ingresar al sistema como funcionario. <br> 2. Abrir el listado de documentos de un trámite. <br> 3. Intentar visualizar un documento sin permiso asignado. |
| **Resultado Esperado** | El sistema bloquea la visualización del documento y muestra un mensaje entendible indicando que el usuario no tiene permisos suficientes. El documento no se descarga automáticamente. |
| **Resultado Obtenido** | Pendiente de ejecución. |
| **Estado** | Pendiente. |

---

## CP-04

| Atributo | Descripción |
|---|---|
| **ID de Prueba** | CP-04 |
| **Caso de Uso** | CU-04: Editar Documento con OnlyOffice |
| **Objetivo** | Verificar que un funcionario autorizado pueda abrir una sesión colaborativa de edición para un documento compatible. |
| **Precondiciones** | El funcionario inició sesión. Existe un documento Word o Excel compatible. El funcionario tiene permiso `EDIT`. OnlyOffice Document Server está disponible. |
| **Pasos de Ejecución** | 1. Ingresar al sistema como funcionario autorizado. <br> 2. Abrir el documento desde la interfaz. <br> 3. Presionar el botón `Iniciar edición`. <br> 4. Esperar la carga del editor OnlyOffice. <br> 5. Modificar el documento. <br> 6. Guardar o esperar el callback automático de guardado. |
| **Resultado Esperado** | El sistema abre el editor OnlyOffice, permite editar el documento colaborativamente y guarda los cambios en el almacenamiento documental. |
| **Resultado Obtenido** | Pendiente de ejecución. |
| **Estado** | Pendiente. |

---

## CP-05

| Atributo | Descripción |
|---|---|
| **ID de Prueba** | CP-05 |
| **Caso de Uso** | CU-05: Consultar Auditoría Documental |
| **Objetivo** | Verificar que el administrador de empresa pueda consultar eventos de auditoría documental. |
| **Precondiciones** | El administrador de empresa inició sesión. Existen eventos de auditoría generados por acciones de visualización, edición o eliminación de documentos. |
| **Pasos de Ejecución** | 1. Ingresar al panel de administración de empresa. <br> 2. Abrir el apartado de auditoría documental. <br> 3. Consultar el listado de eventos. <br> 4. Revisar los datos mostrados para un evento. |
| **Resultado Esperado** | El sistema muestra los eventos de auditoría con usuario, acción, documento y fecha. Solo se visualizan eventos correspondientes a la empresa autenticada. |
| **Resultado Obtenido** | Pendiente de ejecución. |
| **Estado** | Pendiente. |

---

## CP-06

| Atributo | Descripción |
|---|---|
| **ID de Prueba** | CP-06 |
| **Caso de Uso** | CU-06: Solicitar Recomendación de Trámite en Lenguaje Natural |
| **Objetivo** | Verificar que el cliente móvil reciba trámites candidatos al describir una necesidad en lenguaje natural. |
| **Precondiciones** | El cliente móvil inició sesión. Existen trámites iniciables disponibles para su contexto. El motor IA está disponible. |
| **Pasos de Ejecución** | 1. Ingresar a la aplicación móvil. <br> 2. Abrir el asistente o agente de recepción inteligente. <br> 3. Escribir una solicitud, por ejemplo: `Perdí mi tarjeta y necesito una nueva`. <br> 4. Enviar la solicitud. <br> 5. Revisar los trámites candidatos sugeridos. |
| **Resultado Esperado** | El sistema muestra una lista de trámites candidatos con nombre, nivel de confianza y requisitos iniciales asociados. El trámite no se inicia automáticamente; el cliente debe seleccionar una opción. |
| **Resultado Obtenido** | Pendiente de ejecución. |
| **Estado** | Pendiente. |

---

## CP-07

| Atributo | Descripción |
|---|---|
| **ID de Prueba** | CP-07 |
| **Caso de Uso** | CU-07: Consultar Análisis Predictivo Operativo |
| **Objetivo** | Verificar que el administrador de empresa pueda consultar predicciones operativas solo con datos de su empresa. |
| **Precondiciones** | El administrador de empresa inició sesión. Existen procesos, tareas o documentos históricos asociados a políticas de su empresa. |
| **Pasos de Ejecución** | 1. Ingresar al panel de administración de empresa. <br> 2. Abrir el apartado de predicciones. <br> 3. Esperar la carga del análisis operativo. <br> 4. Revisar anomalías, prioridades, rutas probables y cuellos de botella. |
| **Resultado Esperado** | El sistema muestra el análisis predictivo filtrado por empresa, sin exponer datos de otras empresas. Si hay advertencias por datos insuficientes, se muestran de forma entendible. |
| **Resultado Obtenido** | Pendiente de ejecución. |
| **Estado** | Pendiente. |

---

## CP-08

| Atributo | Descripción |
|---|---|
| **ID de Prueba** | CP-08 |
| **Caso de Uso** | CU-08: Generar Reporte Dinámico |
| **Objetivo** | Verificar que el sistema no genere un reporte si la solicitud no contiene datos, criterios y formato. |
| **Precondiciones** | El administrador de empresa inició sesión. Existen datos operativos de la empresa disponibles para reportes. |
| **Pasos de Ejecución** | 1. Ingresar al panel de administración de empresa. <br> 2. Abrir el apartado de predicciones o reportes dinámicos. <br> 3. Escribir una solicitud incompleta, por ejemplo: `Quiero un reporte de RRHH`. <br> 4. Enviar la solicitud. |
| **Resultado Esperado** | El sistema no genera el archivo y muestra una pregunta de aclaración solicitando el dato faltante, el criterio o el formato requerido. |
| **Resultado Obtenido** | Pendiente de ejecución. |
| **Estado** | Pendiente. |

---

# 6.3 Ejecución y Evidencia de Casos de Prueba

## CP-01

| Atributo | Detalle de Ejecución |
|---|---|
| **ID de Prueba** | CP-01 |
| **Caso de Uso** | CU-01: Completar Requisitos Iniciales antes de Iniciar Trámite |
| **Objetivo** | Verificar que el cliente no pueda iniciar un trámite si falta un requisito inicial obligatorio. |
| **Resultado Esperado** | El sistema bloquea el inicio del trámite y muestra un mensaje indicando que falta completar un requisito obligatorio. |
| **Resultado Obtenido** | El sistema impidió iniciar el trámite cuando faltaba un documento obligatorio y mostró un mensaje indicando que se deben completar los requisitos iniciales requeridos. |
| **Evidencia Visual** |  |
| **Estado** | EXITOSO |

---

## CP-02

| Atributo | Detalle de Ejecución |
|---|---|
| **ID de Prueba** | CP-02 |
| **Caso de Uso** | CU-02: Asignar Privilegios Documentales |
| **Objetivo** | Verificar que el administrador de empresa pueda asignar permisos de visualización, edición y eliminación sobre un documento. |
| **Resultado Esperado** | El sistema guarda los permisos seleccionados y muestra confirmación de actualización. Al consultar nuevamente el documento, los permisos asignados permanecen registrados. |
| **Resultado Obtenido** | El sistema guardó correctamente los permisos seleccionados para el funcionario y, al volver a consultar el documento, los privilegios asignados permanecieron registrados. |
| **Evidencia Visual** |  |
| **Estado** | EXITOSO |

---

## CP-03

| Atributo | Detalle de Ejecución |
|---|---|
| **ID de Prueba** | CP-03 |
| **Caso de Uso** | CU-03: Visualizar Documento con Control de Acceso |
| **Objetivo** | Verificar que un funcionario sin permiso de visualización no pueda abrir un documento protegido. |
| **Resultado Esperado** | El sistema bloquea la visualización del documento y muestra un mensaje entendible indicando que el usuario no tiene permisos suficientes. El documento no se descarga automáticamente. |
| **Resultado Obtenido** | El sistema bloqueó el acceso al documento para el funcionario sin permiso, mostró un mensaje claro de falta de privilegios y no descargó el archivo automáticamente. |
| **Evidencia Visual** |  |
| **Estado** | EXITOSO |

---

## CP-04

| Atributo | Detalle de Ejecución |
|---|---|
| **ID de Prueba** | CP-04 |
| **Caso de Uso** | CU-04: Editar Documento con OnlyOffice |
| **Objetivo** | Verificar que un funcionario autorizado pueda abrir una sesión colaborativa de edición para un documento compatible. |
| **Resultado Esperado** | El sistema abre el editor OnlyOffice, permite editar el documento colaborativamente y guarda los cambios en el almacenamiento documental. |
| **Resultado Obtenido** | El sistema abrió el documento en OnlyOffice, permitió realizar la edición colaborativa y procesó el guardado del documento mediante el callback hacia el backend y el almacenamiento documental. |
| **Evidencia Visual** |  |
| **Estado** | EXITOSO |

---

## CP-05

| Atributo | Detalle de Ejecución |
|---|---|
| **ID de Prueba** | CP-05 |
| **Caso de Uso** | CU-05: Consultar Auditoría Documental |
| **Objetivo** | Verificar que el administrador de empresa pueda consultar eventos de auditoría documental. |
| **Resultado Esperado** | El sistema muestra los eventos de auditoría con usuario, acción, documento y fecha. Solo se visualizan eventos correspondientes a la empresa autenticada. |
| **Resultado Obtenido** | El sistema mostró el listado de eventos de auditoría documental con usuario, acción, documento y fecha, manteniendo la información limitada al contexto de la empresa autenticada. |
| **Evidencia Visual** |  |
| **Estado** | EXITOSO |

---

## CP-06

| Atributo | Detalle de Ejecución |
|---|---|
| **ID de Prueba** | CP-06 |
| **Caso de Uso** | CU-06: Solicitar Recomendación de Trámite en Lenguaje Natural |
| **Objetivo** | Verificar que el cliente móvil reciba trámites candidatos al describir una necesidad en lenguaje natural. |
| **Resultado Esperado** | El sistema muestra una lista de trámites candidatos con nombre, nivel de confianza y requisitos iniciales asociados. El trámite no se inicia automáticamente; el cliente debe seleccionar una opción. |
| **Resultado Obtenido** | El sistema devolvió trámites candidatos relacionados con la solicitud escrita por el cliente, mostrando sus datos principales y permitiendo que el usuario seleccione manualmente el trámite antes de completar los requisitos iniciales. |
| **Evidencia Visual** |  |
| **Estado** | EXITOSO |

---

## CP-07

| Atributo | Detalle de Ejecución |
|---|---|
| **ID de Prueba** | CP-07 |
| **Caso de Uso** | CU-07: Consultar Análisis Predictivo Operativo |
| **Objetivo** | Verificar que el administrador de empresa pueda consultar predicciones operativas solo con datos de su empresa. |
| **Resultado Esperado** | El sistema muestra el análisis predictivo filtrado por empresa, sin exponer datos de otras empresas. Si hay advertencias por datos insuficientes, se muestran de forma entendible. |
| **Resultado Obtenido** | El sistema cargó el panel de predicciones mostrando anomalías, prioridades, rutas probables y cuellos de botella calculados únicamente con información de la empresa autenticada. |
| **Evidencia Visual** |  |
| **Estado** | EXITOSO |

---

## CP-08

| Atributo | Detalle de Ejecución |
|---|---|
| **ID de Prueba** | CP-08 |
| **Caso de Uso** | CU-08: Generar Reporte Dinámico |
| **Objetivo** | Verificar que el sistema no genere un reporte si la solicitud no contiene datos, criterios y formato. |
| **Resultado Esperado** | El sistema no genera el archivo y muestra una pregunta de aclaración solicitando el dato faltante, el criterio o el formato requerido. |
| **Resultado Obtenido** | El sistema detectó que la solicitud estaba incompleta, no generó el archivo y mostró una pregunta de aclaración solicitando la información faltante para completar el reporte. |
| **Evidencia Visual** |  |
| **Estado** | EXITOSO |
