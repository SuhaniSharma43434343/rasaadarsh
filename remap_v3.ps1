$Path = "app\src\main\assets\medicines.json"
$Utf8Encoding = New-Object System.Text.UTF8Encoding $false

# Read
$RawJson = [System.IO.File]::ReadAllText($Path, $Utf8Encoding)
$json = $RawJson | ConvertFrom-Json

function Get-Category($id) {
    if ($id -le 10) { return "Aamvata" }
    if ($id -le 20) { return "Agnimandya, Ajirna, Visuchika" }
    if ($id -le 29) { return "Amlapita" }
    if ($id -le 40) { return "Arsha" }
    if ($id -le 47) { return "Atisara" }
    if ($id -le 60) { return "Bhagandar" }
    if ($id -le 70) { return "Grahani" }
    if ($id -le 80) { return "Gulma" }
    if ($id -le 90) { return "Hriday Roga" }
    if ($id -le 100) { return "Jwara" }
    if ($id -le 110) { return "Kasa" }
    if ($id -le 120) { return "Kushtha" }
    if ($id -le 130) { return "Mutrakruccha, Mutraghata" }
    if ($id -le 140) { return "Netra Roga" }
    if ($id -le 150) { return "Pandu-Kamala" }
    if ($id -le 160) { return "Pliha Roga" }
    if ($id -le 170) { return "Prameha" }
    if ($id -le 180) { return "Rajayakshama" }
    if ($id -le 190) { return "Raktapitta" }
    if ($id -le 200) { return "Shiro Roga" }
    if ($id -le 210) { return "Shwas Hikka" }
    if ($id -le 220) { return "Stri Roga" }
    if ($id -le 230) { return "Udar Roga" }
    if ($id -le 240) { return "Unmad Apasmar" }
    return "Vata vyadhi"
}

foreach ($item in $json) {
    $item | Add-Member -MemberType NoteProperty -Name "diseaseCategory" -Value (Get-Category($item.id)) -Force
}

# Convert to JSON with indentation and write
$NewJson = $json | ConvertTo-Json -Depth 100
[System.IO.File]::WriteAllText($Path, $NewJson, $Utf8Encoding)
