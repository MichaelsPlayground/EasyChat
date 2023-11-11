# Prohibited file types on Firebase Cloud Storage

see: https://firebase.google.com/support/faq#storage-exe-restrictions

**Cloud Storage for Firebase: On Spark plan projects, can I store executable files?**

*For no-cost (Spark) plan projects, Firebase blocks uploads and hosting of certain executable file types for 
Windows, Android and Apple by Cloud Storage for Firebase and Firebase Hosting. 
This policy exists to prevent abuse on our platform.

Serving, hosting and file uploads of disallowed files are blocked for all Spark projects created on or 
after Sept 28th, 2023. For existing Spark projects with files uploaded before that date, such files 
can still be uploaded and hosted.

This restriction applies to Spark plan projects. Projects on the pay as you go (Blaze) plan are not affected.

The following file types cannot be hosted on Firebase Hosting and Cloud Storage for Firebase:

Windows files with .exe, .dll and .bat extensions
Android files with .apk extension
Apple platform files with .ipa extension
What do I need to do?

If you still want to host these file types after September 28th, 2023:

For Hosting: upgrade to the Blaze plan before you can deploy these file types to Firebase Hosting via 
the firebase deploy command.
For Storage: upgrade to the Blaze plan to upload these file types to the bucket of your choice using 
the GCS CLI, the Firebase console, or Google Cloud Console.
Use Firebase tools to manage your Firebase Hosting and Cloud Storage resources.

For managing resources in Firebase Hosting, use the Firebase console to delete releases according to this guide.
For managing resources in Cloud Storage, navigate to the Storage product page in your project.
On the Files tab, locate disallowed files to delete in your folder hierarchy, then select them using the 
checkbox next to the filename(s) on the left-hand side of the panel.
Click Delete, and confirm the files were deleted.
Please refer to our documentation for additional information on managing Hosting resources with Firebase 
tools and Cloud Storage for Firebase buckets with client libraries.*
