param(
    [Parameter(Mandatory = $true)][string]$TomcatHome,
    [int]$Port = 18080,
    [switch]$AuthMysql,
    [switch]$RoleMysql,
    [switch]$SubjectMysql,
    [switch]$OfferingMysql,
    [switch]$EnrollmentMysql,
    [switch]$ClassMysql,
    [switch]$CatalogMysql,
    [switch]$InteractivePasswords
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$base = Join-Path $root 'target/tomcat-smoke'
$war = Join-Path $root 'target/online-exam.war'
if (!(Test-Path $war)) { throw 'Build WAR first: .\mvnw.cmd clean verify' }
if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK 21+ directory first.' }
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
    throw "Port $Port is in use. Choose another -Port."
}
foreach ($dir in @('conf', 'logs', 'temp', 'webapps', 'work')) {
    New-Item -ItemType Directory -Force (Join-Path $base $dir) | Out-Null
}
# Copy only standard runtime config; do not copy users, realms or deployed apps.
foreach ($file in @('web.xml', 'catalina.properties')) {
    Copy-Item (Join-Path $TomcatHome "conf/$file") (Join-Path $base "conf/$file")
}
@"
<Server port="-1">
  <Service name="Catalina">
    <Connector address="127.0.0.1" port="$Port" protocol="HTTP/1.1" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="false" autoDeploy="false" />
    </Engine>
  </Service>
