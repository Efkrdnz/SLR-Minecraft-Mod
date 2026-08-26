param(
    [string]$Root = "src"
)

$ErrorActionPreference = "Stop"
$needle = "new ResourceLocation("
$utf8 = New-Object System.Text.UTF8Encoding($false)
$changedFiles = 0
$parseCalls = 0
$namespacedCalls = 0

function Get-ConstructorShape {
    param(
        [string]$Text,
        [int]$ArgumentStart
    )

    $parenDepth = 1
    $bracketDepth = 0
    $braceDepth = 0
    $commas = 0
    $inString = $false
    $inChar = $false
    $escaped = $false

    for ($i = $ArgumentStart; $i -lt $Text.Length; $i++) {
        $ch = $Text[$i]

        if ($inString -or $inChar) {
            if ($escaped) {
                $escaped = $false
                continue
            }
            if ($ch -eq '\') {
                $escaped = $true
                continue
            }
            if ($inString -and $ch -eq '"') {
                $inString = $false
            } elseif ($inChar -and $ch -eq "'") {
                $inChar = $false
            }
            continue
        }

        if ($ch -eq '"') {
            $inString = $true
            continue
        }
        if ($ch -eq "'") {
            $inChar = $true
            continue
        }

        switch ($ch) {
            '(' { $parenDepth++ }
            ')' {
                $parenDepth--
                if ($parenDepth -eq 0) {
                    return [PSCustomObject]@{ Commas = $commas; End = $i }
                }
            }
            '[' { $bracketDepth++ }
            ']' { $bracketDepth-- }
            '{' { $braceDepth++ }
            '}' { $braceDepth-- }
            ',' {
                if ($parenDepth -eq 1 -and $bracketDepth -eq 0 -and $braceDepth -eq 0) {
                    $commas++
                }
            }
        }
    }

    throw "Unterminated ResourceLocation constructor at character $ArgumentStart"
}

function Test-IsJavaCodePosition {
    param(
        [string]$Text,
        [int]$Position
    )

    $state = 'code'
    $escaped = $false
    for ($i = 0; $i -lt $Position; $i++) {
        $ch = $Text[$i]
        $next = if ($i + 1 -lt $Position) { $Text[$i + 1] } else { [char]0 }

        switch ($state) {
            'lineComment' {
                if ($ch -eq "`n") { $state = 'code' }
                continue
            }
            'blockComment' {
                if ($ch -eq '*' -and $next -eq '/') {
                    $state = 'code'
                    $i++
                }
                continue
            }
            'string' {
                if ($escaped) { $escaped = $false; continue }
                if ($ch -eq '\') { $escaped = $true; continue }
                if ($ch -eq '"') { $state = 'code' }
                continue
            }
            'char' {
                if ($escaped) { $escaped = $false; continue }
                if ($ch -eq '\') { $escaped = $true; continue }
                if ($ch -eq "'") { $state = 'code' }
                continue
            }
        }

        if ($ch -eq '/' -and $next -eq '/') {
            $state = 'lineComment'
            $i++
        } elseif ($ch -eq '/' -and $next -eq '*') {
            $state = 'blockComment'
            $i++
        } elseif ($ch -eq '"') {
            $state = 'string'
        } elseif ($ch -eq "'") {
            $state = 'char'
        }
    }

    return $state -eq 'code'
}

Get-ChildItem -LiteralPath $Root -Recurse -File -Filter '*.java' | ForEach-Object {
    $text = [IO.File]::ReadAllText($_.FullName)
    $searchFrom = 0
    $cursor = 0
    $builder = New-Object Text.StringBuilder
    $fileChanged = $false

    while (($index = $text.IndexOf($needle, $searchFrom, [StringComparison]::Ordinal)) -ge 0) {
        $argumentStart = $index + $needle.Length
        if (-not (Test-IsJavaCodePosition -Text $text -Position $index)) {
            $searchFrom = $argumentStart
            continue
        }
        $shape = Get-ConstructorShape -Text $text -ArgumentStart $argumentStart
        if ($shape.Commas -gt 1) {
            throw "Unexpected ResourceLocation constructor with $($shape.Commas + 1) arguments in $($_.FullName)"
        }

        [void]$builder.Append($text, $cursor, $index - $cursor)
        if ($shape.Commas -eq 0) {
            [void]$builder.Append('ResourceLocation.parse(')
            $script:parseCalls++
        } else {
            [void]$builder.Append('ResourceLocation.fromNamespaceAndPath(')
            $script:namespacedCalls++
        }

        $cursor = $argumentStart
        $searchFrom = $argumentStart
        $fileChanged = $true
    }

    if ($fileChanged) {
        [void]$builder.Append($text, $cursor, $text.Length - $cursor)
        [IO.File]::WriteAllText($_.FullName, $builder.ToString(), $utf8)
        $script:changedFiles++
    }
}

Write-Output "Updated $changedFiles files: $parseCalls parse calls, $namespacedCalls namespace/path calls."
