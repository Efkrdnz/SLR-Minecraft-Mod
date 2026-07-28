param(
    [Parameter(Mandatory = $true)]
    [string]$OutputPath
)

Add-Type -AssemblyName System.Drawing

$width = 128
$height = 128
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

    $broad = [Math]::Sin(($X * 0.105) + $Offset + ([Math]::Sin($Y * 0.071) * 0.7))
    $cross = [Math]::Sin((($X + $Y) * 0.061) - ($Offset * 0.37))
    $fine = [Math]::Cos(($Y * 0.143) + ($X * 0.037) + ($Offset * 0.23))
    return (($broad * 0.5) + ($cross * 0.3) + ($fine * 0.2))
}

$random = [System.Random]::new(9247)

for ($y = 0; $y -lt $height; $y++) {
    for ($x = 0; $x -lt $width; $x++) {
        $color = [System.Drawing.Color]::Transparent

        if ($x -lt 80) {
            # Main exoskeleton: subdued iron-red with broad, soft mottling and
            # minute organic grain. The narrow value range keeps it matte.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 1.7
            $grain = (($random.NextDouble() - 0.5) * 4.0)
            $verticalShade = (($y / 127.0) - 0.5) * -5.0
            $color = New-Color 255 `
                (101 + ($noise * 17) + $grain + $verticalShade) `
                (48 + ($noise * 9) + ($grain * 0.45)) `
                (44 + ($noise * 7) + ($grain * 0.35))
        }
        elseif ($y -lt 48) {
            # Legs and joint undersides.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 4.2
            $grain = (($random.NextDouble() - 0.5) * 3.0)
            $color = New-Color 255 `
                (67 + ($noise * 12) + $grain) `
                (34 + ($noise * 6)) `
                (34 + ($noise * 5))
        }
        elseif (($x -lt 96) -and ($y -lt 64)) {
            # Faceted eyes: a subdued plum-violet from the anime reference,
            # deliberately dark enough to avoid an emissive/robotic read.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 8.1
            $color = New-Color 255 `
                (73 + ($noise * 10)) `
                (55 + ($noise * 7)) `
                (79 + ($noise * 11))
        }
        elseif ($y -lt 80) {
            # Mandibles, antennae, claws, and mouth plates.
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 6.3
            $grain = (($random.NextDouble() - 0.5) * 3.0)
            $color = New-Color 255 `
                (58 + ($noise * 14) + $grain) `
                (29 + ($noise * 6)) `
                (29 + ($noise * 5))
        }
        else {
            # Smoky translucent wings. Alpha and color change gradually so the
            # surface feels membranous rather than like a hard plastic plate.
            $u = ($x - 80) / 47.0
            $v = ($y - 80) / 47.0
            $edge = [Math]::Min(
                [Math]::Min($u, 1.0 - $u),
                [Math]::Min($v, 1.0 - $v)
            )
            $edgeFade = [Math]::Max(0.0, [Math]::Min(1.0, $edge * 7.0))
            $noise = Get-SmoothNoise -X $x -Y $y -Offset 10.4
            $alpha = (64 + ($edgeFade * 64) + ($noise * 7))
            $color = New-Color $alpha `
                (169 + ($noise * 7)) `
                (158 + ($noise * 7)) `
                (148 + ($noise * 8))
        }

        $bitmap.SetPixel($x, $y, $color)
    }
}

# Add a few quiet, ant-wing-like veins. Their low opacity keeps transitions
# soft and avoids a synthetic panel-line appearance.
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$veinPen = [System.Drawing.Pen]::new(
    [System.Drawing.Color]::FromArgb(68, 58, 39, 42),
    1.25
)
$minorVeinPen = [System.Drawing.Pen]::new(
    [System.Drawing.Color]::FromArgb(42, 70, 48, 49),
    0.8
)

$graphics.DrawBezier($veinPen, 82, 83, 94, 88, 108, 96, 124, 106)
$graphics.DrawBezier($veinPen, 83, 87, 95, 97, 104, 108, 117, 124)
$graphics.DrawBezier($minorVeinPen, 94, 90, 98, 99, 100, 106, 101, 116)
$graphics.DrawBezier($minorVeinPen, 104, 96, 111, 101, 116, 106, 122, 113)
$graphics.DrawBezier($minorVeinPen, 89, 98, 96, 103, 103, 106, 112, 108)

$graphics.Dispose()
$veinPen.Dispose()
$minorVeinPen.Dispose()

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
$outputDirectory = [System.IO.Path]::GetDirectoryName($resolvedOutput)
[System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null

$temporaryOutput = "$resolvedOutput.tmp.png"
$bitmap.Save($temporaryOutput, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()

Move-Item -LiteralPath $temporaryOutput -Destination $resolvedOutput -Force
Write-Output $resolvedOutput
