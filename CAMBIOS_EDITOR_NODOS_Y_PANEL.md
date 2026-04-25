# Cambios en Editor de Politicas: Nodos y Boton Panel

## Objetivo

Se removieron nodos no usados del editor y se elimino el boton redundante `Panel`.

## Cambios aplicados

### 1) Eliminacion de nodos no usados

Se quitaron del editor los nodos solicitados:

- Union
- Documento
- Evento
- Subproceso

#### Archivo actualizado

- `frontend/src/app/features/policy-designer/utils/policy-designer.constants.ts`
  - Se removieron las plantillas:
    - `JOIN` con etiqueta `Union`
    - `DOCUMENT`
    - `SUBPROCESS`
    - `EVENT`

### 2) Limpieza de implementacion de nodos eliminados

#### Archivo actualizado

- `frontend/src/app/features/policy-designer/services/diagram-canvas.service.ts`
  - Se eliminaron los `case` de creacion de forma para:
    - `DOCUMENT`
    - `SUBPROCESS`
    - `EVENT`
  - Se eliminaron sus tamaños en `getNodeSize(...)`.

Con esto, el editor ya no ofrece ni crea esos nodos.

### 3) Eliminacion del boton `Panel`

#### Archivo actualizado

- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.html`
  - Se elimino el boton:
    - `Panel`
  - Se conserva `Volver al Panel`, que cubre esa navegacion.

## Verificacion

- Build frontend ejecutada correctamente:
  - `npm run build` OK
  - Se mantienen warnings previos de presupuesto/ESM no relacionados a este cambio.
