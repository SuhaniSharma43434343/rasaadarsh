$json = Get-Content app/src/main/assets/medicines.json -Raw | ConvertFrom-Json
foreach ($m in $json) {
    Write-Output "$($m.id): $($m.name)"
}
