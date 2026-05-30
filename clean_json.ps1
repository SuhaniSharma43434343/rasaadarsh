$filePath = "c:\Users\2303031050616\Desktop\rasaadarsh\app\src\main\assets\medicines.json"
$data = Get-Content $filePath -Raw | ConvertFrom-Json

$newData = @()
foreach ($item in $data) {
    $newItem = [PSCustomObject]@{
        id = $item.id
        name = $item.name
        hindiName = $item.hindiName
        benefits = $item.benefits
        ingredients = $item.ingredients
        dose = $item.dose
        preparation = $item.preparation
        anupana = $item.anupana
        diseaseCategory = $item.diseaseCategory
        ingredientsListJson = if ($null -ne $item.ingredientsListJson) { $item.ingredientsListJson } else { "[]" }
    }
    $newData += $newItem
}

$newData | ConvertTo-Json -Depth 10 | Set-Content $filePath -Encoding UTF8
Write-Host "Cleaned JSON saved."
