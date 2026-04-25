# Cambios: Tareas por Proceso (Web + Mobile + Backend)

## Objetivo aplicado

Se reestructuro la visualizacion de tareas para evitar la lista plana por area y mostrar primero las instancias/procesos, y luego sus tareas.

## Backend

1. Se agregaron campos nuevos en `ProcessInstance`:
   - `title`
   - `description`

2. El inicio de proceso (`POST /api/execution/process/start`) ahora recibe:
   - `policyId`
   - `title`
   - `description`

3. Nuevo endpoint:
   - `GET /api/execution/my-processes/tasks`
   - Retorna grupos por instancia de proceso con su metadata y la lista de tareas.

4. Nuevos DTOs:
   - `ProcessTaskGroupDto`
   - `ProcessTaskDto`

5. Reglas de agrupacion:
   - Usuarios de area/funcionarios: procesos `ACTIVE` en los que su area participa.
   - Cliente (`CLIENT`): instancias que el usuario inicio, con sus tareas visibles (pendientes/hechas/otras segun estado).

6. El nombre de proceso mostrado en tareas ahora prioriza el `title` de la instancia y, si no existe, usa el nombre de la politica.

## Frontend Web (Angular)

1. Se agrego captura de **titulo** y **descripcion** por politica al iniciar una instancia.
2. La bandeja de tareas ahora se muestra en dos niveles:
   - Lista de procesos activos
   - Tareas del proceso seleccionado
3. Se ajusto `TaskInbox` para mantener la misma estructura por proceso.
4. Nuevo consumo de servicio:
   - `ExecutionService.getMyProcessTaskGroups()`

## Mobile (Flutter)

1. Al iniciar un tramite se solicita:
   - titulo de instancia
   - descripcion
2. La pantalla de tareas ahora lista:
   - primero las instancias del usuario
   - dentro de cada instancia, sus tareas (con estado y fecha)
3. Nuevo modelo:
   - `process_task_group_model.dart`
4. Nuevo consumo:
   - `TaskService.getMyProcessTaskGroups()`

## Validacion ejecutada

- Backend: compilacion Maven (`-DskipTests`) exitosa.
- Frontend: `ng build` exitoso (solo warnings existentes/presupuesto de estilos).
- Mobile: `flutter analyze` sin issues.
