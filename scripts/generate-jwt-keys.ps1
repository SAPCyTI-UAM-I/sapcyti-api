# Generates dev RSA PEM files under src/main/resources/jwt/
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root
.\mvnw.cmd -q test-compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
    "-Dexec.mainClass=mx.uam.sapcyti.identity.support.JwtKeyGenerator" `
    "-Dexec.classpathScope=test"
Write-Host "JWT dev keys generated in src/main/resources/jwt/"
