# Emits a scoped ERP knowledge-graph reminder for Codex SessionStart.
$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSCommandPath))
$graphRoot = Join-Path $repoRoot 'graph\erp'
$graphState = if (Test-Path $graphRoot) { 'present' } else { 'missing' }

Write-Output @"
ERP Obsidian graph reminder:
- Vault: $graphRoot ($graphState).
- Before final response in D:\workspace\ERP tasks, update graph/erp when this session confirms or changes architecture, routes, API contracts, auth/tenant/permission/menu behavior, SQL/schema/init data, frontend routes/API/menu blueprint, tests, risks, or reusable debugging evidence.
- Use current source, POM, YAML, SQL, Git status, and test output as evidence. Treat old memory, old indexes, and target artifacts as hints only.
- Do not auto-edit files from this hook. The agent should make scoped, evidence-based graph updates during the task.
"@
