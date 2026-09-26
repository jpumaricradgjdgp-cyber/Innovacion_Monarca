#requires -Version 5.1
[CmdletBinding()]
param([switch]$Configurar, [switch]$SoloConfigurar)

$ErrorActionPreference = 'Stop'
$backendMonarca = Join-Path $PSScriptRoot 'backend'
if (-not (Test-Path (Join-Path $backendMonarca 'mvnw.cmd'))) {
    throw 'Coloca este archivo en la carpeta Monarca, junto a backend y fronted.'
}
if ($env:OS -ne 'Windows_NT') { throw 'Este iniciador guarda las claves con la proteccion de Windows.' }
$directorioMonarca = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Monarca'
$archivoMonarca = Join-Path $directorioMonarca 'configuracion.xml'

function Leer-TextoMonarca([string]$Pregunta, [string]$Anterior) {
    $aviso = if ($Anterior) { "$Pregunta [$Anterior]" } else { $Pregunta }
    $valor = (Read-Host $aviso).Trim()
    if (-not $valor) { $valor = $Anterior }
    if (-not $valor) { throw "Falta: $Pregunta" }
    return $valor
}
function Leer-ClaveMonarca([string]$Pregunta, $Anterior) {
    $aviso = if ($Anterior) { "$Pregunta (Enter conserva la guardada)" } else { $Pregunta }
    $valor = Read-Host $aviso -AsSecureString
    if ($valor.Length -eq 0) {
        if ($Anterior) { return $Anterior }
        throw "Falta: $Pregunta"
    }
    return $valor
}
function Proteger-Monarca([string]$Valor) {
    if ($Valor) { return ConvertTo-SecureString -String $Valor -AsPlainText -Force }
    return $null
}
function Revelar-Monarca([Security.SecureString]$Valor) {
    return [Net.NetworkCredential]::new('', $Valor).Password
}

$configMonarca = $null
if (Test-Path $archivoMonarca) {
    try { $configMonarca = Import-Clixml -LiteralPath $archivoMonarca }
    catch { throw 'No se pudo leer la configuracion local. Debes usar la misma cuenta de Windows que la creo.' }
}
if ($Configurar -or $SoloConfigurar -or -not $configMonarca) {
    if (-not $configMonarca) {
        $configMonarca = @{
            DB_URL = $env:DB_URL; DB_USERNAME = $env:DB_USERNAME
            DB_PASSWORD = (Proteger-Monarca $env:DB_PASSWORD)
            JWT_SECRET = (Proteger-Monarca $env:JWT_SECRET)
            MAIL_USERNAME = $env:MAIL_USERNAME
            MAIL_PASSWORD = (Proteger-Monarca $env:MAIL_PASSWORD)
            PASSWORD_RESET_URL = $env:PASSWORD_RESET_URL
        }
    }
    Write-Host 'Configuracion local. Las claves se guardan cifradas por Windows, fuera del repositorio.'
    $dbUrl = Leer-TextoMonarca 'URL JDBC de Supabase (jdbc:postgresql://...)' $configMonarca.DB_URL
    if ($dbUrl -notmatch '^jdbc:postgresql://[^/]+/[^?]+' -or $dbUrl -match '(?i)password=|://[^/]*@|YOUR-PASSWORD') {
        throw 'Usa la URL JDBC sin usuario ni clave; se introducen por separado.'
    }
    $dbUser = Leer-TextoMonarca 'Usuario de la base de datos' $configMonarca.DB_USERNAME
    $dbPassword = Leer-ClaveMonarca 'Clave de la base de datos Supabase' $configMonarca.DB_PASSWORD
    $gmail = Leer-TextoMonarca 'Gmail personal que enviara los enlaces' $configMonarca.MAIL_USERNAME
    if ($gmail -notmatch '^[^\s@]+@gmail\.com$') { throw 'Introduce una cuenta personal @gmail.com.' }
    $mailPassword = Leer-ClaveMonarca 'Contrasena de aplicacion de Google (no la habitual)' $configMonarca.MAIL_PASSWORD
    $pagina = Leer-TextoMonarca 'URL de login.html copiada de Live Server' $configMonarca.PASSWORD_RESET_URL
    $uri = $null
    if (-not [Uri]::TryCreate($pagina, [UriKind]::Absolute, [ref]$uri)) { throw 'La URL del login no es valida.' }
    if (($uri.Scheme -ne 'https' -and -not ($uri.Scheme -eq 'http' -and $uri.Host -in @('localhost', '127.0.0.1'))) -or $uri.Query -or $uri.Fragment -or $uri.UserInfo) {
        throw 'Usa HTTPS o HTTP local, sin usuario, consulta ni fragmento.'
    }
    $jwt = $configMonarca.JWT_SECRET
    if (-not $jwt) {
        $bytes = [byte[]]::new(32)
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        $jwt = Proteger-Monarca ([Convert]::ToBase64String($bytes))
    }
    if ([Text.Encoding]::UTF8.GetByteCount((Revelar-Monarca $jwt)) -lt 32) { throw 'El JWT_SECRET existente debe contener al menos 32 bytes.' }
    $configMonarca = @{
        DB_URL = $dbUrl; DB_USERNAME = $dbUser; DB_PASSWORD = $dbPassword
        JWT_SECRET = $jwt; MAIL_USERNAME = $gmail; MAIL_PASSWORD = $mailPassword
        PASSWORD_RESET_URL = $pagina
    }
    New-Item -ItemType Directory -Path $directorioMonarca -Force | Out-Null
    # Export-Clixml cifra SecureString con DPAPI: misma cuenta y equipo Windows.
    $temporal = Join-Path $directorioMonarca ([Guid]::NewGuid().ToString() + '.tmp')
    try {
        try { $configMonarca | Export-Clixml -LiteralPath $temporal }
        catch { throw 'Windows no pudo cifrar las claves. Ejecuta este archivo en tu terminal habitual, con tu cuenta de Windows. No se sustituyo la configuracion anterior.' }
        Move-Item -LiteralPath $temporal -Destination $archivoMonarca -Force
    } finally {
        if (Test-Path $temporal) { Remove-Item -LiteralPath $temporal }
    }
    Write-Host 'Configuracion guardada. No se han enviado correos ni comprobado la conexion.'
}
if ($SoloConfigurar) { return }

