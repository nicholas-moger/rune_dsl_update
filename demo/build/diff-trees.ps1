#Requires -Version 7.0
<#
.SYNOPSIS
    Rune DSL stats demo - BUILD section byte-parity diff (CONTRACTS section 7E).

.DESCRIPTION
    Byte-compares every relative path present in EITHER of two generated trees and
    reports four counts: identicalFiles, differingFiles, onlyInA, onlyInB. Writes a
    `build.paritydiff` receipt (section `build`) to demo/receipts/.

    Comparison is exact:
      * a path present in both is IDENTICAL only if the two files have the same
        length AND the same SHA-256 (length is checked first purely as a cheap
        early-out; different length already proves different bytes);
      * paths are compared case-SENSITIVELY (ordinal), because Java package paths
        are, even though NTFS is not.

    Differences are DATA, not failure: this script exits 0 whether the trees match
    or not. It exits 1 only when a root directory it was told to read is missing.

.PARAMETER A
    First tree. Default demo/work/build-legacy-out.

.PARAMETER B
    Second tree. Default demo/work/build-plus-out.

.PARAMETER AlsoAgainst
    Optional third tree (CONTRACTS section 7E: demo/work/gen-m1). When given, A-vs-C
    and B-vs-C are computed as well and reported under metrics.alsoAgainst.

.PARAMETER ShowFirst
    How many sample paths to list per category. Default 20.

.EXAMPLE
    pwsh -File demo/build/diff-trees.ps1
    pwsh -File demo/build/diff-trees.ps1 -AlsoAgainst demo/work/gen-m1
