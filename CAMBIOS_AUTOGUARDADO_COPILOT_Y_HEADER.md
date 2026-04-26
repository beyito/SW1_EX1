# Cambios: Autoguardado Copilot + Header Global

Fecha: 2026-04-26

## 1) Autoguardado al aplicar cambios de IA

Problema:
- Cuando el Copilot aplicaba cambios al diagrama, se veian en pantalla pero no siempre quedaban persistidos al salir.

Solucion implementada:
- Archivo: `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.ts`
- Luego de aplicar cambios con IA (`applyChange`) y refrescar el grafo local (`applyPolicy`), ahora se ejecuta guardado inmediato en backend:
  - `policyDataService.updatePolicyDiagram(selectedPolicyId, JSON.stringify(apply.diagram), lanes)`

Resultado:
- Los cambios generados por IA quedan guardados en la politica y se mantienen al volver a entrar.

## 2) Header superior siempre visible + sin selector de rutas

Requerimiento:
- Mantener el panel superior siempre presente.
- Quitar el selector de rutas.
- Permitir iniciar sesion en cualquier momento.

Cambios:
- Archivo: `frontend/src/app/app.ts`
  - Se elimino el selector "Ir a".
  - El header ahora siempre se renderiza (no depende de `isAuthenticated`).
  - Si hay sesion: muestra perfil + boton "Cerrar sesion".
  - Si no hay sesion: muestra boton "Iniciar sesion" (navega a `/login`).
  - `isAuthenticated` y `profile` pasaron a getters para reflejar estado actual al instante.

- Archivo: `frontend/src/app/app.scss`
  - Se removieron estilos del selector de rutas.
  - Se agrego estilo del boton `login-btn`.

## Validacion

- Frontend build ejecutado: `npm run build` OK.
- Warnings no bloqueantes existentes:
  - budget SCSS en `policy-designer.component.scss`
  - dependencias CommonJS (`@joint/core`, `@stomp/stompjs`)
