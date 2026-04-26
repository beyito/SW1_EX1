# Fix: Lienzo en blanco al aplicar cambios IA en otro navegador

Fecha: 2026-04-26

## Problema

Con el mismo diagrama abierto en dos navegadores, cuando el Copilot aplicaba un cambio grande:

- un navegador aplicaba `clear + add` del grafo completo,
- se emitian muchos eventos WebSocket por celda (`remove/add/move/update`),
- en el otro navegador esos eventos podian llegar en orden no estable,
- resultado: el segundo lienzo podia quedar en blanco.

## Solucion implementada

Archivo modificado:

- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.ts`

Cambios clave:

1. Se agrego bandera de bloqueo para reemplazo masivo:
   - `isApplyingPolicySnapshot`

2. Durante snapshot masivo (`applyPolicy`), se bloquea emision por celda:
   - no envia `add/remove/change:position`
   - no envia `scheduleCellRealtimeSync`

3. Se introdujo sincronizacion atomica por WebSocket:
   - nuevo evento `action: 'full-sync'`
   - payload: `{ diagram, lanes, clientId }`

4. Al terminar `applyChange` del Copilot:
   - guarda en backend
   - emite `full-sync` a otros clientes

5. Cliente remoto:
   - nuevo handler `applyRemoteFullSync(...)`
   - reconstruye el diagrama de forma consistente con snapshot completo.

## Resultado

- Se elimina el efecto de lienzo en blanco en el otro navegador al aplicar cambios IA.
- Edicion manual en tiempo real sigue funcionando con eventos por celda.

## Validacion

- Frontend build: `npm run build` OK.
