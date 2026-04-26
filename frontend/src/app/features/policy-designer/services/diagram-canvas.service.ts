import { ElementRef, Injectable } from '@angular/core';
import { dia, shapes } from '@joint/plus';
import { Lane, LinkCondition, PolicyPayload } from '../models/policy-designer.models';

@Injectable({ providedIn: 'root' })
export class DiagramCanvasService {
  private readonly canvasWidth = 1200;
  private readonly canvasHeight = 800;
  private laneBackgrounds: dia.Element[] = [];

  public getCanvasWidth(): number {
    return this.canvasWidth;
  }

  public getCanvasHeight(): number {
    return this.canvasHeight;
  }

  public createGraph(): dia.Graph {
    return new dia.Graph({}, { cellNamespace: shapes });
  }

  public createPaper(graph: dia.Graph): dia.Paper {
    return new dia.Paper({
      model: graph,
      background: { color: '#F9FAFB' },
      async: true,
      sorting: dia.Paper.sorting.APPROX,
      cellViewNamespace: shapes,
      width: this.canvasWidth,
      height: this.canvasHeight,
      gridSize: 20,
      drawGrid: {
        name: 'mesh',
        args: [
          { color: 'rgba(148, 163, 184, 0.18)', thickness: 1 },
          { color: 'rgba(148, 163, 184, 0.08)', scaleFactor: 5, thickness: 1 }
        ]
      },

      // 🚩 1. CONFIGURACIÓN DE MAGNETISMO Y RECONEXIÓN
      snapLinks: { radius: 30 }, // El imán atrapará el nodo si lo sueltas a 30px de distancia
      linkPinning: false, // Evita que la flecha se quede flotando en la nada (si la sueltas fuera, vuelve a su nodo original)
      defaultConnectionPoint: { name: 'boundary' }, // Hace que la flecha apunte al borde de la figura, no al centro exacto

      // 🚩 2. VALIDACIÓN (SEGURIDAD)
      // Evita que el usuario conecte la flecha al fondo de la calle o al mismo nodo de origen
      validateConnection: function(cellViewS, magnetS, cellViewT, magnetT, end, linkView) {
        if (cellViewS === cellViewT) return false; // Evita bucles infinitos (conectar a sí mismo)
        if (cellViewT.model.get('isLaneBackground')) return false; // Evita que la flecha se pegue al fondo de la calle
        return true;
      },

      // 🚩 3. PERMISOS INTERACTIVOS (AQUÍ ESTABA EL BLOQUEO)
      interactive: (cellView) => ({
        elementMove: !cellView.model.get('isLaneBackground'),
        addLinkFromMagnet: false,
        labelMove: false,
        linkMove: false,      // Cambiamos a false para no arrastrar la flecha entera por error
        arrowheadMove: true,  // ✅ ESTO PERMITE MOVER LAS PUNTAS (Origen / Destino)
        vertexMove: true,     // ✅ Permite mover los "codos" (esquinas) de la flecha
        vertexAdd: true,      // ✅ Permite crear nuevos codos al arrastrar desde el medio de la línea
        vertexRemove: true    // ✅ Permite borrar codos
      }),
      clickThreshold: 5,
      moving: { validating: false }
    });
  }

  public mountPaper(container: ElementRef, paper: dia.Paper): void {
    container.nativeElement.appendChild(paper.el);
    paper.unfreeze();
  }

