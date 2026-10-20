param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localDirectory = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Path $localDirectory -Force | Out-Null

function Get-LocalSecret([string]$Name, [int]$Bytes) {
    $secretFile = Join-Path $localDirectory $Name
    if (!(Test-Path -LiteralPath $secretFile)) {
        $randomBytes = New-Object byte[] $Bytes
        $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $generator.GetBytes($randomBytes) } finally { $generator.Dispose() }
        [IO.File]::WriteAllText($secretFile, [Convert]::ToBase64String($randomBytes))
    }
    $value = [IO.File]::ReadAllText($secretFile).Trim()
    # 只导出本机脚本生成的Base64值；不覆盖损坏文件，不把多行内容注入properties。
    $decoded = $null
    try { $decoded = [Convert]::FromBase64String($value) } catch { throw "Invalid local secret file: $Name" }
    if ($decoded.Length -lt $Bytes -or $value -match '[\r\n]') { throw "Invalid local secret length: $Name" }
    return $value
}

# 复用原来的四个文件，重启/重复执行不会轮换签名密钥或初始密码。
# 仅写忽略目录，绝不把密钥写入classpath、IDE运行配置或控制台。
$lines = @(
    'ADMIN_JWT_SECRET=' + (Get-LocalSecret 'jwt-secret.txt' 48)
    'ADMIN_READ_KEY=' + (Get-LocalSecret 'user-read-key.txt' 48)
    'CONTENT_READ_KEY=' + (Get-LocalSecret 'content-read-key.txt' 48)
    'ADMIN_BOOTSTRAP_PASSWORD=' + (Get-LocalSecret 'bootstrap-password.txt' 24)
)
$configFile = Join-Path $localDirectory 'application-secrets.properties'
$temporaryFile = Join-Path $localDirectory ('secrets-' + [Guid]::NewGuid().ToString('N') + '.tmp')
try {
    [IO.File]::WriteAllLines($temporaryFile, $lines, [Text.Encoding]::ASCII)
    if (Test-Path -LiteralPath $configFile) { [IO.File]::Replace($temporaryFile, $configFile, [NullString]::Value) }
    else { [IO.File]::Move($temporaryFile, $configFile) }
} finally {
    if (Test-Path -LiteralPath $temporaryFile) { Remove-Item -LiteralPath $temporaryFile }
}
Write-Output 'Local admin configuration prepared. Secret values were not printed.'
