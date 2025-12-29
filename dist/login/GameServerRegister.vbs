'Get Java path.
Dim path
Set shell = WScript.CreateObject("WScript.Shell")
path = "C:\Program Files\Java\jdk-25\bin\"
path = Replace(path, "\\", "\")
path = Replace(path, "Program Files", "Progra~1")

'Load GUI configuration.
window = 1
Set file = CreateObject("Scripting.FileSystemObject").OpenTextFile("config\Interface.ini", 1)
Do While Not file.AtEndOfStream
	If Replace(LCase(file.ReadLine()), " ", "") = "enablegui=true" Then
		window = 0
		Exit Do
	End If
Loop
file.Close
Set file = Nothing
'Generate command.
command = path & "java " & parameters & " -cp ./../libs/* org.l2jmobius.tools.GameServerRegister"
If window = 1 Then
	command = "cmd /c start ""L2J Mobius - GameServer Register"" " & command
End If
'Run the GameServer Register.
exitcode = shell.Run(command, window, True)