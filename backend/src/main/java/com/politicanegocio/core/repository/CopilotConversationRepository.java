package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.CopilotConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CopilotConversationRepository extends MongoRepository<CopilotConversation, String> {
    Optional<CopilotConversation> findByIdAndOwnerUsername(String id, String ownerUsername);

    Optional<CopilotConversation> findTopByOwnerUsernameAndPolicyIdOrderByUpdatedAtDesc(
            String ownerUsername,
            String policyId
    );
}
