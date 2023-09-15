# start wsa client in background
Start-Process -FilePath "WsaClient" -ArgumentList "/launch wsa://system" -WindowStyle Hidden

# Wait a little
Start-Sleep -Seconds 1

# start adb server for usage in Android Studio
C:\adb\adb connect localhost:58526