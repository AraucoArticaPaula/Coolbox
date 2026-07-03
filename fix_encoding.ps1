$files = Get-ChildItem -Path "." -Recurse -Include *.fxml,*.java

foreach ($f in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($f.FullName)
    $text = [System.Text.Encoding]::UTF8.GetString($bytes)

    $pattern = [regex]"[\u00C3\u00C2][\u0080-\u00BF]"

    if ($pattern.IsMatch($text)) {
        try {
            $cp1252Bytes = [System.Text.Encoding]::GetEncoding(1252).GetBytes($text)
            $fixedText = [System.Text.Encoding]::UTF8.GetString($cp1252Bytes)

            [System.IO.File]::WriteAllText($f.FullName, $fixedText, (New-Object System.Text.UTF8Encoding($false)))
            Write-Output "Fixed: $($f.FullName)"
        } catch {
            Write-Output "ERROR en: $($f.FullName) - $($_.Exception.Message)"
        }
    }
}