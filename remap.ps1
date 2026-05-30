$json = Get-Content -Path app\src\main\assets\medicines.json -Raw | ConvertFrom-Json

function Get-Category($id) {
    if ($id -le 10) { return "Aamvata" }
    if ($id -le 20) { return "Agnimandya, Ajirna, Visuchika" }
    if ($id -le 29) { return "Amlapita" }
    if ($id -le 40) { return "Arsha" }
    if ($id -le 47) { return "Atisara" }
    if ($id -le 51) { return "Bhagandar" }
    if ($id -le 65) { return "Grahani" }
    if ($id -le 72) { return "Gulma" }
    if ($id -le 82) { return "Hriday Roga" }
    if ($id -le 100) { return "Jwara" }
    if ($id -le 112) { return "Kasa" }
    if ($id -le 125) { return "Kushtha" }
    if ($id -le 135) { return "Mutrakruccha, Mutraghata" }
    if ($id -le 140) { return "Netra Roga" }
    if ($id -le 150) { return "Pandu-Kamala" }
    if ($id -le 155) { return "Pliha Roga" }
    if ($id -le 170) { return "Prameha" }
    if ($id -le 180) { return "Rajayakshama" }
    if ($id -le 190) { return "Raktapitta" }
    if ($id -le 195) { return "Shiro Roga" }
    if ($id -le 210) { return "Shwas Hikka" }
    if ($id -le 225) { return "Stri Roga" }
    if ($id -le 235) { return "Udar Roga" }
    if ($id -le 240) { return "Unmad Apasmar" }
    return "Vata vyadhi"
}

foreach ($item in $json) {
    $item.diseaseCategory = Get-Category($item.id)
}

$json | ConvertTo-Json -Depth 100 | Out-File -FilePath app\src\main\assets\medicines.json -Encoding utf8
