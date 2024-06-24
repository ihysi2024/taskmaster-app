FINAL PROJECT PROTOTYPE This program is a productivity app that allows users to track their upcoming tasks and productivity levels to encourage user efficiency in their daily life.

PROGRAM SPECIFICATIONS The following program uses Firebase email/password authentication to allow users to register an account with the system. The program will also connect to the Calendar Provider API in the Android OS to allow users to add events to their calendar and filter events by those occurring in the week selected.

The program was tested on Nexus 5X API 34 virtual device by running FinalProjectPrototype.app.main in the run configurations.

PROGRAM WORKFLOW Users can register an account with the system using a unique email and password combination that is stored in the Firestore Firebase and can then login to the system with that combination.

The background services includes notifying the user of the tasks on their list due the day they logged into the program.

After logging in, the user has access to their existing list of tasks that is unique to their email account in the "Tasks" tab. A user can add a task by pressing the Floating Action Button in the bottom right hand corner, where they can provide the task name, description, due date, and task level of urgency from 1 to 5. Tasks in the list will be colored purple if the assignment is overdue. Selecting the task will allow users to edit the task, and clicking the check box will remove the task from the list and mark the task as completed.

The calendar tab allows the events to visualized in the calendar and selecting a day will display all the tasks that must be completed in that week. Selecting a task in the list will allow the user to share the contents of the task with others.

The tracker tab will track the number of tasks left in the week to be completed. Depending on the percent of tasks that the user has completed, the user will be told they are achieving peak productivity, medium productivity, or low productivity, and there will be an image displayed to reaffirm this productivity level. The productivity tracker is based on the number of completed tasks over the total tasks created for the week. When a user has completed all available tasks, this tally is reset. The following demonstrates the productivity markers and what percent of total tasks completed result in various productivity levels.

- greater than 75%: "Peak Productivity", check mark
- between 25% and 75%: "Good Productivity", pending progress
- less than 25%: "Low Productivity", "X" mark
