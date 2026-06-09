# Ampliación de Fundamentación Teórica

## 1.9 Sistema de Gestión Documental

Un Sistema de Gestión Documental, conocido también como DMS por sus siglas en inglés, es una solución tecnológica orientada a organizar, almacenar, controlar y administrar documentos digitales dentro de una organización.

Su propósito principal es facilitar el ciclo de vida de los documentos, desde su creación o recepción hasta su consulta, modificación, conservación o eliminación. Un DMS permite reducir el manejo manual de archivos, mejorar la trazabilidad de la información y centralizar el acceso a documentos importantes.

Entre las funciones más comunes de un sistema de gestión documental se encuentran:

- Almacenamiento de documentos digitales.
- Organización mediante metadatos.
- Búsqueda y recuperación de archivos.
- Control de versiones.
- Asignación de permisos de acceso.
- Registro de acciones realizadas sobre documentos.
- Integración con procesos de negocio.

El control de acceso es un aspecto importante dentro de un DMS, ya que permite definir qué usuarios pueden consultar, modificar o eliminar determinados documentos. Esto ayuda a proteger información sensible y a mantener la confidencialidad de los datos.

Otro aspecto relevante es la auditoría documental. Esta consiste en registrar eventos relacionados con el uso de los documentos, como visualizaciones, ediciones o eliminaciones. Gracias a la auditoría, una organización puede conocer quién accedió a un documento, cuándo lo hizo y qué acción realizó.

Los sistemas de gestión documental son especialmente útiles en organizaciones que manejan expedientes, contratos, formularios, evidencias o documentos administrativos, ya que permiten mantener la información ordenada, segura y disponible para los usuarios autorizados.

## 1.10 Infraestructura en la Nube Basada en AWS

La infraestructura en la nube consiste en utilizar recursos informáticos proporcionados por un proveedor externo a través de Internet. En lugar de depender únicamente de servidores físicos propios, una organización puede utilizar servicios de cómputo, almacenamiento, redes, bases de datos y seguridad ofrecidos por plataformas cloud.

AWS, Amazon Web Services, es una de las plataformas de computación en la nube más utilizadas. Ofrece servicios que permiten desplegar aplicaciones, almacenar información, administrar redes, procesar datos y escalar recursos según la demanda.

Uno de los servicios principales de AWS es **Amazon EC2**, Elastic Compute Cloud. EC2 permite crear y administrar servidores virtuales llamados instancias. Estas instancias funcionan como máquinas en la nube donde se pueden instalar sistemas operativos, aplicaciones, bases de datos, servidores web o contenedores.

Las principales características de EC2 son:

- Creación de servidores virtuales bajo demanda.
- Selección de sistema operativo.
- Configuración de CPU, memoria y almacenamiento.
- Escalabilidad según las necesidades del sistema.
- Acceso remoto para administración.
- Integración con reglas de seguridad y redes virtuales.

Otro servicio importante es **Amazon S3**, Simple Storage Service. S3 es un servicio de almacenamiento de objetos utilizado para guardar archivos, documentos, imágenes, respaldos y otros tipos de datos.

S3 se caracteriza por:

- Alta disponibilidad.
- Escalabilidad automática.
- Almacenamiento de objetos mediante claves o rutas.
- Control de permisos.
- Integración con aplicaciones web y servicios de servidor.
- Posibilidad de generar accesos temporales mediante URLs prefirmadas.

Una URL prefirmada es un enlace temporal que permite acceder a un archivo privado durante un tiempo limitado. Este mecanismo ayuda a mantener los archivos protegidos sin hacerlos públicos permanentemente.

La nube basada en AWS permite desplegar sistemas de forma flexible, reducir la necesidad de infraestructura física propia y facilitar el crecimiento de una aplicación a medida que aumentan los usuarios o la cantidad de datos.

## 1.11 Deep Learning

Deep Learning, o aprendizaje profundo, es una rama del aprendizaje automático que utiliza redes neuronales artificiales con varias capas para aprender patrones a partir de datos. Estas redes están inspiradas de forma general en el funcionamiento del cerebro humano, aunque su implementación es matemática y computacional.

El objetivo del Deep Learning es permitir que un sistema pueda identificar relaciones complejas en grandes volúmenes de información. A diferencia de los métodos tradicionales basados en reglas explícitas, los modelos de Deep Learning aprenden a partir de ejemplos.

Una red neuronal está compuesta por capas de nodos o neuronas artificiales. Cada capa transforma la información recibida y la pasa a la siguiente. Mientras más capas tenga el modelo, mayor capacidad tendrá para representar patrones complejos.

Entre las aplicaciones comunes del Deep Learning se encuentran:

- Clasificación de imágenes.
- Reconocimiento de voz.
- Procesamiento de lenguaje natural.
- Predicción de series temporales.
- Detección de anomalías.
- Sistemas de recomendación.

Una técnica utilizada para detectar anomalías es el **autoencoder**. Un autoencoder es una red neuronal que aprende a reconstruir datos de entrada. Cuando el modelo ha aprendido el comportamiento normal de los datos, puede detectar entradas anómalas porque son más difíciles de reconstruir correctamente.

En términos generales, el funcionamiento de un autoencoder se divide en dos partes:

- **Codificador:** reduce la información de entrada a una representación interna más pequeña.
- **Decodificador:** intenta reconstruir la entrada original a partir de esa representación.

Si la diferencia entre la entrada original y la reconstrucción es alta, se puede considerar que el dato analizado es inusual o anómalo.

Para implementar modelos de Deep Learning se utilizan frameworks especializados como TensorFlow o Keras. Estas herramientas facilitan la creación, entrenamiento y evaluación de redes neuronales.

Deep Learning requiere datos suficientes, capacidad de procesamiento y un proceso de evaluación adecuado. Por ello, en sistemas reales suele combinarse con mecanismos de respaldo o reglas heurísticas cuando no existe suficiente información histórica o cuando el entorno no permite ejecutar modelos complejos.
