# ==============================================================================
# download_tiles.ps1
# Telecharge les tuiles OSM pour la Tunisie dans src\main\resources\tiles
# Usage: cd "c:\Users\MSI\Desktop\psycoproject\investi-_java"; .\download_tiles.ps1
# ==============================================================================

$outputDir = "src\main\resources\tiles"

function Get-TileX([double]$lon, [int]$zoom) {
    return [int][Math]::Floor(($lon + 180.0) / 360.0 * [Math]::Pow(2, $zoom))
}
function Get-TileY([double]$lat, [int]$zoom) {
    $rad = $lat * [Math]::PI / 180.0
    return [int][Math]::Floor((1.0 - [Math]::Log([Math]::Tan($rad) + 1.0 / [Math]::Cos($rad)) / [Math]::PI) / 2.0 * [Math]::Pow(2, $zoom))
}

$zones = @(
    @{ name="Tunisie";     minLat=30.2; maxLat=37.6; minLon=7.5;  maxLon=11.6; zooms=8..10 },
    @{ name="GrandTunis";  minLat=36.5; maxLat=37.1; minLon=9.9;  maxLon=10.6; zooms=11..14 },
    @{ name="TunisCentre"; minLat=36.77; maxLat=36.87; minLon=10.10; maxLon=10.25; zooms=15..15 }
)

$servers = @("a","b","c")
$client  = New-Object System.Net.WebClient
$client.Headers.Add("User-Agent","Mozilla/5.0 TileDownloader/1.0")

$total = 0; $done = 0; $fail = 0

foreach ($z in $zones) {
    foreach ($zoom in $z.zooms) {
        $x0 = Get-TileX $z.minLon $zoom;  $x1 = Get-TileX $z.maxLon $zoom
        $y0 = Get-TileY $z.maxLat $zoom;  $y1 = Get-TileY $z.minLat $zoom
        $total += ($x1-$x0+1)*($y1-$y0+1)
    }
}
Write-Host "Total tuiles: $total"

foreach ($z in $zones) {
    Write-Host "=== Zone: $($z.name) ==="
    foreach ($zoom in $z.zooms) {
        $x0 = Get-TileX $z.minLon $zoom;  $x1 = Get-TileX $z.maxLon $zoom
        $y0 = Get-TileY $z.maxLat $zoom;  $y1 = Get-TileY $z.minLat $zoom
        for ($x = $x0; $x -le $x1; $x++) {
            for ($y = $y0; $y -le $y1; $y++) {
                $dir  = Join-Path $outputDir "$zoom\$x"
                $file = Join-Path $dir "$y.png"
                if (Test-Path $file) { $done++; continue }
                New-Item -ItemType Directory -Force -Path $dir | Out-Null
                $sub = $servers[$done % 3]
                $url = "https://$sub.tile.openstreetmap.org/$zoom/$x/$y.png"
                try {
                    $client.DownloadFile($url, $file)
                    $done++
                    if ($done % 50 -eq 0) { Write-Host "  [$done/$total] z=$zoom x=$x y=$y" }
                } catch {
                    $fail++
                }
                Start-Sleep -Milliseconds 100
            }
        }
    }
}
Write-Host "Termine: $done OK, $fail erreurs"
Write-Host "Dossier: $(Resolve-Path $outputDir)"
