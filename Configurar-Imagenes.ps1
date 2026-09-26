#requires -Version 5.1
$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'Ejecuta este archivo en Windows.' }
$urlImagenes = (Read-Host 'Project URL de Supabase (https://xxxx.supabase.co)').Trim().TrimEnd('/')
if ($urlImagenes -notmatch '^https://[a-z0-9-]+\.supabase\.co$') { throw 'Usa la URL del proyecto, sin rutas adicionales.' }
$bucketImagenes = (Read-Host 'Nombre del bucket PUBLICO de fotos [productos]').Trim()
if (-not $bucketImagenes) { $bucketImagenes = 'productos' }
if ($bucketImagenes -notmatch '^[a-zA-Z0-9_-]{1,100}$') { throw 'Nombre de bucket no valido.' }
$claveImagenes = Read-Host 'Clave service_role de Supabase (se oculta al escribir; no es la clave de PostgreSQL)' -AsSecureString
if ($claveImagenes.Length -eq 0) { throw 'Falta la clave de servidor.' }
$carpetaImagenes = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Monarca'
New-Item -ItemType Directory -Path $carpetaImagenes -Force | Out-Null
$temporalImagenes = Join-Path $carpetaImagenes ([Guid]::NewGuid().ToString()+'.tmp')
try {
    @{URL=$urlImagenes;BUCKET=$bucketImagenes;KEY=$claveImagenes} | Export-Clixml -LiteralPath $temporalImagenes
    Move-Item -LiteralPath $temporalImagenes -Destination (Join-Path $carpetaImagenes 'imagenes.xml') -Force
} finally { if (Test-Path -LiteralPath $temporalImagenes) { Remove-Item -LiteralPath $temporalImagenes } }
Write-Host 'Configuracion cifrada guardada fuera del repositorio. Reinicia con Iniciar-Monarca.ps1.'
Write-Host 'El bucket debe existir y ser publico para mostrar las fotos en el catalogo.'
