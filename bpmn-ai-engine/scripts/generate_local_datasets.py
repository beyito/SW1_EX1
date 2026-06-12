from __future__ import annotations

import csv
import json
from datetime import datetime, timedelta
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATASET_DIR = ROOT / "datasets"


POLICIES = [
    {
        "id": "reposicion_tarjeta",
        "client": ["regular", "vip"],
        "urgency": [0.45, 0.82],
        "priority": ["MEDIA", "ALTA"],
        "tasks": [
            ("recepcion_solicitud", "atencion", 18, 36, "ana"),
            ("validacion_identidad", "operaciones", 22, 55, "luis"),
            ("bloqueo_emision", "seguridad", 30, 80, "marco"),
            ("entrega_tarjeta", "atencion", 15, 35, "ana"),
        ],
    },
    {
        "id": "renovacion_tarjeta",
        "client": ["regular", "vip"],
        "urgency": [0.30, 0.68],
        "priority": ["BAJA", "MEDIA"],
        "tasks": [
            ("recepcion_solicitud", "atencion", 15, 28, "ana"),
            ("validacion_identidad", "operaciones", 20, 45, "luis"),
            ("aprobacion_emision", "jefatura", 25, 55, "carla"),
            ("entrega_tarjeta", "atencion", 12, 30, "ana"),
        ],
    },
    {
        "id": "solicitud_credito",
        "client": ["pyme", "vip", "regular"],
        "urgency": [0.55, 0.88, 0.40],
        "priority": ["ALTA", "CRITICA", "MEDIA"],
        "tasks": [
            ("recepcion_solicitud", "atencion", 20, 45, "maria"),
            ("revision_documental", "legal", 80, 180, "sofia"),
            ("evaluacion_riesgo", "riesgos", 140, 360, "diego"),
            ("aprobacion_credito", "jefatura", 60, 150, "carla"),
            ("desembolso", "finanzas", 45, 120, "raul"),
        ],
    },
    {
        "id": "reclamo_servicio",
        "client": ["regular", "vip"],
        "urgency": [0.62, 0.92],
        "priority": ["ALTA", "CRITICA"],
        "tasks": [
            ("registro_reclamo", "atencion", 12, 35, "ana"),
            ("analisis_caso", "rrhh", 75, 260, "pedro"),
            ("resolucion_reclamo", "jefatura", 45, 120, "carla"),
            ("notificacion_cliente", "atencion", 10, 25, "ana"),
        ],
    },
    {
        "id": "licencia_funcionamiento",
        "client": ["pyme", "regular"],
        "urgency": [0.58, 0.32],
        "priority": ["ALTA", "MEDIA"],
        "tasks": [
            ("recepcion_documentos", "ventanilla", 25, 60, "maria"),
            ("revision_documental", "legal", 120, 300, "sofia"),
            ("inspeccion_local", "operaciones", 180, 480, "luis"),
            ("aprobacion_licencia", "jefatura", 70, 160, "carla"),
            ("emision_licencia", "ventanilla", 20, 55, "maria"),
        ],
    },
    {
        "id": "certificado_solvencia",
        "client": ["regular", "pyme", "vip"],
        "urgency": [0.22, 0.35, 0.55],
        "priority": ["BAJA", "MEDIA", "ALTA"],
        "tasks": [
            ("recepcion_solicitud", "ventanilla", 10, 24, "maria"),
            ("verificacion_deuda", "finanzas", 25, 85, "raul"),
            ("emision_certificado", "ventanilla", 15, 35, "maria"),
        ],
    },
    {
        "id": "cambio_datos_cliente",
        "client": ["regular", "vip"],
        "urgency": [0.25, 0.50],
        "priority": ["BAJA", "MEDIA"],
        "tasks": [
            ("recepcion_solicitud", "atencion", 8, 22, "ana"),
            ("validacion_identidad", "operaciones", 18, 45, "luis"),
            ("actualizacion_datos", "sistemas", 25, 70, "nora"),
            ("confirmacion_cliente", "atencion", 8, 20, "ana"),
        ],
    },
    {
        "id": "devolucion_pago",
        "client": ["regular", "vip", "pyme"],
        "urgency": [0.48, 0.76, 0.64],
        "priority": ["MEDIA", "ALTA", "ALTA"],
        "tasks": [
            ("registro_solicitud", "atencion", 12, 30, "ana"),
            ("verificacion_pago", "finanzas", 50, 160, "raul"),
            ("aprobacion_devolucion", "jefatura", 45, 120, "carla"),
            ("ejecucion_devolucion", "finanzas", 35, 100, "raul"),
        ],
    },
    {
        "id": "autorizacion_viaje_menor",
        "client": ["regular", "vip"],
        "urgency": [0.42, 0.78],
        "priority": ["MEDIA", "ALTA"],
        "tasks": [
            ("recepcion_documentos", "ventanilla", 15, 35, "maria"),
            ("revision_legal", "legal", 55, 150, "sofia"),
            ("aprobacion_autorizacion", "jefatura", 35, 90, "carla"),
            ("emision_documento", "ventanilla", 15, 35, "maria"),
        ],
    },
    {
        "id": "permiso_construccion",
        "client": ["regular", "pyme"],
        "urgency": [0.36, 0.66],
        "priority": ["MEDIA", "ALTA"],
        "tasks": [
            ("recepcion_planos", "ventanilla", 30, 80, "maria"),
            ("revision_tecnica", "operaciones", 240, 720, "luis"),
            ("revision_legal", "legal", 120, 320, "sofia"),
            ("inspeccion_terreno", "operaciones", 180, 540, "luis"),
            ("aprobacion_permiso", "jefatura", 80, 220, "carla"),
        ],
    },
    {
        "id": "beca_estudiantil",
        "client": ["regular", "vip"],
        "urgency": [0.52, 0.70],
        "priority": ["MEDIA", "ALTA"],
        "tasks": [
            ("recepcion_postulacion", "ventanilla", 20, 55, "maria"),
            ("revision_requisitos", "legal", 70, 180, "sofia"),
            ("evaluacion_socioeconomica", "rrhh", 120, 360, "pedro"),
            ("aprobacion_beca", "jefatura", 60, 150, "carla"),
            ("notificacion_resultado", "ventanilla", 12, 28, "maria"),
        ],
    },
    {
        "id": "registro_proveedor",
        "client": ["pyme", "regular"],
        "urgency": [0.44, 0.30],
        "priority": ["MEDIA", "BAJA"],
        "tasks": [
            ("recepcion_documentos", "ventanilla", 20, 50, "maria"),
            ("revision_legal", "legal", 90, 240, "sofia"),
            ("validacion_financiera", "finanzas", 80, 210, "raul"),
            ("alta_proveedor", "sistemas", 35, 95, "nora"),
        ],
    },
]


