import { Injectable } from '@angular/core';
import { executeGraphql } from '../../../core/api/graphql-client';
import { Lane, PolicyInitialRequirement, PolicyPayload, PolicySummary, TaskExecutionOrder } from '../models/policy-designer.models';

@Injectable({ providedIn: 'root' })
export class PolicyDataService {
  private laneWidthSupported: boolean | null = null;
  private laneHeightSupported: boolean | null = null;

  public async getAllPolicies(): Promise<PolicySummary[]> {
    const response = await executeGraphql<{ getAllPolicies: PolicySummary[] }>(`
      query GetAllPolicies {
        getAllPolicies {
          id
          name
          description
          initialRequirements {
            id
            name
            description
            required
            allowedExtensions
          }
        }
      }
    `);

    return response.getAllPolicies ?? [];
  }

  public async getPolicyById(id: string): Promise<PolicyPayload | null> {
    if (this.laneWidthSupported === false || this.laneHeightSupported === false) {
      return this.getPolicyByIdWithoutWidth(id);
    }

    try {
      const response = await executeGraphql<{ getPolicyById: PolicyPayload | null }>(`
        query GetPolicyById($id: ID!) {
          getPolicyById(id: $id) {
            id
            name
            description
            diagramJson
            initialRequirements {
              id
              name
              description
              required
              allowedExtensions
            }
            lanes {
              id
              name
              color
              x
              width
              height
            }
          }
        }
      `, { id });

      this.laneWidthSupported = true;
      this.laneHeightSupported = true;
      return response.getPolicyById ?? null;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      if (!/Field 'width' in type 'Lane' is undefined|FieldUndefined@\[(getPolicyById\/lanes\/width)\]|Field 'height' in type 'Lane' is undefined|FieldUndefined@\[(getPolicyById\/lanes\/height)\]/i.test(message)) {
        throw error;
      }
      this.laneWidthSupported = false;
      this.laneHeightSupported = false;
      return this.getPolicyByIdWithoutWidth(id);
    }
  }

  public async createPolicy(
    name: string,
    description: string,
    initialRequirements: PolicyInitialRequirement[] = []
  ): Promise<PolicySummary> {
    const response = await executeGraphql<{ createPolicy: PolicySummary }>(`
      mutation CreatePolicy($name: String!, $description: String, $initialRequirements: [PolicyInitialRequirementInput!]) {
        createPolicy(name: $name, description: $description, initialRequirements: $initialRequirements) {
          id
          name
          description
          initialRequirements {
            id
            name
            description
            required
            allowedExtensions
          }
        }
      }
    `, { name, description, initialRequirements });

    return response.createPolicy;
  }

  public async updatePolicyDiagram(
    policyId: string,
    diagramJson: string,
    lanes: Lane[],
    initialRequirements: PolicyInitialRequirement[] = []
  ): Promise<void> {
    const lanePayload = lanes.map((lane) =>
      this.laneWidthSupported && this.laneHeightSupported
        ? lane
        : {
            id: lane.id,
            name: lane.name,
            color: lane.color,
            x: lane.x
          }
    );

    await executeGraphql<{ updatePolicyGraph: PolicySummary }>(`
      mutation UpdatePolicyGraph($policyId: ID!, $diagramJson: String!, $lanes: [LaneInput!], $initialRequirements: [PolicyInitialRequirementInput!]) {
        updatePolicyGraph(policyId: $policyId, diagramJson: $diagramJson, lanes: $lanes, initialRequirements: $initialRequirements) {
          id
          name
        }
      }
    `, { policyId, diagramJson, lanes: lanePayload, initialRequirements });
  }

 public async getTaskExecutionOrder(policyId: string): Promise<TaskExecutionOrder | null> {
    const response = await executeGraphql<{ getTaskExecutionOrder: TaskExecutionOrder | null }>(`
      query GetTaskExecutionOrder($policyId: ID!) {
        getTaskExecutionOrder(policyId: $policyId) {
          policyId
          policyName
          tasks {
            nodeId
            nodeLabel
            nodeType
            order
            dependencies
            laneId
            laneName
          }
        }
      }
    `, { policyId });

    return response.getTaskExecutionOrder ?? null;
  }

  private async getPolicyByIdWithoutWidth(id: string): Promise<PolicyPayload | null> {
    const response = await executeGraphql<{ getPolicyById: PolicyPayload | null }>(`
      query GetPolicyById($id: ID!) {
        getPolicyById(id: $id) {
          id
          name
          description
          diagramJson
          initialRequirements {
            id
            name
            description
            required
            allowedExtensions
          }
          lanes {
            id
            name
            color
            x
          }
        }
      }
    `, { id });

    return response.getPolicyById ?? null;
  }
}
