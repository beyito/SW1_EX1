# Cambios Implementados - Drag & Drop y Canvas Expandido

## 🎯 Problemas Resueltos

| Problema | Solución |
|----------|----------|
| Canvas muy pequeño | Expandido de 1400x900 a **3000x2000** |
| Lienzo centrado en el medio | Viewport inicia en **top-left (0,0)** en lugar de center |
| Crear nodos con click | Ahora se **arrastra desde los botones al canvas** (drag & drop) |
| Flechas no visibles | Stroke: #1e293b → **#000000** (puro negro), ancho: 3 → **5px**, punta: grande → **xlarge** |

---

## 📐 Cambios en app.ts

### 1. Paper Configuration (Canvas Ampliado)
```typescript
// ANTES: 1400x900
width: 1400,
height: 900,

// AHORA: 3000x2000 para más espacio
width: 3000,
height: 2000,

// Nuevas opciones
clickThreshold: 5,
moving: { validating: false }
```

### 2. Zoom y Viewport Inicial
```typescript
// ANTES: Centrado
this.scroller.zoom(0.8, ...);
this.scroller.center();

// AHORA: Top-left al 65% zoom
this.scroller.zoom(0.65, { max: 3, min: 0.2, grid: 0.2 });
this.scroller.scroll(0, 0);  // ← Top-left
```

### 3. Sistema de Drag & Drop
```typescript
// Nuevas propiedades para tracking del drag
private draggingNodeType: string | null = null;
private draggedNodeData: { x: number; y: number } | null = null;

// Nuevos métodos
public startDraggingNode(nodeType: string, label: string, event: DragEvent): void
private getNodeSize(type: string): { width: number; height: number }
```

**Flujo**:
1. Usuario hace drag desde botón en panel
2. Se muestra preview flotante en tiempo real
3. Al soltar sobre canvas, se crea el nodo en esa posición
4. El nodo es movible después de crearse

### 4. Flechas Más Visibles
```typescript
// ANTES
stroke: '#1e293b',
strokeWidth: 3,
targetMarker: { size: 'large', fill: '#1e293b', ... }

// AHORA
stroke: '#000000',        // ← Negro puro
strokeWidth: 5,           // ← 67% más grueso
targetMarker: {
  type: 'classic',
  size: 'xlarge',         // ← Punta más grande
  fill: '#000000',
  stroke: '#000000'
}
```

### 5. Paper Event Handlers Mejorados
```typescript
// Permite seleccionar elementos AND moverlos
this.paper.on('element:pointerdown', (cellView: any) => {
  if ((cellView.model as any).isLaneBackground) {
    cellView.model.toFront();
  }
});

// Interactive mode ya activo por defecto
interactive: true  // Los nodos se pueden mover después de crearse
```

---

## 🎨 Cambios en app.html

### Botones con Drag & Drop
```html
<!-- ANTES: Click simple -->
<button type="button" (click)="addNode('START', 'Inicio')">Inicio</button>

<!-- AHORA: Draggable -->
<button type="button" draggable="true" 
        (dragstart)="startDraggingNode('START', 'Inicio', $event)">
  ⭕ Inicio
</button>
```

**Todos los botones (Inicio, Tarea, Decisión, Fin**) ahora soportan drag & drop.

### Hint Visual
```html
<p class="section-hint">Arrastra desde aquí al lienzo</p>
```

---

## 🎨 Cambios en app.scss

### Estilos Drag & Drop
```scss
.tool-buttons {
  button {
    background: linear-gradient(135deg, #3b82f6, #2563eb);
    cursor: grab;  // ← Visual feedback para draggable
    
    &:hover:not(:disabled) {
      box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
      transform: translateY(-2px);
    }
    
    &:active {
      cursor: grabbing;  // ← Durante el drag
      opacity: 0.8;
    }
  }
}
```

### Hint Section
```scss
.section-hint {
  font-size: 0.75rem;
  color: #94a3b8;
  font-style: italic;
}
```

---

## 🔧 Cómo Usar

### Crear Nodos
1. **Arrastra** un botón (Inicio, Tarea, Decisión, Fin) desde el panel
2. El botón se vuelve **"grabbing"** durante el movimiento
3. **Suelta** sobre el canvas en la posición deseada
4. El nodo aparece exactamente donde soltaste
5. Puedes **mover el nodo después** arrastrándolo en el canvas

### Conectar Nodos
1. Haz **click en Nodo A** → "Nodo origen seleccionado"
2. Haz **click en Nodo B** → "Nodo destino seleccionado"
3. Haz **click "Conectar nodos"** → aparece la flecha **negra y gruesa**
4. La flecha debe ser **visible oscura** entre los dos nodos

### Flechas Visibles
- Color: **Negro puro (#000000)**
- Grosor: **5px** (muy gruesas)
- Punta: **xlarge** (triángulo grande)
- Router: Orthogonal (ángulos rectos)
- Conector: Smooth (curvas suaves)

---

## 📊 Comparativa de Canvas

| Aspecto | Antes | Ahora |
|--------|-------|-------|
| **Dimensión** | 1400×900 | 3000×2000 (6.67x más área) |
| **Zoom inicial** | 80% centrado | 65% top-left |
| **Creación nodos** | Click (posición fija) | **Drag & drop (posición libre)** |
| **Movimiento nodos** | Arrastar después | Arrastar (igual, pero mejor UX) |
| **Flecha stroke** | 3px #1e293b (gris oscuro) | **5px #000000 (negro)** |
| **Flecha punta** | large | **xlarge** |

---

## ✅ Verificación

```bash
# Frontend build
cd c:\SW1_EX1\flow-web
npm run build
# ✓ Application bundle generation complete [9.6s]
# ✓ Only non-fatal warnings about CSS selectors

# Backend compile
cd c:\SW1_EX1\core
.\mvnw.cmd clean compile
# ✓ No output = compilation successful
```

---

## 🚀 Cómo Empezar

```powershell
# Terminal 1 - Backend
cd c:\SW1_EX1\core
.\mvnw.cmd spring-boot:run

# Terminal 2 - Frontend (cuando backend esté listo)
cd c:\SW1_EX1\flow-web
npm start
```

Luego abre http://localhost:4200 y verifica:
1. ✓ Canvas visible (65% zoom, más área)
2. ✓ Arrastra botones al canvas
3. ✓ Los nodos aparecen donde sueltas
4. ✓ Las flechas son **negras, gruesas y visibles**
5. ✓ Las calles (swimlanes) se ven con opacidad 0.18
