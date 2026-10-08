<#
.SYNOPSIS
  Generate the ANTLR4 Python3 parser for the Rune grammar.

.DESCRIPTION
  Runs the ANTLR4 tool jar over the fork's OWN grammars
  (rune-parser/src/main/antlr4/com/regnosys/rosetta/parser/*.g4) with
  -Dlanguage=Python3, emitting a flat package into demo/work/nojvm-gen/.

  This is the ONE step in the no-JVM lane that needs java, and it is a
  build-time step: nothing at query time touches a JVM. Run it once.

  The grammars are portable precisely because they carry ZERO embedded target
  actions and ZERO semantic predicates - the same .g4 that drives the Java
  engine drives CPython unmodified.

.NOTES
  RosettaParser.g4 declares `options { tokenVocab = RosettaLexer; }`, so the
  lexer must be processed first and its RosettaLexer.tokens must be findable.
  Both grammars are passed in one invocation (the ANTLR tool topologically
  sorts them by tokenVocab) with -lib pointing at the output directory. If
  that ever stops working, the script falls back to two ordered invocations.
#>
[CmdletBinding()]
param(
    [string] $AntlrJar,
    [string] $GrammarDir,
    [string] $OutDir,
    [switch] $NoClean
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$here    = Split-Path -Parent $MyInvocation.MyCommand.Path
$demoDir = Split-Path -Parent $here
$repoDir = Split-Path -Parent $demoDir

if (-not $GrammarDir) {
    $GrammarDir = Join-Path $repoDir 'rune-parser/src/main/antlr4/com/regnosys/rosetta/parser'
}
if (-not $OutDir) {
    $OutDir = Join-Path $demoDir 'work/nojvm-gen'
}

# CONTRACTS 7C names the jar antlr-4.13.2-complete.jar; accept the short name too.
if (-not $AntlrJar) {
    $candidates = @(
        (Join-Path $demoDir 'work/antlr-4.13.2-complete.jar'),
        (Join-Path $demoDir 'work/antlr-complete.jar')
    )
    $AntlrJar = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $AntlrJar) {
        Write-Error ("ANTLR tool jar not found. Looked for:`n  " +
                     ($candidates -join "`n  ") +
                     "`nThe integrator downloads it during bootstrap (ANTLR 4.13.2).")
    }
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error 'java is not on PATH. Parser generation is the only step that needs it.'
}
if (-not (Test-Path -LiteralPath $AntlrJar)) {
    Write-Error "ANTLR tool jar not found: $AntlrJar"
}

# The ANTLR4 TOOL itself depends on the ANTLR3 runtime and StringTemplate4. A true
# "-complete" jar bundles both; the plain org.antlr:antlr4 tool jar does not
# (NoClassDefFoundError: org/antlr/runtime/tree/CommonTree). Build a classpath:
# the jar we were given, plus - when the local Maven cache holds them, which this
# repo's own build guarantees - the two companions. Harmless when the jar really
# is complete; rescues the run when it is not.
$AntlrCp = $AntlrJar
$m2 = Join-Path $HOME '.m2/repository'
$hermetic = Join-Path $demoDir 'work/.m2'
foreach ($companion in @(
    'org/antlr/antlr4-runtime/4.13.2/antlr4-runtime-4.13.2.jar',
    'org/antlr/antlr-runtime/3.5.3/antlr-runtime-3.5.3.jar',
    'org/antlr/ST4/4.3.4/ST4-4.3.4.jar',
    'org/abego/treelayout/org.abego.treelayout.core/1.0.3/org.abego.treelayout.core-1.0.3.jar')) {
    foreach ($repo in @($hermetic, $m2)) {
        $p = Join-Path $repo $companion
        if (Test-Path -LiteralPath $p) { $AntlrCp += [IO.Path]::PathSeparator + $p; break }
    }
}

# Absolute grammar paths keep the ANTLR output flat (relative paths with
# directories get mirrored under -o). -Xexact-output-dir enforces it anyway.
$lexerG4  = (Resolve-Path (Join-Path $GrammarDir 'RosettaLexer.g4')).Path
$parserG4 = (Resolve-Path (Join-Path $GrammarDir 'RosettaParser.g4')).Path

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$OutDir = (Resolve-Path $OutDir).Path

if (-not $NoClean) {
    # Delete only what this script generates - never the directory itself.
    foreach ($stale in @('RosettaLexer.py', 'RosettaLexer.tokens', 'RosettaLexer.interp',
                         'RosettaParser.py', 'RosettaParser.tokens', 'RosettaParser.interp',
                         'RosettaParserListener.py', 'RosettaParserVisitor.py')) {
        $p = Join-Path $OutDir $stale
        if (Test-Path -LiteralPath $p) { Remove-Item -LiteralPath $p -Force }
    }
}

# Absolute grammar paths already make ANTLR write flat into -o;
# -Xexact-output-dir is belt-and-braces, so the fallback below drops it in case
# an ANTLR build ever stops recognising the option.
$base = @(
    '-Dlanguage=Python3',
    '-o', $OutDir,
    '-lib', $OutDir,
    '-visitor',
    '-listener',
    '-long-messages'
)
$common = $base + @('-Xexact-output-dir')

Write-Host "##DEMO## {`"ev`":`"start`",`"id`":`"nojvm.genparser`",`"detail`":`"ANTLR 4.13.2 -> Python3`"}"
Write-Host "[gen-parser] jar     : $AntlrJar"
Write-Host "[gen-parser] grammars: $GrammarDir"
Write-Host "[gen-parser] out     : $OutDir"

& java -cp $AntlrCp org.antlr.v4.Tool @common $lexerG4 $parserG4
$rc = $LASTEXITCODE

$expected = @('RosettaLexer.py', 'RosettaParser.py',
              'RosettaLexer.tokens', 'RosettaParser.tokens')
$missing = $expected | Where-Object { -not (Test-Path -LiteralPath (Join-Path $OutDir $_)) }

if ($rc -ne 0 -or $missing) {
    Write-Host "[gen-parser] single-invocation run incomplete (exit $rc; missing: $($missing -join ', '))."
    Write-Host '[gen-parser] falling back to two ordered invocations (lexer, then parser).'
    & java -cp $AntlrCp org.antlr.v4.Tool @base $lexerG4
    if ($LASTEXITCODE -ne 0) { Write-Error "ANTLR failed on RosettaLexer.g4 (exit $LASTEXITCODE)" }
    & java -cp $AntlrCp org.antlr.v4.Tool @base $parserG4
    if ($LASTEXITCODE -ne 0) { Write-Error "ANTLR failed on RosettaParser.g4 (exit $LASTEXITCODE)" }
    $missing = $expected | Where-Object { -not (Test-Path -LiteralPath (Join-Path $OutDir $_)) }
    if ($missing) { Write-Error "Generation incomplete; still missing: $($missing -join ', ')" }
}

Write-Host ''
Write-Host '[gen-parser] generated:'
Get-ChildItem -LiteralPath $OutDir -File |
    Where-Object { $_.Name -like 'Rosetta*' } |
    Sort-Object Name |
    ForEach-Object { Write-Host ("  {0,-32} {1,9:N0} bytes" -f $_.Name, $_.Length) }

Write-Host ''
Write-Host '[gen-parser] done. Next: setup-venv.ps1, then analytics.py'
Write-Host "##DEMO## {`"ev`":`"done`",`"metrics`":{`"outDir`":`"$($OutDir -replace '\\','/')`"}}"
