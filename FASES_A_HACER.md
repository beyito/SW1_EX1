FASE 1: Reestructuración Core y Almacenamiento (La Base)
Antes de meter IA, los cimientos de datos deben estar listos.
1. Definición de Requisitos en Políticas de Negocio
•	Dónde: Spring Boot + Angular.
•	Cómo: Modifica tu DTO/Colección de Policy (la que guarda el JSON del diagrama) para añadir un array de requirements (ej. [{"name": "Carnet", "type": "PDF", "mandatory": true}]). En Angular, añade una pestaña en tu modelador para que el creador de la política defina esto antes de guardarla.
2. Reestructuración del Repositorio S3
•	Dónde: Spring Boot (StorageService).
•	Cómo: Cambia la lógica de generación de llaves (keys) en AWS S3. Actualmente subes a la raíz. Ahora tu backend debe generar la ruta así: s3://tu-bucket/clientes/{clientId}/tramites/{processInstanceId}/{documentId}_{fileName}. Esto aísla los archivos lógicamente y permite descargar todo un trámite descargando un solo prefijo.
FASE 2: Gestión Documental y Colaboración (DMS)
3. Privilegios, Auditoría y Edición Colaborativa
•	Dónde: Spring Boot (Control de Acceso) + Angular (Visor) + Servicio Externo (Motor Colaborativo).
•	Cómo (Advertencia de Arquitecto): NO intentes programar la edición colaborativa de Excel/Word desde cero (es un infierno de concurrencia). * Auditoría: Spring Boot intercepta cada GET/POST a un documento y guarda un log en MongoDB/PostgreSQL: [Usuario X, Acción: VIEW/EDIT, Documento Y, Timestamp].
o	Edición Colaborativa: Despliega un contenedor de OnlyOffice Document Server o Collabora Online. Tu Angular embebe su iframe. Tu Spring Boot actúa como puente: genera un token JWT temporal, se lo da a OnlyOffice, y OnlyOffice lee/guarda el archivo directamente en tu S3 mediante callbacks a tu backend.
FASE 3: El Agente de Recepción Inteligente (NLP)
4. Entendimiento del Cliente y Asignación de Política
•	Dónde: Python (Motor IA) + Spring Boot (Gateway).
•	Cómo: El cliente escribe o habla en la aplicación móvil: "Perdí mi tarjeta y necesito una nueva".
o	Spring Boot recibe el texto y se lo manda a Python.
o	Python (usando LLMs o un modelo clasificador TF-IDF/BERT entrenado con tu histórico) compara la intención del usuario con el catálogo de Policies disponibles.
o	Python responde: {"policyId": "123", "confidence": 0.95, "missing_requirements": ["Carnet de Identidad"]}.
o	Spring Boot dispara automáticamente el inicio del ProcessExecution para esa política.
FASE 4: Deep Learning y Predicción (TensorFlow)
Aquí entra el núcleo predictivo utilizando el histórico de ejecuciones que guarda Spring Boot.
5. Identificación de Prioridades y Anomalías (TensorFlow)
•	Dónde: Python (TensorFlow / Keras).
•	Cómo:
o	Anomalías (Autoencoders): Entrenas un modelo no supervisado con los tiempos y rutas normales de tus trámites. Si un trámite se desvía drásticamente o un cliente sube 50 documentos en 1 minuto, el modelo lo marca como anomalía.
o	Prioridad (Clasificación Multiclase): Una red neuronal que evalúa: (Tipo de cliente, Tipo de Trámite, Urgencia detectada en texto) y devuelve una etiqueta de prioridad (Baja, Media, Alta, Crítica) para ordenar la bandeja de los funcionarios.
6. Predecir Rutas y Cuellos de Botella (TensorFlow Series Temporales)
•	Dónde: Python (Redes LSTM o Transformers para secuencias).
•	Cómo: Extraes el Event Log de Spring Boot (qué tareas pasaron, a qué hora, en qué carril). Entrenas un modelo de secuencias predictivas.
o	Ruta: Dado el paso 1 y 2, el modelo predice que el paso 3 será un rechazo con un 80% de probabilidad.
o	Cuellos de botella: El modelo predice que el carril "rrhh" colapsará la próxima semana basado en la carga actual de procesos instanciados.
FASE 5: Analítica y Reportes Dinámicos
7. Dashboard Dinámico y Tipos de Métricas (Deep Learning / KPIs)
•	Dónde: Python (Procesamiento de Datos / Agente Analítico) + Spring Boot (Generación de Archivos).
•	Cómo:
o	El usuario pide en texto: "Quiero un reporte de los cuellos de botella de RRHH del último mes en PDF".
o	Python procesa la orden, ejecuta consultas SQL/Mongo (usando herramientas como Pandas), aplica los modelos predictivos de TensorFlow para proyectar las métricas, y estructura un JSON con los Datos y Criterios.
o	Ese JSON pasa a Spring Boot, que utiliza librerías como JasperReports o Apache POI para renderizar el Formato final (PDF, Excel, CSV) y devolvérselo al frontend.