</Server>
"@ | Set-Content -Encoding UTF8 (Join-Path $base 'conf/server.xml')
Copy-Item $war (Join-Path $base 'webapps/online-exam.war')
$classpath = (Join-Path $TomcatHome 'bin/bootstrap.jar') + ';' + (Join-Path $TomcatHome 'bin/tomcat-juli.jar')
$javaArgs = @("-Dcatalina.home=`"$TomcatHome`"", "-Dcatalina.base=`"$base`"",
    "-Djava.io.tmpdir=`"$base/temp`"", '-cp', "`"$classpath`"", 'org.apache.catalina.startup.Bootstrap', 'start')
$process = Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin/java.exe') -ArgumentList $javaArgs `
    -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $base 'logs/stdout.log') `
    -RedirectStandardError (Join-Path $base 'logs/stderr.log')
try {
    $url = "http://127.0.0.1:$Port/online-exam"
    $ready = $false
    for ($i = 0; $i -lt 40; $i++) {
        if ($process.HasExited) { throw "Tomcat exited. See $base/logs/stderr.log" }
        try {
            $response = Invoke-WebRequest "$url/home" -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Milliseconds 500 }
    }
    if (!$ready) { throw "Tomcat did not become ready. See $base/logs/stderr.log" }
    if ($response.Content -notmatch 'Nhóm: Khang, Trung, Cường' -or
        $response.Content -notmatch 'Nền tảng JSP/Servlet đã hoạt động!') { throw 'JSP output/UTF-8 mismatch' }
    Write-Output 'PASS /home: HTTP 200, Servlet attribute rendered, UTF-8 intact'
    $welcome = Invoke-WebRequest "$url/" -UseBasicParsing
    if ($welcome.StatusCode -ne 200 -or $welcome.Content -notmatch 'Nền tảng JSP/Servlet') { throw 'Welcome mapping failed' }
    Write-Output 'PASS /: welcome mapping'
    $css = Invoke-WebRequest "$url/assets/css/app.css" -UseBasicParsing
    if ($css.StatusCode -ne 200 -or $css.Content -notmatch 'font-family') { throw 'CSS failed' }
    Write-Output 'PASS /assets/css/app.css: HTTP 200'
    $status = 0
    try { $null = Invoke-WebRequest "$url/WEB-INF/views/home.jsp" -UseBasicParsing }
    catch { if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode } else { throw } }
    if ($status -ne 404) { throw "Expected protected JSP HTTP 404; got $status" }
    Write-Output 'PASS /WEB-INF/views/home.jsp: HTTP 404'
    $login = Invoke-WebRequest "$url/login" -UseBasicParsing -SessionVariable browser
    if ($login.Content -notmatch 'name="csrf" value="[0-9a-f-]{36}"') { throw 'Login CSRF missing' }
    foreach ($path in 'admin/account','teacher/account','student/account','admin/users','admin/users/new','admin/users/edit?id=1','admin/users/lock','admin/users/unlock','admin/users/roles') {
        $guest = Invoke-WebRequest "$url/$path" -WebSession $browser -UseBasicParsing
        if ($guest.BaseResponse.ResponseUri.AbsolutePath -ne '/online-exam/login') { throw "Guest reached $path" }
    }
    foreach ($case in @(@('GET','logout',405),@('POST','logout',403),@('POST','login',403))) {
        $status = 0
        try { $null = Invoke-WebRequest "$url/$($case[1])" -Method $case[0] -UseBasicParsing -WebSession $browser }
        catch { if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode } else { throw } }
        if ($status -ne $case[2]) { throw "Unexpected status $status for $case" }
    }
    Write-Output 'PASS login JSP, guest redirects, logout method and missing CSRF'
    foreach ($path in 'admin/users/new','admin/users/edit','admin/users/lock','admin/users/unlock','admin/users/roles') {
        foreach ($body in @{},@{csrf='invalid'}) {
            $status = 0
            try { $null = Invoke-WebRequest "$url/$path" -Method Post -Body $body -WebSession $browser -UseBasicParsing }
            catch { if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode } else { throw } }
            if ($status -ne 403) { throw "Expected CSRF denial for $path" }
        }
    }
    Write-Output 'PASS create/edit guest redirects and POST missing/invalid CSRF'
    if ($RoleMysql -or $SubjectMysql -or $OfferingMysql -or $EnrollmentMysql -or $ClassMysql -or $CatalogMysql) {
        $classes = Join-Path $root 'target/role-check'
        New-Item -ItemType Directory -Force $classes | Out-Null
        $cp = "$root/target/classes;$root/target/online-exam/WEB-INF/lib/*;$classes"
        & "$env:JAVA_HOME/bin/javac.exe" -encoding UTF-8 -cp $cp -d $classes "$root/scripts/VerifyRoles.java" "$root/scripts/AcceptAuth.java" "$root/scripts/VerifySubjects.java" "$root/scripts/VerifyOfferings.java" "$root/scripts/VerifyEnrollments.java" "$root/scripts/VerifyClasses.java" "$root/scripts/VerifyQuestionCatalog.java"
        if ($LASTEXITCODE) { throw 'Role test compilation failed' }
        $suite = if ($SubjectMysql -or $OfferingMysql -or $EnrollmentMysql -or $ClassMysql -or $CatalogMysql) { 'VerifySubjects' } else { 'VerifyRoles' }
        $options = @("$Port")
        if ($CatalogMysql) { $options += 'catalog' }
        elseif ($ClassMysql) { $options += 'classes' }
        elseif ($EnrollmentMysql) { $options += 'enrollments' }
        elseif ($OfferingMysql) { $options += 'offerings' }
        & "$env:JAVA_HOME/bin/java.exe" -cp $cp $suite @options
        if ($LASTEXITCODE) { throw 'Role acceptance failed' }
    }
    if ($AuthMysql) {
        $options = @("$Port")
        if ($InteractivePasswords) { $options += 'interactive' }
        & "$env:JAVA_HOME/bin/java.exe" -cp "$root/target/classes;$root/target/online-exam/WEB-INF/lib/*" "$root/scripts/AcceptAuth.java" @options
        if ($LASTEXITCODE) { throw 'Auth acceptance failed' }
    }
} finally {
    if (!$process.HasExited) { Stop-Process -Id $process.Id; $process.WaitForExit() }
}
