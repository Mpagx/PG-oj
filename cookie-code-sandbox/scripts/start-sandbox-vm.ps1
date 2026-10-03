param(
    [string]$VmPath = 'D:\compile\compile\VMproject\Ubuntu 64 位.vmx',
    [string]$VmUser = 'pengge',
    [string]$SshKeyPath = "$env:USERPROFILE\.ssh\poj_sandbox_vm"
)

$ErrorActionPreference = 'Stop'
$vmrun = 'C:\Program Files (x86)\VMware\VMware Workstation\vmrun.exe'

if (-not (Test-Path -LiteralPath $vmrun)) {
    throw "未找到 VMware vmrun: $vmrun"
}
if (-not (Test-Path -LiteralPath $VmPath)) {
    throw "未找到虚拟机配置: $VmPath"
}
if (-not (Test-Path -LiteralPath $SshKeyPath)) {
    throw "未找到沙箱虚拟机 SSH 密钥: $SshKeyPath"
}

$runningVms = & $vmrun list
if ($runningVms -notcontains $VmPath) {
    & $vmrun start $VmPath nogui
    if ($LASTEXITCODE -ne 0) {
        throw '虚拟机启动失败'
    }
}

$guestIp = & $vmrun getGuestIPAddress $VmPath -wait
if ($LASTEXITCODE -ne 0 -or -not $guestIp) {
    throw '无法获取虚拟机 IP'
}
$guestIp = $guestIp.Trim()

$existingListener = Get-NetTCPConnection -LocalAddress 127.0.0.1 -LocalPort 2375 `
    -State Listen -ErrorAction SilentlyContinue
if (-not $existingListener) {
    $sshArgs = @(
        '-N',
        '-o', 'BatchMode=yes',
        '-o', 'ExitOnForwardFailure=yes',
        '-o', 'ServerAliveInterval=30',
        '-o', 'ServerAliveCountMax=3',
        '-o', 'StrictHostKeyChecking=yes',
        '-i', $SshKeyPath,
        '-L', '127.0.0.1:2375:127.0.0.1:2375',
        "$VmUser@$guestIp"
    )
    Start-Process -FilePath 'ssh.exe' -ArgumentList $sshArgs -WindowStyle Hidden
}

$deadline = (Get-Date).AddSeconds(15)
do {
    Start-Sleep -Milliseconds 250
    try {
        $ping = Invoke-RestMethod -Uri 'http://127.0.0.1:2375/_ping' -TimeoutSec 2
    } catch {
        $ping = $null
    }
} while ($ping -ne 'OK' -and (Get-Date) -lt $deadline)

if ($ping -ne 'OK') {
    throw 'SSH 隧道未能连接 Docker Engine'
}

Write-Output "沙箱虚拟机已就绪：$guestIp，Docker 仅通过 127.0.0.1:2375 访问"
