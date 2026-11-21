param([string]$Edge = "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe", [switch]$Attempts)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$layout = Join-Path $root 'target/exam-layout'
$pages = @('list','draft','schedule','assignments')
if ($Attempts) { $pages = 'attempt-start','attempt-take','attempt-confirm','attempt-history' }
foreach ($name in $pages) {
    if (!(Test-Path -LiteralPath (Join-Path $layout "$name.html"))) { throw 'Run the matching ExamMysql or AttemptMysql suite first.' }
}
$probe = @'
<!doctype html><meta charset="utf-8"><body><script>
for (const width of [390,1280]) {
  for (const page of PAGES_TO_CHECK) {
    const frame = document.createElement("iframe");
    frame.style.width = width + "px";
    frame.style.height = "3600px";
    frame.src = page + ".html";
    frame.onload = () => {
      const dom = frame.contentDocument.documentElement;
      const output = document.createElement("output");
      output.textContent = page + " " + width + " " +
        (dom.clientWidth === width && dom.scrollWidth <= width ? "PASS" : "FAIL");
      document.body.append(output);
    };
    document.body.append(frame);
  }
}
</script>
'@
$file = Join-Path $layout 'measure.html'
[IO.File]::WriteAllText($file,$probe.Replace('PAGES_TO_CHECK', (ConvertTo-Json -Compress -InputObject $pages)))
$profile = Join-Path $root ('target/edge-exams-' + [guid]::NewGuid().ToString('N'))
# Use only the four sanitized pages captured from the isolated fixture.
$output = & $Edge --headless --disable-gpu --disable-extensions --no-first-run --allow-file-access-from-files "--user-data-dir=$profile" --virtual-time-budget=5000 --dump-dom ([uri]$file).AbsoluteUri 2>$null | Out-String
$results = [regex]::Matches($output,'<output>([^<]+)</output>')
$results | ForEach-Object { $_.Groups[1].Value }
if ($results.Count -ne 2*$pages.Count -or $output.Contains('FAIL</output>')) { throw 'Incomplete or overflowing exam layout' }
