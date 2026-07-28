param(
    [Parameter(Mandatory = $true)]
    [string]$OutputPath
)

Add-Type -AssemblyName System.Drawing

$width = 256
$height = 256
$bitmap = [System.Drawing.Bitmap]::new(
    $width,
    $height,
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
)

function Clamp-Byte {
    param([double]$Value)
    return [int][Math]::Max(0, [Math]::Min(255, [Math]::Round($Value)))
}

function New-Color {
    param(
        [double]$Alpha,
        [double]$Red,
        [double]$Green,
        [double]$Blue
    )

    return [System.Drawing.Color]::FromArgb(
        (Clamp-Byte $Alpha),
        (Clamp-Byte $Red),
        (Clamp-Byte $Green),
        (Clamp-Byte $Blue)
    )
}

function Get-SmoothNoise {
    param(
        [int]$X,
        [int]$Y,
        [double]$Offset
    )

    $large = [Math]::Sin(($X * 0.067) + $Offset + ([Math]::Sin($Y * 0.039) * 0.8))
    $cross = [Math]::Cos((($X + $Y) * 0.043) - ($Offset * 0.31))
    $small = [Math]::Sin(($Y * 0.119) + ($X * 0.027) + ($Offset * 0.57))
    return (($large * 0.52) + ($cross * 0.30) + ($small * 0.18))
}

$random = [System.Random]::new(18491)

