
FirstFragment.java     #User login and registration interface
MainActivity.java     #Main activity container, manages navigation and menu
SecondFragment.java     #Home page, displays all feature options
Pet.java     #Pet data model class
PetAdapter.java     #Pet list adapter
PetListFragment.java     #Display user pet list
AddPetFragment.java     #Add new pet interface
PetDetailFragment.java     #Pet details editing
PetParksFragment.java     #Show map, search parks, voice search
PetPark.java     #Park data model
PetAlbumFragment.java     #Display photos, upload photos, date grouping
CameraFragment.java     #Use CameraX to capture, save photos
Photo.java     #Photo data model
PhotoAdapter.java    #Display photo grid, date headers
VaccineRecordsFragment.java     #Vaccine records list with biometric
AddVaccineFragment.java     #Input vaccine data, date selection
VaccineDetailFragment.java     #Edit vaccine record details
VaccineRecord.java     #vaccine name, vaccination date, next due
VaccineRecordAdapter.java     #Display records, due status alerts
HealthRemindersFragment.java     #Display all reminders, toggle reminders
AddHealthReminderFragment.java     #Set reminder type, time, frequency
HealthReminderDetailFragment.java     #Edit reminder details
HealthReminder.java     #Reminder data model
HealthReminderAdapter.java    #Display reminders, toggle switch
LocalNotificationHelper.java    #Schedule notifications, calculate next time
NotificationHelper.java    #Create notification channel show notification
ReminderBroadcastReceiver.java     #Receive timed broadcast, trigger notification
BootReceiver.java    #Reschedule all reminder
WalkingRoutesFragment.java    #Display records, show total stats
RecordWalkingFragment.java     #Record walking activity interface
WalkingRecord.java    #Walking record data model
WalkingRecordAdapter.java    #Walking record list adapter
PetAIChatFragment.java     #AI chat bot interface, Integrate Gemini AI and pet Q&A
ChatMessage.java     #Chat message data model
ChatAdapter.java     #Differentiate user/AI message styles




Layouts:
Authentication:
fragment_first.xml              # Login/Register UI
fragment_second.xml             # Home page UI
activity_main.xml               # Main activity container
content_main.xml                # Navigation container

Pet Management:
fragment_pet_list.xml           #  Pet list
fragment_add_pet.xml            # Add pet
fragment_pet_detail.xml         # Pet details
fragment_pet_album.xml          # Pet album
fragment_camera.xml             # Camera UI
item_pet.xml                    # Pet list item

Map Features:
fragment_pet_parks.xml          # Parks map

Health Management:
fragment_vaccine_records.xml    # Vaccine records list
fragment_add_vaccine.xml        # Add vaccine
fragment_vaccine_detail.xml     # Vaccine details
item_vaccine_record.xml         # Vaccine record item
fragment_health_reminders.xml   # Health reminders list
fragment_add_health_reminder.xml # Add reminder
fragment_health_reminder_detail.xml # Reminder details
item_health_reminder.xml        # Reminder list item

Walking Records:
fragment_walking_routes.xml     # Walking records list
fragment_record_walking.xml     # Record walking
item_walking_record.xml         # Walking record item

Bot Assistant (Google Gemini):
fragment_pet_ai_chat.xml        # AI chat UI
item_chat_message.xml           # Chat message item



Reference:
https://firebase.google.com/docs/android/setup
https://developers.google.com/maps/documentation/android-sdk/
https://developer.android.com/training/camerax
https://www.youtube.com/watch?v=IrwhjDtpIU0
https://developer.android.com/training/sign-in/biometric-auth
https://ai.google.dev/tutorials/android_quickstart
