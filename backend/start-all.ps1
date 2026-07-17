# 北京中考东城区模拟系统 · 一键启动与验证脚本
# 用法: powershell -ExecutionPolicy Bypass -File start-all.ps1
$ErrorActionPreference = 'Continue'
$base = $PSScriptRoot

Write-Host "=== [1/5] 停止旧 Java 进程 ==="
taskkill /F /IM java.exe 2>$null
Start-Sleep -Seconds 2

Write-Host "=== [2/5] 清理数据库（让新种子按 510 量纲重建）==="
@('auth-service','school-service','student-service','admission-service') | ForEach-Object {
    $p = Join-Path $base "$_\data"
    if (Test-Path $p) { Remove-Item -Recurse -Force $p }
}

Write-Host "=== [3/5] 构建（mvn package，首次较慢）==="
cd $base
mvn package -DskipTests
if ($LASTEXITCODE -ne 0) { Write-Host "构建失败，请检查 Maven/JDK"; exit 1 }

Write-Host "=== [4/5] 启动 5 个服务 ==="
$svcs = @('auth-service','school-service','student-service','admission-service','gateway')
foreach ($svc in $svcs) {
    $jar = Join-Path $base "$svc\target\$svc-1.0.0.jar"
    $log = Join-Path $base "$svc.log"
    Start-Process -FilePath java -ArgumentList "-jar","$jar" `
        -WorkingDirectory (Join-Path $base $svc) `
        -RedirectStandardOutput $log -RedirectStandardError "$svc.err" -WindowStyle Hidden
}
Write-Host "等待服务启动（120s，含 5729 考生种子）..."
Start-Sleep -Seconds 120

Write-Host "=== [5/5] 登录 + 生成志愿 + 一键模拟 + 统计 ==="
$body = '{"username":"admin","password":"admin123"}'
$login = curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d $body
Write-Host "LOGIN=$login"
$token = ($login | ConvertFrom-Json).token
if (-not $token) { Write-Host "登录失败，请查看 gateway.log"; exit 1 }

$sim = curl.exe -s -X POST http://localhost:8080/api/student/applications/simulate -H "Authorization: Bearer $token"
Write-Host "SIMULATE=$sim"

$run = curl.exe -s -X POST http://localhost:8080/api/admission/run/full -H "Authorization: Bearer $token"
Write-Host "RUN_FULL=$run"
$stats = curl.exe -s http://localhost:8080/api/admission/stats -H "Authorization: Bearer $token"
Write-Host "STATS=$stats"

Write-Host ""
Write-Host "=== 完成 ==="
Write-Host "前端: cd ../frontend ; npm run dev  (访问 http://localhost:5173)"
Write-Host "预期: 校额到校按 430 控制线+校内排名录取；统招平行志愿兜底；admitted=quotaAdmitted+tongzhaoAdmitted"