INTENTS = {
    "reposicion_tarjeta": (
        ["Carnet de Identidad", "Denuncia o declaracion de extravio"],
        [
            "perdi mi tarjeta y necesito otra",
            "me robaron la tarjeta",
            "quiero reponer mi tarjeta",
            "mi tarjeta desaparecio",
            "bloquear tarjeta robada y sacar nueva",
            "necesito una nueva tarjeta por extravio",
            "la tarjeta fue retenida y necesito reemplazo",
            "solicito reposicion de tarjeta",
            "mi plastico se perdio",
            "quiero sacar otra tarjeta bancaria",
            "tarjeta extraviada",
            "me hurtaron la tarjeta",
        ],
    ),
    "renovacion_tarjeta": (
        ["Carnet de Identidad", "Tarjeta vencida"],
        [
            "renovar tarjeta vencida",
            "mi tarjeta ya expiro",
            "quiero cambiar mi tarjeta por vencimiento",
            "necesito renovar mi tarjeta",
            "la tarjeta vence este mes",
            "actualizar tarjeta caducada",
            "renovacion de plastico",
            "mi tarjeta antigua ya no sirve",
            "solicito nueva tarjeta por fecha vencida",
            "tarjeta por vencer",
            "cambio de tarjeta vencida",
            "emitir tarjeta renovada",
        ],
    ),
    "solicitud_credito": (
        ["Carnet de Identidad", "Comprobante de ingresos", "Extracto bancario"],
        [
            "necesito solicitar un credito",
            "quiero un prestamo",
            "busco financiamiento para mi negocio",
            "solicitud de credito urgente",
            "quiero credito para capital de trabajo",
            "necesito dinero financiado",
            "prestamo para comprar maquinaria",
            "credito para ampliar mi negocio",
            "requiero financiamiento personal",
            "quiero evaluar un prestamo",
            "pedir credito empresarial",
            "necesito aprobacion de credito",
        ],
    ),
    "reclamo_servicio": (
        ["Documento de respaldo", "Descripcion del incidente"],
        [
            "quiero presentar un reclamo",
            "me atendieron mal",
            "el servicio fallo",
            "tengo que quejarme por mala atencion",
            "hubo un error en el servicio",
            "quiero reclamar por cobro incorrecto",
            "no resolvieron mi solicitud",
            "necesito reportar una mala experiencia",
            "el tramite tuvo demora injustificada",
            "quiero levantar queja",
            "servicio deficiente",
            "reclamo por incumplimiento",
        ],
    ),
    "licencia_funcionamiento": (
        ["NIT", "Croquis", "Carnet de Identidad", "Contrato de alquiler o propiedad"],
        [
            "quiero abrir un negocio",
            "necesito licencia de funcionamiento",
            "permiso para operar mi local",
            "tramite para habilitar comercio",
            "abrir una tienda",
            "habilitar mi restaurante",
            "licencia para actividad comercial",
            "autorizacion para local comercial",
            "permiso municipal de funcionamiento",
            "quiero legalizar mi negocio",
            "documentos para funcionar como empresa",
            "registrar actividad economica",
        ],
    ),
    "certificado_solvencia": (
        ["Carnet de Identidad", "Numero de cuenta o registro"],
        [
            "necesito certificado de solvencia",
            "quiero demostrar que no tengo deudas",
            "solicito solvencia",
            "certificado de no adeudo",
            "constancia de deuda cero",
            "documento que certifique que estoy al dia",
            "necesito comprobar mis pagos",
            "sacar certificado financiero",
            "constancia de solvencia economica",
            "requiero certificado para tramite",
            "quiero validar que no debo nada",
            "certificacion de pagos al dia",
        ],
    ),
    "cambio_datos_cliente": (
        ["Carnet de Identidad", "Respaldo del dato a modificar"],
        [
            "quiero cambiar mis datos",
            "actualizar mi direccion",
            "modificar mi telefono",
            "corregir mi nombre",
            "actualizar correo electronico",
            "cambiar datos personales",
            "editar informacion de cliente",
            "registrar nuevo domicilio",
            "cambio de numero celular",
            "corregir mis datos registrados",
            "actualizacion de datos",
            "modificar informacion de contacto",
        ],
    ),
    "devolucion_pago": (
        ["Comprobante de pago", "Carnet de Identidad", "Cuenta de devolucion"],
        [
            "quiero devolucion de pago",
            "pague dos veces",
            "me cobraron de mas",
            "solicito reembolso",
            "devolver dinero cobrado",
            "tengo pago duplicado",
            "quiero recuperar un pago incorrecto",
            "reclamar devolucion de dinero",
            "reembolso por cobro errado",
            "devolucion por transaccion duplicada",
            "pago equivocado",
            "anular cobro y devolver dinero",
        ],
    ),
    "autorizacion_viaje_menor": (
        ["Carnet de Identidad del tutor", "Certificado de nacimiento", "Itinerario de viaje"],
        [
            "autorizacion para viaje de menor",
            "mi hijo necesita permiso de viaje",
            "permiso para que viaje un menor",
            "autorizar salida de menor",
            "documento para viaje de mi hija",
            "necesito permiso notarial de viaje",
            "viaje de menor al exterior",
            "tramite para menor viajando",
            "autorizar viaje escolar",
            "permiso de padres para viaje",
            "menor debe viajar con familiar",
            "solicitar autorizacion de viaje",
        ],
    ),
    "permiso_construccion": (
        ["Plano aprobado", "Documento de propiedad", "Carnet de Identidad"],
        [
            "necesito permiso de construccion",
            "quiero construir en mi terreno",
            "autorizacion para obra",
            "licencia de construccion",
            "permiso para ampliar mi casa",
            "tramite para edificar",
            "aprobar planos de construccion",
            "construccion de vivienda",
            "permiso para remodelacion",
            "autorizacion municipal de obra",
            "quiero levantar una construccion",
            "habilitar proyecto de obra",
        ],
    ),
    "beca_estudiantil": (
        ["Carnet de Identidad", "Certificado de notas", "Comprobante socioeconomico"],
        [
            "quiero solicitar una beca",
            "necesito ayuda estudiantil",
            "postular a beca",
            "beca para estudios",
            "apoyo economico para estudiar",
            "solicitud de beca universitaria",
            "quiero financiamiento academico",
            "ayuda para pagar estudios",
            "beca por rendimiento",
            "beca socioeconomica",
            "postulacion estudiantil",
            "beneficio para estudiante",
        ],
    ),
    "registro_proveedor": (
        ["NIT", "Matricula de comercio", "Cuenta bancaria", "Representante legal"],
        [
            "quiero registrarme como proveedor",
            "alta de proveedor",
            "inscribir mi empresa para vender servicios",
            "registro para proveedor",
            "quiero ofrecer productos a la empresa",
            "habilitar proveedor",
            "registro de empresa proveedora",
            "ser proveedor autorizado",
            "inscripcion en padron de proveedores",
            "postular como proveedor",
            "registrar datos de proveedor",
            "alta comercial de proveedor",
        ],
    ),
}


