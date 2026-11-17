$ErrorActionPreference = 'Stop'

if ($env:OS -ne 'Windows_NT') {
    throw 'Este pacote deve ser gerado no Windows.'
}

$jdkBin = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME 'bin'
} else {
    $comando = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if (-not $comando) {
        throw 'Instale um JDK 17 ou superior e configure JAVA_HOME ou jpackage no PATH.'
    }
    Split-Path $comando.Source
}

$javac = Join-Path $jdkBin 'javac.exe'
$jar = Join-Path $jdkBin 'jar.exe'
$jpackage = Join-Path $jdkBin 'jpackage.exe'
foreach ($ferramenta in @($javac, $jar, $jpackage)) {
    if (-not (Test-Path -LiteralPath $ferramenta -PathType Leaf)) {
        throw "Ferramenta nao encontrada: $ferramenta. Use um JDK completo."
    }
}

$release = Join-Path (Split-Path $jdkBin) 'release'
if (-not (Test-Path -LiteralPath $release -PathType Leaf) -or
        -not (Select-String -LiteralPath $release -Pattern '^OS_ARCH="(x86_64|amd64)"$' -Quiet)) {
    throw 'Use um JDK Windows x64 para gerar o pacote Windows x64.'
}

$destino = Join-Path $PSScriptRoot 'dist'
$aplicativo = Join-Path $destino 'Eudoria'
$zip = Join-Path $destino 'Eudoria-Windows-x64.zip'
if ((Test-Path -LiteralPath $aplicativo) -or (Test-Path -LiteralPath $zip)) {
    throw 'dist\Eudoria ou dist\Eudoria-Windows-x64.zip ja existe. Mova ou remova os arquivos antes de gerar novamente.'
}

$temporario = Join-Path ([System.IO.Path]::GetTempPath()) ('eudoria-' + [guid]::NewGuid())
$classes = Join-Path $temporario 'classes'
$entrada = Join-Path $temporario 'entrada'
try {
    New-Item -ItemType Directory -Path $classes, $entrada, $destino -Force | Out-Null
    $fontes = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'desafio1') -Filter '*.java' -Recurse |
        ForEach-Object { $_.FullName })
    if ($fontes.Count -eq 0) { throw 'Nenhum arquivo Java encontrado.' }

    & $javac --release 11 -encoding UTF-8 -d $classes $fontes
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar o jogo.' }
    & $jar --create --file (Join-Path $entrada 'eudoria.jar') --main-class desafio1.Main -C $classes .
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar o JAR.' }
    & $jpackage --type app-image --name Eudoria --input $entrada --dest $destino `
        --main-jar eudoria.jar --main-class desafio1.Main --add-modules java.desktop,java.logging
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar o aplicativo Windows.' }

    foreach ($nome in @('LICENSE', 'NOTICE')) {
        $documento = Join-Path (Split-Path $jdkBin) $nome
        if (Test-Path -LiteralPath $documento -PathType Leaf) {
            Copy-Item -LiteralPath $documento -Destination (Join-Path $aplicativo "$nome-JAVA.txt")
        }
    }
    Compress-Archive -LiteralPath $aplicativo -DestinationPath $zip
    Write-Host "Pacote criado: $zip"
} finally {
    if (Test-Path -LiteralPath $temporario) {
        Remove-Item -LiteralPath $temporario -Recurse -Force
    }
}
