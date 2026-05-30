$jsonPath = "app/src/main/assets/medicines.json"
$jsonText = Get-Content $jsonPath -Raw -Encoding UTF8
$data = $jsonText | ConvertFrom-Json

function Get-Disease($text) {
    if ($text -match 'rheumatoid|aamvata|amavata') { return "Aamvata" }
    if ($text -match 'indigestion|agnimandya|ajirna|visuchika') { return "Agnimandya, Ajirna, Visuchika" }
    if ($text -match 'acidity|amlapita|amlapitta') { return "Amlapita" }
    if ($text -match 'piles|arsha|hemorrhoid') { return "Arsha" }
    if ($text -match 'diarrhea|atisara') { return "Atisara" }
    if ($text -match 'fistula|bhagandar') { return "Bhagandar" }
    if ($text -match 'ibs|grahani') { return "Grahani" }
    if ($text -match 'tumor|gulma') { return "Gulma" }
    if ($text -match 'heart|hriday|hridya') { return "Hriday Roga" }
    if ($text -match 'fever|jwara') { return "Jwara" }
    if ($text -match 'cough|kasa') { return "Kasa" }
    if ($text -match 'skin|kushtha|leprosy') { return "Kushtha" }
    if ($text -match 'dysuria|mutra') { return "Mutrakruccha, Mutraghata" }
    if ($text -match 'eye|netra') { return "Netra Roga" }
    if ($text -match 'anemia|pandu|kamala|jaundice') { return "Pandu-Kamala" }
    if ($text -match 'spleen|pliha') { return "Pliha Roga" }
    if ($text -match 'diabetes|prameha') { return "Prameha" }
    if ($text -match 'tuberculosis|rajayakshama') { return "Rajayakshama" }
    if ($text -match 'bleeding|raktapitta') { return "Raktapitta" }
    if ($text -match 'head|shiro') { return "Shiro Roga" }
    if ($text -match 'asthma|shwas|hikka') { return "Shwas Hikka" }
    if ($text -match 'gynaecological|stri|menstrual') { return "Stri Roga" }
    if ($text -match 'abdominal|udar') { return "Udar Roga" }
    if ($text -match 'insanity|epilepsy|unmad|apasmar') { return "Unmad Apasmar" }
    if ($text -match 'vata|neurological') { return "Vata vyadhi" }
    return $null
}

$lastDisease = "Aamvata"
foreach ($m in $data) {
    $text = "$($m.tags) $($m.benefits) $($m.name)".ToLower()
    $d = Get-Disease $text
    if ($null -ne $d) {
        $lastDisease = $d
    }
    
    if (-not $m.psobject.properties.match('diseaseCategory').Count) {
        $m | Add-Member -MemberType NoteProperty -Name "diseaseCategory" -Value $lastDisease
    } else {
        $m.diseaseCategory = $lastDisease
    }
}

$jsonStr = $data | ConvertTo-Json -Depth 10
# Unescape unicode
$jsonStr = [Regex]::Replace($jsonStr, "\\u(?<Value>[a-zA-Z0-9]{4})", {
    param($m)
    [char][int]::Parse($m.Groups['Value'].Value, [System.Globalization.NumberStyles]::HexNumber)
})

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText((Get-Item $jsonPath).FullName, $jsonStr, $utf8NoBom)
Write-Host "Successfully categorized medicines with UTF8 unescaped."
