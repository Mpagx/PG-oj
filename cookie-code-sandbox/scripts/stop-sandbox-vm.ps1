param(
    [string]$VmPath = $(if ($env:POJ_SANDBOX_VM_PATH) { $env:POJ_SANDBOX_VM_PATH } else { 'D:\compile\compile\VMproject\Ubuntu 64 位.vmx' }),
    [string]$SshKeyName = 'poj_sandbox_vm'
)

$ErrorActionPreference = 'Stop'
$vmrun = 'C:\Program Files (x86)\VMware\VMware Workstation\vmrun.exe'

Get-CimInstance Win32_Process -Filter "Name = 'ssh.exe'" |
    Where-Object {
        $_.CommandLine -like '*127.0.0.1:2375:127.0.0.1:2375*' -and
        $_.CommandLine -like "*$SshKeyName*"
    } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force }

$runningVms = & $vmrun list
if ($runningVms -contains $VmPath) {
    & $vmrun stop $VmPath soft
    if ($LASTEXITCODE -ne 0) {
        throw '虚拟机正常关机失败'
    }
}

Write-Output '沙箱 SSH 隧道已关闭，虚拟机已正常关机'