def main() -> None:
    DATASET_DIR.mkdir(parents=True, exist_ok=True)
    task_count = write_task_history()
    document_count = write_document_activity()
    intent_count = write_policy_intents()
    print(f"predictive_task_history rows: {task_count}")
    print(f"document_activity_history rows: {document_count}")
    print(f"policy_intents policies: {intent_count}")


def write_task_history() -> int:
    path = DATASET_DIR / "predictive_task_history.csv"
    start = datetime(2026, 3, 1, 8, 0, 0)
    process_number = 1
    rows = []

    for policy in POLICIES:
        for index in range(1, 19):
            process_id = f"p{process_number:04d}"
            client_index = (index + process_number) % len(policy["client"])
            client_type = policy["client"][client_index]
            urgency = policy["urgency"][client_index]
            priority = policy["priority"][client_index]
            current = start + timedelta(days=process_number % 75, hours=index % 7)
            reject = (
                index % 13 == 0
                or (policy["id"] == "solicitud_credito" and index % 9 == 0)
                or (policy["id"] == "reclamo_servicio" and index % 8 == 0)
            )
            slow = index % 10 == 0 or (policy["id"] == "permiso_construccion" and index % 5 == 0)

            for task_index, task in enumerate(policy["tasks"]):
                task_id, lane_id, min_minutes, max_minutes, assignee = task
                created = current
                started = created + timedelta(minutes=2 + ((index + task_index) % 18))
                duration = min_minutes + ((index * 17 + task_index * 23 + process_number) % (max_minutes - min_minutes + 1))
                if slow and lane_id in {"rrhh", "legal", "operaciones", "riesgos"}:
                    duration = int(duration * 1.9)
                completed = started + timedelta(minutes=duration)
                status = "COMPLETED"
                rejected_next = 0
                if reject and task_index == len(policy["tasks"]) - 2:
                    status = "REJECTED"
                    rejected_next = 1
                next_task = policy["tasks"][task_index + 1][0] if task_index < len(policy["tasks"]) - 1 else "cierre"
                rows.append(
                    {
                        "policy_id": policy["id"],
                        "process_instance_id": process_id,
                        "task_id": task_id,
                        "lane_id": lane_id,
                        "status": status,
                        "created_at": created.isoformat(timespec="seconds"),
                        "started_at": started.isoformat(timespec="seconds"),
                        "completed_at": completed.isoformat(timespec="seconds"),
                        "assigned_to": assignee,
                        "client_type": client_type,
                        "urgency_score": f"{urgency:.2f}",
                        "priority_label": priority,
                        "next_task_id": next_task,
                        "rejected_next": rejected_next,
                    }
                )
                current = completed + timedelta(minutes=5 + ((index + task_index) % 20))
                if status == "REJECTED":
                    break
            process_number += 1

    write_csv(path, rows)
    return len(rows)


