rootProject.name = "multi-agent-learning-platform"

include(
    // Shared libraries
    ":shared:domain",
    ":shared:messaging",
    ":shared:utils",
    ":shared:security",
    ":shared:database",
    
    // Core services
    ":services:api-gateway",
    ":services:user-service",
    ":services:agent-orchestrator",
    ":services:content-service",
    ":services:payment-service"
)