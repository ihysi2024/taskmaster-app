*****FINAL PROJECT PROTOTYPE*****
This program is a productivity app that allows users to track
their upcoming tasks and productivity levels to encourage user 
efficiency in their daily life. 

***PROGRAM SPECIFICATIONS***
The following program uses Firebase email/password
authentication to allow users to register an account with the 
system. The program will also connect to the Calendar Provider
API in the Android OS to allow users to add events to their 
calendar and filter events by those occurring in the week 
selected.

The program was tested on Nexus 5X API 34 virtual device by running 
FinalProjectPrototype.app.main in the run configurations. 


***PROGRAM WORKFLOW***
Users can register an account with the system using a unique email and 
password combination that is stored in the Firestore Firebase and can then
login to the system with that combination.

After logging in, the user has access to their existing list of tasks that
is unique to their email account in the "Tasks" tab. A user can add a task 
by pressing the Floating Action Button in the bottom right hand corner,
where they can provide the task name, description, due date, and task level
of urgency from 1 to 5. Tasks in the list will be colored purple if the
assignment is overdue. Swiping left on the task will allow users to edit the task, 
and clicking the check box will remove the task from the list and mark
the task as completed. 

The calendar tab allows the events to visualized in the calendar and
selecting a day will display all the tasks that must be completed in that
week. 

The tracker tab will track the number of tasks left in the week to be
completed. Depending on the percent of tasks that the user has completed, 
the user will be told they are achieving peak productivity, 
medium productivity, or low productivity, and there will be an image
displayed to reaffirm this productivity level. 


****CHECKLIST****

DONE
- firebase email/password authentication
- user-specific tasks are stored in the firebase when a user adds a task
- tasks are displayed in RecyclerView in chronological order
- tasks that are overdue are filled in purple in the tasks tab
- calendar is connected and recycler view tab only displays
events that occur in the week selected

TO DO 
- task on click handlers (deleting and editing)
- adding an event to a calendar 
- tracking number of tasks completed and left to complete
- associating number of tasks completed with an imageview
- sorting tasks occurring on the same day by urgency
- push notifications when a task is due on the day that the user logs in