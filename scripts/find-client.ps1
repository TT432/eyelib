Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'devlaunch' } | ForEach-Object {
  $tail = $_.CommandLine.Substring([Math]::Max(0, $_.CommandLine.Length - 200))
  "{0}  {1}  ...{2}" -f $_.ProcessId, $_.CreationDate, $tail
}
