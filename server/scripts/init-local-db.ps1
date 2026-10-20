param([string]$Username = 'root', [string]$Password = 'root')
$ErrorActionPreference = 'Stop'
$previousPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $Password
    # 独立项目库，不读取/清空小程序端数据库。
    & mysql.exe -h localhost -P 3306 -u $Username -e 'CREATE DATABASE IF NOT EXISTS cy_pregnancy_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
    if ($LASTEXITCODE -ne 0) { throw 'Admin database initialization failed.' }
} finally {
    if ($null -eq $previousPassword) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PWD = $previousPassword }
}