def write_document_activity() -> int:
    path = DATASET_DIR / "document_activity_history.csv"
    start = datetime(2026, 3, 1, 8, 0, 0)
    content_types = [
        "application/pdf",
        "image/jpeg",
        "image/png",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ]
    rows = []
    process_number = 1
    document_number = 1

    for policy in POLICIES:
        for index in range(1, 19):
            process_id = f"p{process_number:04d}"
            client_id = f"c{process_number:04d}"
            base_time = start + timedelta(days=process_number % 75, hours=index % 7)
            burst = index % 17 == 0 or (policy["id"] == "solicitud_credito" and index % 12 == 0)
            document_total = 12 + (index % 8) if burst else 2 + (index % 5)
            for doc_index in range(1, document_total + 1):
                count_last_minute = min(20, 8 + doc_index) if burst else min(6, doc_index)
                rows.append(
                    {
                        "document_id": f"d{document_number:05d}",
                        "process_instance_id": process_id,
                        "policy_id": policy["id"],
                        "client_id": client_id,
                        "created_at": (base_time + timedelta(minutes=3 + doc_index)).isoformat(timespec="seconds"),
                        "count_last_minute": count_last_minute,
                        "content_type": content_types[(doc_index + index + process_number) % len(content_types)],
                        "size": 95_000 + ((doc_index * 41_237 + process_number * 17_311) % 1_450_000),
                        "anomaly_label": 1 if count_last_minute >= 9 else 0,
                    }
                )
                document_number += 1
            process_number += 1

    write_csv(path, rows)
    return len(rows)


def write_policy_intents() -> int:
    rows = [
        {
            "policy_id": policy_id,
            "examples": examples,
            "missing_requirements": requirements,
        }
        for policy_id, (requirements, examples) in INTENTS.items()
    ]
    path = DATASET_DIR / "policy_intents.json"
    path.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
    return len(rows)


def write_csv(path: Path, rows: list[dict[str, object]]) -> None:
    if not rows:
        return
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    main()
