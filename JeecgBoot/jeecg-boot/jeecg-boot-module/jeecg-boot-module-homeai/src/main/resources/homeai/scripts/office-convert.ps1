param(
    [Parameter(Mandatory = $true)][string]$SourcePath,
    [Parameter(Mandatory = $true)][string]$OutDir,
    [Parameter(Mandatory = $true)][string]$TargetFormat
)

$ErrorActionPreference = 'Stop'

function Release-ComObject([object]$Obj) {
    if ($null -ne $Obj) {
        [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($Obj)
    }
}

function Convert-Word([string]$Src, [string]$Out, [string]$Fmt) {
    $word = $null
    $doc = $null
    try {
        $word = New-Object -ComObject Word.Application
        $word.Visible = $false
        $word.DisplayAlerts = 0
        $doc = $word.Documents.Open($Src, $false, $true, $false)
        switch ($Fmt) {
            'pdf' { $doc.ExportAsFixedFormat($Out, 17) }
            'docx' { $doc.SaveAs2([ref]$Out, [ref]16) }
            'doc' { $doc.SaveAs2([ref]$Out, [ref]0) }
            'txt' { $doc.SaveAs2([ref]$Out, [ref]2) }
            'rtf' { $doc.SaveAs2([ref]$Out, [ref]6) }
            default { throw "Unsupported Word target format: $Fmt" }
        }
    }
    finally {
        if ($null -ne $doc) { $doc.Close($false) | Out-Null; Release-ComObject $doc }
        if ($null -ne $word) { $word.Quit() | Out-Null; Release-ComObject $word }
        [GC]::Collect()
        [GC]::WaitForPendingFinalizers()
    }
}

function Convert-Excel([string]$Src, [string]$Out, [string]$Fmt) {
    $excel = $null
    $wb = $null
    try {
        $excel = New-Object -ComObject Excel.Application
        $excel.Visible = $false
        $excel.DisplayAlerts = $false
        $wb = $excel.Workbooks.Open($Src, $null, $true)
        switch ($Fmt) {
            'pdf' { $wb.ExportAsFixedFormat(0, $Out) }
            'xlsx' { $wb.SaveAs($Out, 51) }
            'xls' { $wb.SaveAs($Out, 56) }
            'csv' { $wb.SaveAs($Out, 6) }
            default { throw "Unsupported Excel target format: $Fmt" }
        }
    }
    finally {
        if ($null -ne $wb) { $wb.Close($false) | Out-Null; Release-ComObject $wb }
        if ($null -ne $excel) { $excel.Quit() | Out-Null; Release-ComObject $excel }
        [GC]::Collect()
        [GC]::WaitForPendingFinalizers()
    }
}

function Convert-PowerPoint([string]$Src, [string]$Out, [string]$Fmt) {
    $ppt = $null
    $pres = $null
    try {
        $ppt = New-Object -ComObject PowerPoint.Application
        $ppt.Visible = 1
        $pres = $ppt.Presentations.Open($Src, $true, $true, $false)
        switch ($Fmt) {
            'pdf' { $pres.SaveAs($Out, 32) }
            'pptx' { $pres.SaveAs($Out, 24) }
            'ppt' { $pres.SaveAs($Out, 1) }
            default { throw "Unsupported PowerPoint target format: $Fmt" }
        }
    }
    finally {
        if ($null -ne $pres) { $pres.Close() | Out-Null; Release-ComObject $pres }
        if ($null -ne $ppt) { $ppt.Quit() | Out-Null; Release-ComObject $ppt }
        [GC]::Collect()
        [GC]::WaitForPendingFinalizers()
    }
}

$source = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($SourcePath)
$outDirPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutDir)
if (-not (Test-Path -LiteralPath $source)) {
    Write-Error "Source file not found: $source"
    exit 2
}

$target = $TargetFormat.ToLower()
$baseName = [System.IO.Path]::GetFileNameWithoutExtension($source)
$outputPath = Join-Path $outDirPath ($baseName + '.' + $target)
$ext = [System.IO.Path]::GetExtension($source).ToLower()

$wordExts = @('.doc', '.docx', '.rtf', '.txt', '.odt', '.wps')
$excelExts = @('.xls', '.xlsx', '.csv', '.ods')
$pptExts = @('.ppt', '.pptx', '.odp')

try {
    if ($wordExts -contains $ext) {
        Convert-Word $source $outputPath $target
    }
    elseif ($excelExts -contains $ext) {
        Convert-Excel $source $outputPath $target
    }
    elseif ($pptExts -contains $ext) {
        Convert-PowerPoint $source $outputPath $target
    }
    else {
        throw "Unsupported source format for Microsoft Office: $ext"
    }

    if (Test-Path -LiteralPath $outputPath) {
        Write-Output $outputPath
        exit 0
    }
    Write-Error "Output file was not created"
    exit 1
}
catch {
    Write-Error $_.Exception.Message
    exit 1
}
