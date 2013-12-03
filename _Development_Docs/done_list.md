28.11.2013
- getInfo method added on server side
- server is started immediately after application is started
- non-interactive mode, can be started without GUI
- method for getting information about one project was added - PrintProjectInfo
- switch with command identification was replaced by class ServerCommands, commands are identified by Java Reflection API    
- problem with closing application fixed, it closes java properly now
- multiple lines to output fixed - it was problem with logging system, which was fixed
- client GUI shrinking was fixed

19.11.2013
- library path made relatives

12.11.2013
- class loading system has changed. Now instead of one class, user upload jar file with classes needed for computation and classes needed for project preparation before it's uploaded and after it's downloaded


4.11.2013
- fix bug with classFormatException truncate error during automatic calculation
- fix different time padding in log

3.11.2013:
- create interface on client side: automatic recieving of task, changing it and sending to server

21.10.2013:
- text in log in GUI console are now intended evenly
- project is removed from the server if is successfully downloaded
- method downloadProject is overloaded to specify name of the downloaded file
- parameter downloadedFileName added to Downloader constructor class, instead of creating name inside class

19.10.2013:
- Class project has been commented, data structures was  changed to achieve more efficiency and code was changed to 			 	                      meet concurrency problems
- method addClient in IServerImpl was deleted as it won't be needed because of rewriting of logging for concurrency purposes
- Extraction of project after uploading was changed, class Extractor created
- Class server was split and made more clear
- classManager class was made more concurrency stable
- Logging was't concurrency stable, two users with same name could be logged at same time

=============

13.10.2013:
- handling unknown command in console
- check correct number of command parameters in console
- Time is not changing in logs 
- Make log and history windows in GUI console bigger
- Implement closing of GUI console
- Implement history in GUI console - up and down
- Add message after start command is written on server
- after disconnection it looks like the last user is still on the server
- After session is closed, remove client from active clients and remove classloader associated with this session
- In Client checker firstly ask the server which compute class is needed to proceed next task,
  so client can download the class data and set correct classloader
- Implement cache for class data in classloaders
- Implement class projectUID - project is identified by owner and name	

=============

12.10.2013:
- Problem with uploding project on client solved - progress of uploading wasn't seen
- On client, thread management was changed to use Exetutors and Callable<T>
- Add path validity testing in path which are written to consoles
- Implement GUI console, which will be devided into 3 parts, input, input history and logging window

=============

29.09.2013:
- automatic creation of upload, projects, server_basedir folders
- repair and implement properties storing on clint and server
- initialize some variables from properties file at start of client or server
- change texts on client and server side
- Class Logger was rewrited, logging to file implemented
- Datum and time is now part of the log messages

=============
	
Issues solved before draft handling in:        

- when is tasks unfinished for some reason , it is needed to place it back into list uncompletedTask in project,
  implement timeout


- ClassLoader problems fixed:
	- there is no need to have special folder where all compute classes are stored
	- no extra folder has to be added into classpath before starting program
	- no extra rules about the name of compute class, f.e like format before ClientID_ProjectID_Task.class
    
