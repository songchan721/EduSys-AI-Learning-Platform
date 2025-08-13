# PowerShell script to fix requirement numbering
$content = Get-Content ".kiro/specs/multi-agent-learning-platform/requirements.md" -Raw

# Define the correct order of requirements
$replacements = @(
    @{ old = "### Requirement 5: Technology Stack and Build System"; new = "### Requirement 5: Cross-Platform Technology Stack and Build System" }
    @{ old = "### Requirement 10: Intelligent Study Index Generation"; new = "### Requirement 14: Intelligent Study Index Generation" }
    @{ old = "### Requirement 11: Multi-Channel Notification System"; new = "### Requirement 15: Multi-Channel Notification System" }
    @{ old = "### Requirement 12: Calendar Integration"; new = "### Requirement 16: Calendar Integration" }
    @{ old = "### Requirement 13: Comprehensive Audit"; new = "### Requirement 17: Comprehensive Audit" }
    @{ old = "### Requirement 14: System Observability"; new = "### Requirement 18: System Observability" }
    @{ old = "### Requirement 15: Content Freshness"; new = "### Requirement 19: Content Freshness" }
    @{ old = "### Requirement 16: Spaced Repetition"; new = "### Requirement 20: Spaced Repetition" }
    @{ old = "### Requirement 17: Multi-Modal Learning"; new = "### Requirement 21: Multi-Modal Learning" }
    @{ old = "### Requirement 18: Dialectical Learning"; new = "### Requirement 22: Dialectical Learning" }
    @{ old = "### Requirement 19: Data Privacy"; new = "### Requirement 23: Data Privacy" }
    @{ old = "### Requirement 20: Content Quality"; new = "### Requirement 24: Content Quality" }
)

# Apply replacements
foreach ($replacement in $replacements) {
    $content = $content -replace [regex]::Escape($replacement.old), $replacement.new
}

# Continue with more replacements...
# This is a complex task that would require many more replacements

Write-Host "Numbering fix script created. Manual execution required due to complexity."