  public createShape(type: string, label: string, x: number, y: number): dia.Element {
    const magnetSize = 8;
    const baseOptions = {
      position: { x, y },
      z: 10,
      ports: {
        groups: {
          'in': {
            position: { name: 'left' },
            attrs: {
              portBody: {
                magnet: true,
                r: magnetSize,
                fill: '#1E3A8A',
                stroke: '#1E3A8A',
                strokeWidth: 2,
                opacity: 0
              }
            }
          },
          'out': {
            position: { name: 'right' },
            attrs: {
              portBody: {
                magnet: true,
                r: magnetSize,
                fill: '#1E3A8A',
                stroke: '#1E3A8A',
                strokeWidth: 2,
                opacity: 0
              }
            }
          }
        }
      },
      attrs: {
        body: {
          fill: '#ffffff',
          stroke: '#1E3A8A',
          strokeWidth: 2
        },
        label: {
          text: label,
          fill: '#374151',
          fontSize: 14,
          fontWeight: '600',
          textWrap: { width: -10, height: -10 }
        }
      }
    };

    let shape: dia.Element;

    switch (type) {
      case 'START':
        shape = new shapes.standard.Circle({
          ...baseOptions,
          size: { width: 80, height: 80 },
          attrs: {
            ...baseOptions.attrs,
            body: { ...baseOptions.attrs.body, fill: '#ecfdf5', stroke: '#10B981' }
          }
        });
        break;
      case 'DECISION':
        shape = new shapes.standard.Polygon({
          ...baseOptions,
          size: { width: 120, height: 120 },
          attrs: {
            body: {
              refPoints: '0,60 60,0 120,60 60,120',
              fill: '#fffbeb',
              stroke: '#d97706',
              strokeWidth: 2
            },
            label: { ...baseOptions.attrs.label, text: label }
          }
        });
        break;
      case 'SYNCHRONIZATION':
        shape = new shapes.standard.Rectangle({
          ...baseOptions,
          size: { width: 160, height: 20 },
          attrs: {
            body: {
              fill: '#1f2937',
              stroke: '#1f2937',
              strokeWidth: 2,
              rx: 0,
              ry: 0
            },
            label: { ...baseOptions.attrs.label, text: label, fill: '#ffffff', fontSize: 12, fontWeight: 'bold' }
          }
        });
        break;
      case 'JOIN':
        shape = new shapes.standard.Rectangle({
          ...baseOptions,
          size: { width: 160, height: 20 },
          attrs: {
            body: {
              fill: '#374151',
              stroke: '#374151',
              strokeWidth: 2,
              rx: 0,
              ry: 0
            },
            label: { ...baseOptions.attrs.label, text: label, fill: '#ffffff', fontSize: 12, fontWeight: 'bold' }
          }
        });
        break;
      case 'FORK':
        shape = new shapes.standard.Rectangle({
          ...baseOptions,
          size: { width: 20, height: 160 },
          attrs: {
            body: {
              fill: '#1f2937',
              stroke: '#1f2937',
              strokeWidth: 2,
              rx: 0,
              ry: 0
            },
            label: { ...baseOptions.attrs.label, text: label, fill: '#ffffff', fontSize: 12, fontWeight: 'bold' }
          }
        });
        break;
      case 'END':
        shape = new shapes.standard.Circle({
          ...baseOptions,
          size: { width: 90, height: 90 },
          attrs: {
            ...baseOptions.attrs,
            body: { ...baseOptions.attrs.body, fill: '#fef2f2', stroke: '#ef4444', strokeWidth: 4 }
          }
        });
        break;
      default:
        shape = new shapes.standard.Rectangle({
          ...baseOptions,
          size: { width: 140, height: 70 },
          attrs: {
            ...baseOptions.attrs,
            body: { ...baseOptions.attrs.body, rx: 12, ry: 12, fill: '#f8fafc', stroke: '#1E3A8A' }
          }
        });
    }

    // Add ports to the shape
    shape.addPort({ id: 'in', group: 'in' });
    shape.addPort({ id: 'out', group: 'out' });
    shape.set('nodeType', type);
    if (type === 'TASK') {
      shape.set('nodeMeta', {
        taskForm: {
          title: '',
          description: '',
          fields: [],
          attachments: []
        }
      });
    }
    if (type === 'DECISION') {
      shape.set('nodeMeta', { decisionExpression: '' });
    }

    return shape;
  }

