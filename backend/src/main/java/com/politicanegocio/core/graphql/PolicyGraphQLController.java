package com.politicanegocio.core.graphql;

import com.politicanegocio.core.model.Lane;
import com.politicanegocio.core.model.Policy;
import com.politicanegocio.core.model.TaskExecutionOrder;
import com.politicanegocio.core.service.PolicyService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class PolicyGraphQLController {

    private final PolicyService policyService;

    public PolicyGraphQLController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('COMPANY_ADMIN')")
    public Policy createPolicy(@Argument String name, @Argument String description) {
        return policyService.createPolicy(name, description);
    }

    @QueryMapping
    public List<Policy> getAllPolicies() {
        return policyService.getAllPolicies();
    }

    @QueryMapping
    public Policy getPolicyById(@Argument String id) {
        return policyService.getPolicyById(id);
    }

    @QueryMapping
    public TaskExecutionOrder getTaskExecutionOrder(@Argument String policyId) {
        return policyService.getTaskExecutionOrder(policyId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('COMPANY_ADMIN')")
    public Policy updatePolicyGraph(
            @Argument String policyId,
            @Argument String diagramJson,
            @Argument List<Lane> lanes
    ) {
        return policyService.updatePolicyGraph(policyId, diagramJson, lanes);
    }
}
