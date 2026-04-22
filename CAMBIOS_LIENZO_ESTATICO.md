# Cambios - Lienzo Estático (Sin Zoom/Scroll)

## 🎯 Problema Resuelto

**Antes**: Lienzo con zoom/scroll dinámico (PaperScroller)  
**Ahora**: **Lienzo estático fijo** de 1200×800 píxeles - sin lidiar con tamaños

---

## 📐 Cambios Técnicos

### 1. Remoción del PaperScroller
```typescript
// REMOVED completamente
// private scroller!: ui.PaperScroller;

// REMOVED configuración
// this.scroller = new ui.PaperScroller({...});
// this.scroller.render();
// this.scroller.zoom(...);
// this.scroller.scroll(...);
```

### 2. Paper con Tamaño Fijo
```typescript
this.paper = new dia.Paper({
  // ANTES: 3000×2000 (demasiado grande)
  // AHORA: 1200×800 (tamaño manejable)
  width: 1200,
  height: 800,
  drawGrid: true,  // Grid visible para mejor UX
  // ... resto igual
});
```

### 3. Render Directo del Paper
```typescript
// ANTES: Usaba scroller
this.canvas.nativeElement.appendChild(this.scroller.el);

// AHORA: Paper directo
this.canvas.nativeElement.appendChild(this.paper.el);
```

### 4. Coordenadas Simplificadas
```typescript
// ANTES: Conversión compleja con scroller
const point = this.scroller.clientToLocalPoint(offsetX, offsetY);

// AHORA: Coordenadas directas
const offsetX = upEvent.clientX - paperRect.left;
const offsetY = upEvent.clientY - paperRect.top;
this.addNode(this.draggingNodeType!, label, offsetX, offsetY);
```

### 5. CSS del Canvas Fijo
```scss
.canvas {
  // ANTES: flex: 1; min-height: 760px;
  // AHORA: tamaño fijo
  width: 1200px;
  height: 800px;
  // ... resto igual
}
```

### 6. Altura de Calles Ajustada
```typescript
// ANTES: canvasHeight = 900;
// AHORA: canvasHeight = 800; // Coincide con paper height
```

---

## ✅ Beneficios

| Aspecto | Antes | Ahora |
|---------|-------|-------|
| **Tamaño** | Dinámico (3000×2000) | **Fijo (1200×800)** |
| **Zoom** | 0.2x - 3x | **Sin zoom** (100%) |
| **Scroll** | Auto-scroll | **Sin scroll** |
| **Complejidad** | Alta (PaperScroller) | **Baja** (paper directo) |
| **Coordenadas** | Conversión compleja | **Directas** |
| **Grid** | Invisible | **Visible** (mejor UX) |

---

## 🎨 UX Mejorada

### Lienzo Siempre Visible
- **1200×800 píxeles** - cabe en cualquier pantalla
- **Sin zoom out** que haga elementos invisibles
- **Grid visible** para alinear elementos fácilmente

### Drag & Drop Sencillo
- Coordenadas directas del mouse
- Sin conversiones de escala
- Más predecible y rápido

### Calles (Swimlanes) Ajustadas
- Altura exacta del canvas (800px)
- Mejor proporción visual

---

## 🚀 Cómo Usar

```powershell
# Terminal 1 - Backend
cd c:\SW1_EX1\core
.\mvnw.cmd spring-boot:run

# Terminal 2 - Frontend
cd c:\SW1_EX1\flow-web
npm start
```

**Resultado esperado:**
- ✅ Lienzo **1200×800 píxeles** siempre visible
- ✅ **Sin zoom ni scroll** - tamaño fijo
- ✅ **Grid visible** para mejor alineación
- ✅ Drag & drop funciona igual
- ✅ Flechas negras y gruesas
- ✅ Calles con opacidad 0.18

---

## 📊 Comparativa Final

| Fase | Canvas | Zoom | Scroll | Complejidad |
|------|--------|------|--------|-------------|
| **Inicial** | 1400×900 | 80% centrado | Sí | Media |
| **Drag&Drop** | 3000×2000 | 65% top-left | Sí | Alta |
| **Estático** | **1200×800** | **Sin zoom** | **No** | **Baja** |

**Resultado**: Lienzo simple, predecible, sin problemas de tamaño.