# Cria o repositorio no GitHub e envia o codigo.
# Rode este script DEPOIS de concluir o "gh auth login".

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$gh = 'C:\Program Files\GitHub CLI\gh.exe'
if (-not (Test-Path $gh)) {
    $cmd = Get-Command gh -ErrorAction SilentlyContinue
    if (-not $cmd) { throw 'GitHub CLI nao encontrado. Reabra o terminal e tente novamente.' }
    $gh = $cmd.Source
}

Write-Host 'Verificando autenticacao...' -ForegroundColor Cyan
& $gh auth status
if ($LASTEXITCODE -ne 0) { throw 'Nao autenticado. Rode primeiro:  gh auth login' }

$usuario = (& $gh api user --jq .login)
Write-Host "Autenticado como: $usuario" -ForegroundColor Green

# O badge de CI no README precisa apontar para o dono real do repositorio.
$readme = Get-Content 'README.md' -Raw
if ($readme -match 'OWNER/finance-api') {
    $readme.Replace('OWNER/finance-api', "$usuario/finance-api") |
        Set-Content 'README.md' -Encoding utf8 -NoNewline
    git add README.md
    git commit -q -m "Aponta o badge de CI para o repositorio publicado"
    Write-Host 'Badge do README atualizado.' -ForegroundColor Green
}

Write-Host 'Criando o repositorio e enviando o codigo...' -ForegroundColor Cyan
& $gh repo create finance-api `
    --public `
    --source . `
    --remote origin `
    --push `
    --description 'API REST de gestao financeira pessoal em Java 21 e Spring Boot 3.3, com JWT, PostgreSQL, Flyway, OpenAPI, Docker e CI'

if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar ou enviar o repositorio.' }

Write-Host ''
Write-Host 'Pronto! Repositorio publicado em:' -ForegroundColor Green
Write-Host "https://github.com/$usuario/finance-api" -ForegroundColor Yellow