  public createLink(source: dia.Element, target: dia.Element, condition?: string): dia.Link {
    const link = new shapes.standard.Link();
    link.source(source);
    link.target(target);
    link.set('z', 0);
    link.toBack();
    link.attr({
      line: {
        stroke: '#1E3A8A',
        strokeWidth: 2.5,
        strokeLinecap: 'round',
        strokeLinejoin: 'round',
        sourceMarker: null, // Sin marcador en el origen
        targetMarker: {
          type: 'path',
          d: 'M 10 -5 0 0 10 5 z',
          fill: '#1E3A8A',
          stroke: '#1E3A8A',
          'stroke-width': 1
        }
      }
    });

    if (condition) {
      link.set('conditionLabel', condition);
      link.prop('condition', {
        type: 'expression',
        script: `#_decisionTomada == '${this.escapeSingleQuotes(condition)}'`
      });
      link.appendLabel({
        attrs: {
          text: {
            text: condition,
            fill: '#1E3A8A',
            fontSize: 13,
            fontWeight: 'bold'
          },
          rect: {
            fill: '#ffffff',
            stroke: '#1E3A8A',
            strokeWidth: 1,
            rx: 3,
            ry: 3
          }
        },
        position: 0.5
      });
    }

    link.router('orthogonal', { padding: 30 });
    link.connector('straight', { cornerType: 'line' });
    return link;
  }

public renderLaneBackgrounds(graph: dia.Graph, lanes: Lane[]): void {
    this.clearLaneBackgrounds();

    if (lanes.length === 0) {
      return;
    }

    const normalizedLanes = this.normalizeLaneGeometry(lanes);
    const headerHeight = 44; 

    normalizedLanes.forEach((lane) => {
      const laneWidth = this.getLaneWidth(lane, normalizedLanes.length);
      const positionX = (lane.x ?? laneWidth / 2) - laneWidth / 2;
      
      // 🚩 Usamos HeaderedRectangle: Es UN SOLO elemento indivisible
      const laneShape = new shapes.standard.HeaderedRectangle({
        z: -10, // Se queda al fondo
        position: { x: positionX, y: 0 },
        size: { width: laneWidth, height: this.canvasHeight },
        isLaneBackground: true,
        attrs: {
          // El cuerpo de la calle
          body: {
            fill: '#ffffff',
            fillOpacity: 0.6, // IMPORTANTE: fillOpacity para no afectar la cabecera
            stroke: '#d1d5db', 
            strokeWidth: 1
          },
          // La cabecera
          header: {
            fill: '#eef2ff', 
            height: headerHeight,
            stroke: '#d1d5db', 
            strokeWidth: 1
          },
          // El texto de la cabecera
          headerText: {
            text: lane.name.toUpperCase(), 
            fill: '#1E3A8A',
            fontSize: 14,
            fontWeight: 'bold',
            refX: 20,
            textAnchor: 'start' 
          },
          // Sin texto en el cuerpo
          bodyText: {
            text: ''
          }
        }
      });

      (laneShape as any).laneId = lane.id;
      
      graph.addCell(laneShape);
      // Como ahora es uno solo, empujamos directamente laneShape
      this.laneBackgrounds.push(laneShape); 
    });
  }

  public renderPolicy(graph: dia.Graph, policy: PolicyPayload, lanes: Lane[]): void {
    graph.clear();
    this.clearLaneBackgrounds();

    if (policy.diagramJson) {
      const graphData = this.sanitizeGraphJSON(JSON.parse(policy.diagramJson));
      graph.fromJSON(graphData);
    }

    if (lanes.length > 0) {
      this.renderLaneBackgrounds(graph, lanes);
    }
  }

  public getPersistedGraphJSON(graph: dia.Graph): dia.Graph.JSON {
    const graphJson = graph.toJSON() as dia.Graph.JSON;
    return this.sanitizeGraphJSON(graphJson);
  }

  public sanitizeGraphJSON(graphJson: dia.Graph.JSON): dia.Graph.JSON {
    return {
      ...graphJson,
      cells: graphJson.cells.filter((cell: any) => !cell.isLaneBackground)
    };
  }

  public hasExistingLink(graph: dia.Graph, sourceId: dia.Cell.ID, targetId: dia.Cell.ID): boolean {
    return graph.getLinks().some((link) => {
      const currentSourceId = link.get('source')?.id;
      const currentTargetId = link.get('target')?.id;
      return currentSourceId === sourceId && currentTargetId === targetId;
    });
  }

