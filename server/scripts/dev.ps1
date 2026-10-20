param(
    [ValidateSet('run', 'test', 'package')][string]$Task = 'run',
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
$env:SPRING_PROFILES_ACTIVE = 'local'
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
