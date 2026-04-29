# Diseño de Base de Datos (Modelo Lógico y Físico)

## 1) Modelo Lógico (MongoDB orientado a documentos)

Colección: `users`
  - `id`
  - `username`
  - `password`
  - `roles[]`
  - `company`
  - `parentCompany`
  - `area`
  - `laneId`
  - `fcmToken`
  - `fcmTokenUpdatedAt`

Colección: `companies`
  - `id`
  - `name`
  - `createdBy`

Colección: `areas`
  - `id`
  - `name`
  - `company`

Colección: `policies`
  - `id`
  - `name`
  - `description`
  - `diagramJson` (grafo BPMN serializado)
  - `startLaneId`
  - `lanes[]`:
      - `id`
      - `name`
      - `color`
      - `x`
      - `width`
      - `height`
  - `companyId`
  - `companyName`
  - `createdBy`
  - `createdAt`
  - `updatedAt`

Colección: `process_instances`
  - `id`
  - `policyId`
  - `title`
  - `description`
  - `status` (ACTIVE, COMPLETED, CANCELLED)
  - `startedBy`
  - `startedAt`
  - `completedAt`

Colección: `task_instances`
  - `id`
  - `processInstanceId`
  - `taskId` (id de nodo TASK dentro del BPMN)
  - `laneId`
  - `status` (PENDING, IN_PROGRESS, COMPLETED, REJECTED)
  - `assignedTo`
  - `formData` (JSON serializado)
  - `createdAt`
  - `startedAt`
  - `completedAt`

Colección: `copilot_conversations`
  - `id`
  - `ownerUsername`
  - `company`
  - `policyId`
  - `policyName`
  - `createdAt`
  - `updatedAt`
  - `messages[]`:
      - `role`
      - `text`
      - `timestamp`
      - `suggestedActions[]`

---

## 2) Modelo Físico (MongoDB Atlas)

```javascript
// ==========================================
// Colección: users
// ==========================================
db.createCollection("users", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["username", "password", "roles", "company"],
      properties: {
        username: { bsonType: "string", minLength: 3 },
        password: { bsonType: "string" },
        roles: {
          bsonType: "array",
          items: { bsonType: "string" }
        },
        company: { bsonType: "string" },
        parentCompany: { bsonType: ["string", "null"] },
        area: { bsonType: ["string", "null"] },
        laneId: { bsonType: ["string", "null"] },
        fcmToken: { bsonType: ["string", "null"] },
        fcmTokenUpdatedAt: { bsonType: ["date", "null"] }
      }
    }
  }
});
db.users.createIndex({ username: 1 }, { unique: true });
db.users.createIndex({ company: 1 });
db.users.createIndex({ roles: 1, company: 1 });

// ==========================================
// Colección: companies
// ==========================================
db.createCollection("companies", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name"],
      properties: {
        name: { bsonType: "string" },
        createdBy: { bsonType: ["string", "null"] }
      }
    }
  }
});
db.companies.createIndex({ name: 1 }, { unique: true });

// ==========================================
// Colección: areas
// ==========================================
db.createCollection("areas", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name", "company"],
      properties: {
        name: { bsonType: "string" },
        company: { bsonType: "string" }
      }
    }
  }
});
db.areas.createIndex({ company: 1, name: 1 }, { unique: true });

// ==========================================
// Colección: policies
// ==========================================
db.createCollection("policies", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["name", "diagramJson", "companyId", "createdBy", "createdAt", "updatedAt"],
      properties: {
        name: { bsonType: "string" },
        description: { bsonType: ["string", "null"] },
        diagramJson: { bsonType: "string" },
        startLaneId: { bsonType: ["string", "null"] },
        lanes: {
          bsonType: "array",
          items: {
            bsonType: "object",
            required: ["id", "name", "color"],
            properties: {
              id: { bsonType: "string" },
              name: { bsonType: "string" },
              color: { bsonType: "string" },
              x: { bsonType: ["double", "int", "long", "null"] },
              width: { bsonType: ["double", "int", "long", "null"] },
              height: { bsonType: ["double", "int", "long", "null"] }
            }
          }
        },
        companyId: { bsonType: "string" },
        companyName: { bsonType: ["string", "null"] },
        createdBy: { bsonType: "string" },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" }
      }
    }
  }
});
db.policies.createIndex({ companyId: 1 });
db.policies.createIndex({ companyId: 1, startLaneId: 1 });
db.policies.createIndex({ updatedAt: -1 });

// ==========================================
// Colección: process_instances
// ==========================================
db.createCollection("process_instances", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["policyId", "status", "startedBy", "startedAt"],
      properties: {
        policyId: { bsonType: "string" },
        title: { bsonType: ["string", "null"] },
        description: { bsonType: ["string", "null"] },
        status: { enum: ["ACTIVE", "COMPLETED", "CANCELLED"] },
        startedBy: { bsonType: "string" },
        startedAt: { bsonType: "date" },
        completedAt: { bsonType: ["date", "null"] }
      }
    }
  }
});
db.process_instances.createIndex({ policyId: 1 });
db.process_instances.createIndex({ startedBy: 1, startedAt: -1 });
db.process_instances.createIndex({ status: 1, startedAt: -1 });

// ==========================================
// Colección: task_instances
// ==========================================
db.createCollection("task_instances", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["processInstanceId", "taskId", "laneId", "status", "createdAt"],
      properties: {
        processInstanceId: { bsonType: "string" },
        taskId: { bsonType: "string" },
        laneId: { bsonType: "string" },
        status: { enum: ["PENDING", "IN_PROGRESS", "COMPLETED", "REJECTED"] },
        assignedTo: { bsonType: ["string", "null"] },
        formData: { bsonType: ["string", "null"] },
        createdAt: { bsonType: "date" },
        startedAt: { bsonType: ["date", "null"] },
        completedAt: { bsonType: ["date", "null"] }
      }
    }
  }
});
db.task_instances.createIndex({ laneId: 1, status: 1 });
db.task_instances.createIndex({ processInstanceId: 1, status: 1 });
db.task_instances.createIndex({ assignedTo: 1 });
db.task_instances.createIndex({ processInstanceId: 1, taskId: 1, status: 1 });

// ==========================================
// Colección: copilot_conversations
// ==========================================
db.createCollection("copilot_conversations", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["ownerUsername", "company", "createdAt", "updatedAt", "messages"],
      properties: {
        ownerUsername: { bsonType: "string" },
        company: { bsonType: "string" },
        policyId: { bsonType: ["string", "null"] },
        policyName: { bsonType: ["string", "null"] },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" },
        messages: {
          bsonType: "array",
          items: {
            bsonType: "object",
            required: ["role", "text", "timestamp"],
            properties: {
              role: { bsonType: "string" },
              text: { bsonType: "string" },
              timestamp: { bsonType: "date" },
              suggestedActions: {
                bsonType: "array",
                items: { bsonType: "string" }
              }
            }
          }
        }
      }
    }
  }
});
db.copilot_conversations.createIndex({ ownerUsername: 1, policyId: 1, updatedAt: -1 });
db.copilot_conversations.createIndex({ company: 1, updatedAt: -1 });
```