# Detecta el caso habitual de un backend anterior abierto sin detener programas.
if (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue) {
    $puertoMonarca = if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8080 }
    $ocupadoMonarca = Get-NetTCPConnection -LocalPort $puertoMonarca -State Listen -ErrorAction SilentlyContinue
    if ($ocupadoMonarca) {
        throw "El puerto $puertoMonarca esta ocupado por el proceso $($ocupadoMonarca[0].OwningProcess). Deten el backend anterior con Ctrl+C y vuelve a ejecutar este archivo. No se ha detenido ningun programa."
    }
}

$variablesMonarca = @{
    DB_URL = $configMonarca.DB_URL; DB_USERNAME = $configMonarca.DB_USERNAME
    DB_PASSWORD = (Revelar-Monarca $configMonarca.DB_PASSWORD)
    JWT_SECRET = (Revelar-Monarca $configMonarca.JWT_SECRET)
    MAIL_HOST = 'smtp.gmail.com'; MAIL_PORT = '587'
    MAIL_USERNAME = $configMonarca.MAIL_USERNAME; MAIL_FROM = $configMonarca.MAIL_USERNAME
    MAIL_PASSWORD = (Revelar-Monarca $configMonarca.MAIL_PASSWORD).Replace(' ', '')
    PASSWORD_RESET_URL = $configMonarca.PASSWORD_RESET_URL
}
$archivoImagenes = Join-Path $directorioMonarca 'imagenes.xml'
if (Test-Path -LiteralPath $archivoImagenes) {
    $configImagenes = Import-Clixml -LiteralPath $archivoImagenes
    $variablesMonarca.SUPABASE_STORAGE_URL = $configImagenes.URL
    $variablesMonarca.SUPABASE_STORAGE_BUCKET = $configImagenes.BUCKET
    $variablesMonarca.SUPABASE_STORAGE_KEY = Revelar-Monarca $configImagenes.KEY
}
$previasMonarca = @{}
try {
    foreach ($nombre in $variablesMonarca.Keys) {
        $previasMonarca[$nombre] = [Environment]::GetEnvironmentVariable($nombre, 'Process')
        [Environment]::SetEnvironmentVariable($nombre, $variablesMonarca[$nombre], 'Process')
    }
    Push-Location $backendMonarca
    try {
        Write-Host 'Iniciando Monarca. Manten esta terminal abierta; Ctrl+C detiene el servidor.'
        & .\mvnw.cmd spring-boot:run
        if ($LASTEXITCODE -ne 0) { throw 'El backend no termino correctamente. Revisa el error de Maven mostrado arriba.' }
    } finally { Pop-Location }
} finally {
    foreach ($nombre in $previasMonarca.Keys) {
        [Environment]::SetEnvironmentVariable($nombre, $previasMonarca[$nombre], 'Process')
    }
    $variablesMonarca.Clear()
}