  public getNodeSize(type: string): { width: number; height: number } {
    switch (type) {
      case 'START':
      case 'END':
        return { width: 80, height: 80 };
      case 'DECISION':
        return { width: 120, height: 120 };
      case 'SYNCHRONIZATION':
      case 'JOIN':
        return { width: 160, height: 20 };
      case 'FORK':
        return { width: 20, height: 160 };
      case 'TASK':
      default:
        return { width: 140, height: 70 };
    }
  }

  public clampNodeX(x: number, nodeWidth: number): number {
    return Math.max(10, Math.min(x, this.canvasWidth - nodeWidth - 10));
  }

  public clampNodeY(y: number, nodeHeight: number): number {
    return Math.max(54, Math.min(y, this.canvasHeight - nodeHeight - 10));
  }

  public recalculateLanePositions(lanes: Lane[]): Lane[] {
    const laneWidth = this.getDefaultLaneWidth(lanes.length);
    return lanes.map((lane, index) => ({
      ...lane,
      x: index * laneWidth + laneWidth / 2,
      width: laneWidth
    }));
  }

  public getLaneIdByX(lanes: Lane[], x: number): string | null {
    if (lanes.length === 0) {
      return null;
    }
    const normalizedLanes = this.normalizeLaneGeometry(lanes);
    const lane = normalizedLanes.find((item) => {
      const width = this.getLaneWidth(item, normalizedLanes.length);
      const center = item.x ?? width / 2;
      const left = center - width / 2;
      const right = center + width / 2;
      return x >= left && x <= right;
    });
    return lane?.id ?? normalizedLanes[normalizedLanes.length - 1]?.id ?? null;
  }

  public getCanvasRect(paper: dia.Paper): DOMRect {
    return paper.viewport.getBoundingClientRect();
  }

  public clearLaneBackgrounds(): void {
    this.laneBackgrounds.forEach((background) => background.remove());
    this.laneBackgrounds = [];
  }

  public updateNodeLabel(element: dia.Element, newLabel: string): void {
    if (element.isLink()) {
      return;
    }
    element.attr('label/text', newLabel);
  }

  public deleteElement(graph: dia.Graph, cellId: dia.Cell.ID): void {
    const cell = graph.getCell(cellId);
    if (cell) {
      cell.remove();
    }
  }

  public updateLinkCondition(link: dia.Link, conditionLabel: string, condition?: LinkCondition): void {
    if (!link || !link.isLink()) {
      return;
    }

    if (link.labels().length > 0) {
      link.removeLabel(0);
    }

    const cleanLabel = (conditionLabel ?? '').trim();
    link.set('conditionLabel', cleanLabel);
    if (condition) {
      link.prop('condition', condition);
    } else if (cleanLabel) {
      link.prop('condition', {
        type: 'expression',
        script: `#_decisionTomada == '${this.escapeSingleQuotes(cleanLabel)}'`
      });
    } else {
      link.prop('condition', { type: 'default' });
    }

    if (cleanLabel) {
      link.appendLabel({
        attrs: {
          text: {
            text: cleanLabel,
            fill: '#1E3A8A',
            fontSize: 13,
            fontWeight: 'bold'
          },
          rect: {
            fill: '#ffffff',
            stroke: '#1E3A8A',
            strokeWidth: 1,
            rx: 3,
            ry: 3
          }
        },
        position: 0.5
      });
    }
  }

  private escapeSingleQuotes(value: string): string {
    return value.replace(/'/g, "\\'");
  }

  private normalizeLaneGeometry(lanes: Lane[]): Lane[] {
    if (lanes.length === 0) {
      return [];
    }

    const hasGeometry = lanes.every((lane) => Number.isFinite(lane.x) && Number.isFinite(lane.width) && (lane.width ?? 0) > 0);
    if (!hasGeometry) {
      return this.recalculateLanePositions(lanes);
    }

    return [...lanes].sort((a, b) => (a.x ?? 0) - (b.x ?? 0));
  }

  private getLaneWidth(lane: Lane, laneCount: number): number {
    return lane.width && lane.width > 0 ? lane.width : this.getDefaultLaneWidth(laneCount);
  }

  private getDefaultLaneWidth(laneCount: number): number {
    return laneCount > 0 ? this.canvasWidth / laneCount : this.canvasWidth;
  }
}