#>
[CmdletBinding()]
param(
    [string] $A           = (Join-Path $PSScriptRoot '../work/build-legacy-out'),
    [string] $B           = (Join-Path $PSScriptRoot '../work/build-plus-out'),
    [string] $AlsoAgainst = '',
    [string] $ReceiptDir  = (Join-Path $PSScriptRoot '../receipts'),
    [int]    $ShowFirst   = 20,
    # Hash with CRLF-to-LF normalisation first. This is the repo's DECLARED Layer-3
    # normalisation (line endings): the legacy Xtext toolchain writes the PLATFORM line
    # separator (CRLF on Windows) while the fork writes the golden LF bytes verbatim, so a
    # raw byte compare on Windows reports every file as differing when the CONTENT is
    # identical. The receipt records normalizeEol so the claim is never silently softened.
    [switch] $NormalizeEol,
    # Receipt identity. The script serves several tree pairs (build parity, codegen
    # legacy-vs-m1, m1-vs-m2, m1-vs-m3); each pair needs its own id or successive runs
    # overwrite one another's receipt (the filename is "<Id>.json").
    [string] $Id      = 'build.paritydiff',
    [string] $Title   = 'Generated-tree byte parity: legacy 9.83.0 vs rune-dsl-plus fork',
    [string] $Section = 'build'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ReceiptDir = [System.IO.Path]::GetFullPath($ReceiptDir)
New-Item -ItemType Directory -Force -Path $ReceiptDir | Out-Null

# ------------------------------------------------------- demo protocol ----

# NOTE: these write STRAIGHT to the process stdout handle, never to PowerShell's
# success stream. Write-Output here would be captured as part of the enclosing
# function's return value (Compare-Trees returns a hashtable, and every progress
# line would end up prepended to it).
function Write-Demo {
    param([Parameter(Mandatory)] [hashtable] $Event)
    [Console]::Out.WriteLine('##DEMO## ' + ($Event | ConvertTo-Json -Compress -Depth 8))
}

function Write-Note {
    param([string] $Text)
    [Console]::Out.WriteLine("[diff-trees] $Text")
}

# ------------------------------------------------------------- helpers ----

$Sha = [System.Security.Cryptography.SHA256]::Create()

function Get-FileSha {
    param([Parameter(Mandatory)] [string] $Path)
    if ($NormalizeEol) {
        $bytes = [System.IO.File]::ReadAllBytes($Path)
        $ms = [System.IO.MemoryStream]::new($bytes.Length)
        for ($i = 0; $i -lt $bytes.Length; $i++) {
            if ($bytes[$i] -eq 13 -and ($i + 1) -lt $bytes.Length -and $bytes[$i + 1] -eq 10) { continue }
            $ms.WriteByte($bytes[$i])
        }
        $ms.Position = 0
        try { return [System.BitConverter]::ToString($Sha.ComputeHash($ms)) }
        finally { $ms.Dispose() }
    }
    $stream = [System.IO.File]::OpenRead($Path)
    try { return [System.BitConverter]::ToString($Sha.ComputeHash($stream)) }
    finally { $stream.Dispose() }
}

# relPath (forward-slashed, ordinal-keyed) -> FileInfo
function Get-TreeIndex {
    param([Parameter(Mandatory)] [string] $Root)
    $full = [System.IO.Path]::GetFullPath($Root).TrimEnd('\', '/')
    $map  = [System.Collections.Generic.Dictionary[string, System.IO.FileInfo]]::new(
                [System.StringComparer]::Ordinal)
    foreach ($f in Get-ChildItem -LiteralPath $full -Recurse -File -Force -ErrorAction SilentlyContinue) {
        $rel = $f.FullName.Substring($full.Length + 1).Replace('\', '/')
        $map[$rel] = $f
    }
    return $map
}

function Compare-Trees {
    param(
        [Parameter(Mandatory)] [string] $RootA,
        [Parameter(Mandatory)] [string] $RootB,
        [Parameter(Mandatory)] [string] $Label
    )

    Write-Note "indexing $Label ..."
    $mapA = Get-TreeIndex -Root $RootA
    $mapB = Get-TreeIndex -Root $RootB

    $union = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach ($k in $mapA.Keys) { [void] $union.Add($k) }
    foreach ($k in $mapB.Keys) { [void] $union.Add($k) }

    $total     = $union.Count
    $identical = 0
    $differing = [System.Collections.Generic.List[string]]::new()
    $onlyA     = [System.Collections.Generic.List[string]]::new()
    $onlyB     = [System.Collections.Generic.List[string]]::new()
    $processed = 0

    Write-Demo @{ ev = 'start'; id = "build.paritydiff.$Label"; detail = "$total distinct relative paths" }

    foreach ($rel in ($union | Sort-Object -CaseSensitive)) {
        $inA = $mapA.ContainsKey($rel)
        $inB = $mapB.ContainsKey($rel)

        if ($inA -and $inB) {
            $fa = $mapA[$rel]; $fb = $mapB[$rel]
            if ((-not $NormalizeEol) -and $fa.Length -ne $fb.Length) {
                $differing.Add($rel)
            }
            elseif ((Get-FileSha -Path $fa.FullName) -eq (Get-FileSha -Path $fb.FullName)) {
                $identical++
            }
            else {
                $differing.Add($rel)
            }
        }
        elseif ($inA) { $onlyA.Add($rel) }
        else          { $onlyB.Add($rel) }

        $processed++
        if (($processed % 250) -eq 0) {
            Write-Demo @{ ev = 'progress'; n = $processed; of = $total }
        }
    }
    if (($processed % 250) -ne 0) {
        Write-Demo @{ ev = 'progress'; n = $processed; of = $total }
    }

    return [ordered]@{
        rootA           = $RootA
        rootB           = $RootB
        totalPaths      = $total
        filesInA        = $mapA.Count
        filesInB        = $mapB.Count
        identicalFiles  = $identical
        differingFiles  = $differing.Count
        onlyInA         = $onlyA.Count
        onlyInB         = $onlyB.Count
        firstDiffering  = @($differing  | Select-Object -First $ShowFirst)
        firstOnlyInA    = @($onlyA      | Select-Object -First $ShowFirst)
        firstOnlyInB    = @($onlyB      | Select-Object -First $ShowFirst)
    }
}

function Write-Summary {
    param([string] $Label, $R)
    Write-Note "--- $Label ---"
    Write-Note ("  A = {0}  ({1} files)" -f $R.rootA, $R.filesInA)
    Write-Note ("  B = {0}  ({1} files)" -f $R.rootB, $R.filesInB)
    Write-Note ("  identicalFiles = {0}" -f $R.identicalFiles)
    Write-Note ("  differingFiles = {0}" -f $R.differingFiles)
    Write-Note ("  onlyInA        = {0}" -f $R.onlyInA)
    Write-Note ("  onlyInB        = {0}" -f $R.onlyInB)
    foreach ($cat in @(
        @{ n = 'differing'; v = $R.firstDiffering },
        @{ n = 'only in A'; v = $R.firstOnlyInA },
        @{ n = 'only in B'; v = $R.firstOnlyInB })) {
        if ($cat.v.Count -gt 0) {
            Write-Note ("  first {0} {1}:" -f $cat.v.Count, $cat.n)
            foreach ($p in $cat.v) { Write-Note "    $p" }
        }
    }
}

# ---------------------------------------------------------------- main ----

$A = [System.IO.Path]::GetFullPath($A)
$B = [System.IO.Path]::GetFullPath($B)
if ($AlsoAgainst) { $AlsoAgainst = [System.IO.Path]::GetFullPath($AlsoAgainst) }

$missing = @()
foreach ($r in @($A, $B)) { if (-not (Test-Path -LiteralPath $r)) { $missing += $r } }
if ($AlsoAgainst -and -not (Test-Path -LiteralPath $AlsoAgainst)) { $missing += $AlsoAgainst }
if ($missing.Count -gt 0) {
    foreach ($m in $missing) { Write-Demo @{ ev = 'error'; message = "tree not found: $m" } }
    Write-Note 'nothing compared; run demo/build/measure-build.ps1 first.'
    exit 1
}

$startedAt = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
$sw = [System.Diagnostics.Stopwatch]::StartNew()

$primary = Compare-Trees -RootA $A -RootB $B -Label 'legacy-vs-plus'
Write-Summary -Label 'legacy (A) vs plus (B)' -R $primary

$metrics = [ordered]@{
    normalizeEol    = [bool]$NormalizeEol
    identicalFiles = $primary.identicalFiles
    differingFiles = $primary.differingFiles
    onlyInA        = $primary.onlyInA
    onlyInB        = $primary.onlyInB
    totalPaths     = $primary.totalPaths
    rootA          = $primary.rootA
    rootB          = $primary.rootB
    filesInA       = $primary.filesInA
    filesInB       = $primary.filesInB
    firstDiffering = $primary.firstDiffering
    firstOnlyInA   = $primary.firstOnlyInA
    firstOnlyInB   = $primary.firstOnlyInB
}

$notes = 'SHA-256 byte compare over the union of relative paths; length checked first as a cheap early-out. ' +
         'Paths compared case-sensitively (ordinal). Differences are the RESULT, not a failure: exit stays 0.'

if ($AlsoAgainst) {
    $ac = Compare-Trees -RootA $A -RootB $AlsoAgainst -Label 'legacy-vs-third'
    Write-Summary -Label 'legacy (A) vs third (C)' -R $ac
    $bc = Compare-Trees -RootA $B -RootB $AlsoAgainst -Label 'plus-vs-third'
    Write-Summary -Label 'plus (B) vs third (C)' -R $bc
    $metrics['alsoAgainst'] = [ordered]@{
        root  = ([System.IO.Path]::GetFullPath($AlsoAgainst))
        aVsC  = $ac
        bVsC  = $bc
    }
    $notes += ' alsoAgainst holds the same comparison run against a third tree (onlyInB there means "only in the third tree").'
}

$sw.Stop()

$hostBlock = @{}
try { $hostBlock['os']  = (Get-CimInstance Win32_OperatingSystem -ErrorAction Stop).Caption.Trim() } catch { }
try { $hostBlock['cpu'] = (Get-CimInstance Win32_Processor -ErrorAction Stop | Select-Object -First 1).Name.Trim() } catch { }

$cmd = "pwsh -File demo/build/diff-trees.ps1 -A `"$A`" -B `"$B`""
if ($AlsoAgainst) { $cmd += " -AlsoAgainst `"$AlsoAgainst`"" }
if ($NormalizeEol) { $cmd += ' -NormalizeEol' }
if ($Id -ne 'build.paritydiff') { $cmd += " -Id $Id" }
if ($PSBoundParameters.ContainsKey('Title'))   { $cmd += " -Title `"$Title`"" }
if ($PSBoundParameters.ContainsKey('Section')) { $cmd += " -Section $Section" }

$receipt = [ordered]@{
    id         = $Id
    section    = $Section
    title      = $Title
    command    = $cmd
    startedAt  = $startedAt
    durationMs = [int] $sw.Elapsed.TotalMilliseconds
    metrics    = $metrics
    notes      = $notes
}
if ($hostBlock.Count -gt 0) { $receipt['host'] = $hostBlock }

$path = Join-Path $ReceiptDir "$Id.json"
# LF endings + UTF-8 without BOM, per CONTRACTS section 0.8.
$json = ($receipt | ConvertTo-Json -Depth 10) -replace "`r`n", "`n"
[System.IO.File]::WriteAllText($path, $json + "`n", (New-Object System.Text.UTF8Encoding($false)))
Write-Note "receipt -> $path"

Write-Demo @{ ev = 'done'; metrics = $metrics }
exit 0
