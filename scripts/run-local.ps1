param([int]$Port = 18080)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$root = $PWD.Path
if (!$env:JAVA_HOME) {
    $props = & cmd.exe /d /c 'java -XshowSettings:properties -version 2>&1'
    $env:JAVA_HOME = ($props | Select-String '^\s*java\.home = (.+)$').Matches.Groups[1].Value
}
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
$tomcat = $env:CATALINA_HOME
if (!$tomcat) { $tomcat = Join-Path $env:ProgramFiles 'Apache Software Foundation/Tomcat 10.1' }
if (!(Test-Path "$tomcat/bin/bootstrap.jar")) { throw 'Set CATALINA_HOME to a readable Tomcat 10.1 installation.' }
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) { throw "Port $Port is already in use." }
$env:ONLINE_EXAM_ENV_FILE = (Resolve-Path '.env').Path
& .\mvnw.cmd package
if ($LASTEXITCODE) { exit $LASTEXITCODE }
$base = Join-Path $root '.local/tomcat-vscode'
foreach ($dir in 'conf','logs','temp','work','webapps') {
    New-Item -ItemType Directory -Force (Join-Path $base $dir) | Out-Null
}
foreach ($file in 'web.xml','catalina.properties') { Copy-Item "$tomcat/conf/$file" "$base/conf/$file" -Force }
@"
<Server port="-1"><Service name="Catalina">
  <Connector address="127.0.0.1" port="$Port" protocol="HTTP/1.1"/>
  <Engine name="Catalina" defaultHost="localhost">
    <Host name="localhost" appBase="webapps" unpackWARs="false" autoDeploy="false"/>
  </Engine>
</Service></Server>
"@ | Set-Content -Encoding UTF8 "$base/conf/server.xml"
Copy-Item 'target/online-exam.war' "$base/webapps/online-exam.war" -Force
Write-Host "After Tomcat startup: http://127.0.0.1:$Port/online-exam/home (Ctrl+C to stop)"
& $java "-Dcatalina.home=$tomcat" "-Dcatalina.base=$base" "-Djava.io.tmpdir=$base/temp" -cp "$tomcat/bin/bootstrap.jar;$tomcat/bin/tomcat-juli.jar" org.apache.catalina.startup.Bootstrap start
exit $LASTEXITCODE