for ($y = 0; $y -lt $height; $y++) {
    for ($x = 0; $x -lt $width; $x++) {
        $noise = Get-SmoothNoise -X $x -Y $y -Offset 1.0
        $grain = (($random.NextDouble() - 0.5) * 3.0)

        if (($x -lt 96) -and ($y -lt 128)) {
            # Gray-brown skin. Broad, close-value variation keeps the giant
            # organic without introducing hard painted muscle outlines.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 1.8
            $color = New-Color 255 `
                (94 + ($noise * 17) + $grain) `
                (84 + ($noise * 14) + ($grain * 0.6)) `
                (82 + ($noise * 13) + ($grain * 0.5))
        }
        elseif (($x -ge 96) -and ($x -lt 160) -and ($y -lt 96)) {
            # Near-black shaggy fur with subtle warm-violet undertones.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 4.7
            $color = New-Color 255 `
                (29 + ($noise * 8) + ($grain * 0.4)) `
                (25 + ($noise * 7)) `
                (29 + ($noise * 8))
        }
        elseif (($x -ge 160) -and ($x -lt 224) -and ($y -lt 96)) {
            # Horn surface: dull stone-gray, gently brighter toward the right
            # side of the atlas so successive horn pieces can taper in value.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 7.2
            $hornProgress = ($x - 160) / 63.0
            $color = New-Color 255 `
                (87 + ($hornProgress * 33) + ($noise * 8)) `
                (82 + ($hornProgress * 29) + ($noise * 7)) `
                (88 + ($hornProgress * 29) + ($noise * 8))
        }
        elseif (($x -ge 224) -and ($y -lt 32)) {
            # Icy eyes: bright but not pure white.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 9.4
            $color = New-Color 255 `
                (188 + ($noise * 18)) `
                (211 + ($noise * 16)) `
                (241 + ($noise * 10))
        }
        elseif (($x -ge 224) -and ($y -lt 64)) {
            # Aged teeth.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 10.6
            $color = New-Color 255 `
                (180 + ($noise * 10)) `
                (174 + ($noise * 9)) `
                (160 + ($noise * 8))
        }
        elseif (($x -ge 224) -and ($y -lt 96)) {
            # Mouth and eye-socket recess.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 12.0
            $color = New-Color 255 `
                (45 + ($noise * 8)) `
                (37 + ($noise * 7)) `
                (40 + ($noise * 8))
        }
        elseif (($x -ge 96) -and ($x -lt 160) -and ($y -ge 96) -and ($y -lt 160)) {
            # Slate shoulder and hip armor, deliberately matte.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 13.8
            $color = New-Color 255 `
                (77 + ($noise * 12) + ($grain * 0.4)) `
                (79 + ($noise * 12)) `
                (94 + ($noise * 14))
        }
        elseif (($x -ge 160) -and ($x -lt 224) -and ($y -ge 96) -and ($y -lt 160)) {
            # Dirty pale forearm and calf wrappings.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 15.4
            $color = New-Color 255 `
                (130 + ($noise * 13) + ($grain * 0.5)) `
                (127 + ($noise * 12)) `
                (133 + ($noise * 14))
        }
        elseif (($x -lt 96) -and ($y -ge 128) -and ($y -lt 224)) {
            # Dark indigo kilt.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 17.1
            $color = New-Color 255 `
                (40 + ($noise * 9)) `
                (40 + ($noise * 9)) `
                (70 + ($noise * 15))
        }
        elseif (($x -ge 96) -and ($x -lt 128) -and ($y -ge 160) -and ($y -lt 224)) {
            # Muted brick-red front cloth panels.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 18.8
            $color = New-Color 255 `
                (102 + ($noise * 13)) `
                (51 + ($noise * 8)) `
                (55 + ($noise * 9))
        }
        elseif (($x -ge 128) -and ($x -lt 192) -and ($y -ge 160) -and ($y -lt 224)) {
            # Central skull crest and dull leather belt.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 20.1
            $color = New-Color 255 `
                (101 + ($noise * 14)) `
                (87 + ($noise * 12)) `
                (78 + ($noise * 11))
        }
        elseif (($x -ge 192) -and ($y -ge 160) -and ($y -lt 224)) {
            # Dark foot, palm, and joint material.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 21.9
            $color = New-Color 255 `
                (57 + ($noise * 10)) `
                (49 + ($noise * 9)) `
                (51 + ($noise * 9))
        }
        else {
            # General deep joint/under-layer swatch.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 23.4
            $color = New-Color 255 `
                (51 + ($noise * 9)) `
                (43 + ($noise * 8)) `
                (47 + ($noise * 9))
        }

        $bitmap.SetPixel($x, $y, $color)
    }
}

$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# Low-contrast wrap overlaps.
$wrapPen = [System.Drawing.Pen]::new(
    [System.Drawing.Color]::FromArgb(70, 87, 82, 89),
    1.4
)
for ($lineY = 103; $lineY -lt 158; $lineY += 8) {
    $graphics.DrawBezier($wrapPen, 161, $lineY, 177, ($lineY + 2), 205, ($lineY - 2), 223, ($lineY + 1))
}

# Subtle horn growth grooves.
$hornPen = [System.Drawing.Pen]::new(
    [System.Drawing.Color]::FromArgb(48, 62, 58, 65),
    1.2
)
for ($lineX = 166; $lineX -lt 222; $lineX += 11) {
    $graphics.DrawBezier($hornPen, $lineX, 3, ($lineX - 4), 29, ($lineX + 5), 61, ($lineX + 1), 92)
}

# Soft armor striation and cloth folds.
$armorPen = [System.Drawing.Pen]::new(
    [System.Drawing.Color]::FromArgb(40, 43, 45, 58),
    1.2
)
$graphics.DrawBezier($armorPen, 98, 109, 115, 101, 140, 119, 158, 108)
$graphics.DrawBezier($armorPen, 98, 139, 119, 127, 143, 148, 159, 134)

$clothPen = [System.Drawing.Pen]::new(
    [System.Drawing.Color]::FromArgb(38, 19, 19, 40),
    1.1
)
for ($lineX = 10; $lineX -lt 94; $lineX += 16) {
    $graphics.DrawBezier($clothPen, $lineX, 132, ($lineX + 4), 163, ($lineX - 3), 192, ($lineX + 1), 222)
}

$wrapPen.Dispose()
$hornPen.Dispose()
$armorPen.Dispose()
$clothPen.Dispose()
$graphics.Dispose()

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
$outputDirectory = [System.IO.Path]::GetDirectoryName($resolvedOutput)
[System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null

$temporaryOutput = "$resolvedOutput.tmp.png"
$bitmap.Save($temporaryOutput, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()

Move-Item -LiteralPath $temporaryOutput -Destination $resolvedOutput -Force
Write-Output $resolvedOutput
