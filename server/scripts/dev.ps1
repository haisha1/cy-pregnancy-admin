param(
    [ValidateSet('run', 'test', 'package')][string]$Task = 'run',
    # local 连接本机依赖；remote 通过预先建立的 SSH 隧道连接远程依赖。
    [ValidateSet('local', 'remote')][string]$Connection = 'local',
    [string]$JdkPath = 'C:/Program Files/Java/jdk-1.8',
    [string]$MavenPath = 'D:/code/apache-maven-3.9.16'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$mavenExecutable = Join-Path $MavenPath 'bin/mvn.cmd'
if (!(Test-Path -LiteralPath (Join-Path $JdkPath 'bin/java.exe')) -or !(Test-Path -LiteralPath $mavenExecutable)) {
    throw 'JDK or Maven not found. Set -JdkPath / -MavenPath.'
}
# 只修改本次进程环境，不修改系统配置；本工程不用小程序端的JWT_SECRET。
$env:JAVA_HOME = $JdkPath
$env:PATH = (Join-Path $JdkPath 'bin') + ';' + $env:PATH
if ($Connection -eq 'remote') {
    if ($Task -ne 'run') { throw 'remote 只允许 run，测试和打包请使用 local，避免连接线上数据库执行测试。' }
    $remoteConfig = Join-Path $projectRoot '.local/connections-remote.properties'
    if (!(Test-Path -LiteralPath $remoteConfig)) {
        throw '请先将 config/connections-remote.properties.example 复制到 .local/connections-remote.properties，填写连接信息并建立 SSH 隧道。'
    }
}
$env:SPRING_PROFILES_ACTIVE = $Connection
Write-Output ('Connection profile: ' + $Connection + '; secret values are not printed.')
# IDEA与脚本共用本机配置；外部提供的ADMIN_*环境变量仍优先于文件。
& (Join-Path $PSScriptRoot 'prepare-local.ps1')
Push-Location $projectRoot
try {
    switch ($Task) {
        'run' { & $mavenExecutable '-B' 'spring-boot:run' }
        'test' { & $mavenExecutable '-B' 'test' }
        'package' { & $mavenExecutable '-B' 'package' }
    }
    if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }
