$Path = "app\src\main\assets\medicines.json"
# Read the file using Latin-1 encoding to capture the raw byte values of the garbled text
$Content = [System.IO.File]::ReadAllText($Path, [System.Text.Encoding]::GetEncoding("iso-8859-1"))

# Now $Content contains the string where characters like 'à' (0xE0) are correct.
# We need to convert this string back to a byte array and then decode it as UTF-8.
$Bytes = [System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($Content)
$FixedContent = [System.Text.Encoding]::UTF8.GetString($Bytes)

# Write it back as UTF-8 without BOM
$Utf8NoBomEncoding = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($Path, $FixedContent, $Utf8NoBomEncoding)
