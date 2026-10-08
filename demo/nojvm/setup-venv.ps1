<#
.SYNOPSIS
  Create demo/work/venv/ and install the ANTLR Python runtime into it.

.DESCRIPTION
  The runtime version MUST match the ANTLR tool version used by gen-parser.ps1
  exactly (4.13.2). The generated parser calls checkVersion("4.13.2") at
  construction time; a mismatch only warns, but a mismatched serialised ATN is
  a real failure mode, so the pin is exact rather than a range.

  antlr4-python3-runtime is pure Python with no transitive dependencies, which
  is the whole dependency footprint of this lane.

.PARAMETER NoTiktoken
Skip the pinned tiktoken installation (only the ANTLR runtime is then installed; ai_context.py needs tiktoken).

.PARAMETER WheelDir
  Optional local wheel directory for an offline install (pip --no-index
  --find-links). Use when the demo host has no network. Unless -NoTiktoken is
  given, an offline host also needs the tokenizer's o200k_base encoding file
  staged in a cache directory named by the TIKTOKEN_CACHE_DIR environment
  variable (demo/nojvm/README.md, step 2): wheels alone are not enough.
#>
[CmdletBinding()]
param(
    [string] $VenvDir,
    [string] $Python = 'py',
    [string] $WheelDir,
    [switch] $Recreate,
    [switch] $UpgradePip,
    [switch] $NoTiktoken
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$ANTLR_RUNTIME_VERSION = '4.13.2'
$TIKTOKEN_VERSION = '0.14.0'

$here    = Split-Path -Parent $MyInvocation.MyCommand.Path
$demoDir = Split-Path -Parent $here
if (-not $VenvDir) { $VenvDir = Join-Path $demoDir 'work/venv' }

if ($Recreate -and (Test-Path -LiteralPath $VenvDir)) {
    Write-Host "[setup-venv] removing existing venv: $VenvDir"
    Remove-Item -LiteralPath $VenvDir -Recurse -Force
}

# `py -3` is the Windows launcher form; a direct interpreter path also works.
$createArgs = if ($Python -eq 'py') { @('-3', '-m', 'venv', $VenvDir) }
              else { @('-m', 'venv', $VenvDir) }

if (-not (Test-Path -LiteralPath (Join-Path $VenvDir 'Scripts/python.exe'))) {
    Write-Host "[setup-venv] creating venv at $VenvDir"
    & $Python @createArgs
    if ($LASTEXITCODE -ne 0) { Write-Error "venv creation failed (exit $LASTEXITCODE)" }
} else {
    Write-Host "[setup-venv] reusing existing venv at $VenvDir"
}

$venvPy = Join-Path $VenvDir 'Scripts/python.exe'
if (-not (Test-Path -LiteralPath $venvPy)) {
    Write-Error "venv python not found at $venvPy"
}

if ($UpgradePip) {
    & $venvPy -m pip install --upgrade pip
    if ($LASTEXITCODE -ne 0) { Write-Error "pip upgrade failed (exit $LASTEXITCODE)" }
}

$pkg = "antlr4-python3-runtime==$ANTLR_RUNTIME_VERSION"
$pipArgs = @('-m', 'pip', 'install', '--disable-pip-version-check')
if ($WheelDir) {
    if (-not (Test-Path -LiteralPath $WheelDir)) { Write-Error "WheelDir not found: $WheelDir" }
    $pipArgs += @('--no-index', '--find-links', (Resolve-Path $WheelDir).Path)
    Write-Host "[setup-venv] offline install from $WheelDir"
}
$pipArgs += $pkg

Write-Host "[setup-venv] installing $pkg"
& $venvPy @pipArgs
if ($LASTEXITCODE -ne 0) { Write-Error "pip install failed (exit $LASTEXITCODE)" }

# The AI structural-context experiment (ai_context.py) counts exact tokens with
# tiktoken; install it into the same venv unless -NoTiktoken is given (an offline
# install needs the wheels of tiktoken AND its dependencies - regex, requests,
# charset_normalizer, idna, urllib3, certifi - in -WheelDir; `pip download
# tiktoken==0.14.0 -d <dir>` collects them - and the staged encoding cache below).
if (-not $NoTiktoken) {
    $tiktokenPkg = "tiktoken==$TIKTOKEN_VERSION"
    $tikArgs = @('-m', 'pip', 'install', '--disable-pip-version-check')
    if ($WheelDir) { $tikArgs += @('--no-index', '--find-links', (Resolve-Path $WheelDir).Path) }
    $tikArgs += $tiktokenPkg
    Write-Host "[setup-venv] installing $tiktokenPkg"
    & $venvPy @tikArgs
    if ($LASTEXITCODE -ne 0) { Write-Error "pip install failed (exit $LASTEXITCODE)" }
}

# tiktoken downloads the o200k_base encoding on first use and caches it; an
# offline host must be given a staged cache through TIKTOKEN_CACHE_DIR.
if (-not $NoTiktoken) {
    if ($env:TIKTOKEN_CACHE_DIR) {
        Write-Host "[setup-venv] tiktoken encoding cache: $env:TIKTOKEN_CACHE_DIR (TIKTOKEN_CACHE_DIR)"
    } else {
        Write-Host '[setup-venv] tiktoken encoding cache: the default one (TIKTOKEN_CACHE_DIR not set)'
        if ($WheelDir) {
            Write-Host '[setup-venv] offline install: o200k_base must already be cached; see README.md step 2'
        }
    }
}

# Verify the runtime imports (and tiktoken with its o200k_base encoding when installed).
$check = @'
import antlr4, sys
from antlr4.Recognizer import Recognizer
print("antlr4 runtime OK, python " + sys.version.split()[0])
try:
    import tiktoken
except ImportError:
    print("tiktoken not installed (-NoTiktoken): ai_context.py needs it")
else:
    try:
        tiktoken.get_encoding("o200k_base")
    except Exception as exc:
        print("ERROR: tiktoken could not load the o200k_base encoding: " + type(exc).__name__)
        print("It is downloaded on first use; without network, set TIKTOKEN_CACHE_DIR to a cache")
        print("staged on a connected machine (demo/nojvm/README.md, step 2).")
        sys.exit(3)
    print("tiktoken " + tiktoken.__version__ + " OK (o200k_base)")
'@
$check | & $venvPy -
if ($LASTEXITCODE -eq 3) { Write-Error 'the tiktoken encoding is not available (see the message above)' }
if ($LASTEXITCODE -ne 0) { Write-Error 'antlr4 runtime did not import cleanly' }

& $venvPy -m pip show antlr4-python3-runtime |
    Select-String -Pattern '^(Name|Version|Location):'

Write-Host ''
Write-Host '[setup-venv] done.'
Write-Host "[setup-venv] venv python: $venvPy"
Write-Host '[setup-venv] analytics.py finds this venv automatically; you can also run'
Write-Host "[setup-venv]   & '$venvPy' analytics.py --roots ..."
