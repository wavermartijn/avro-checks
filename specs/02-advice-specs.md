### Feature Advice
* When a incompatible change is detected, give suggestions how to migrate to the requested change in steps with a clear advice on how to approach. For example when a new mandatory field is advices with the BACKWARD compatibility mode, do suggestion how this can be done.
* Add tests for this
* Initialize a git repository when not done yet
* Generate a clear git commit for every feature delivered with a clear message added to the commit. All tests should run successful before committing.
* Add a VERSION file and use that in the CLI. Update the version for each commit. Start with 0.0.1-RC1 and cound up the RC version for now
* When the --help option is given, generate a help menu in the cli and also add info about the github repo and the OWNERS. For now that is me, Martijn van der Pauw
* Put the text for the help menu in a template in the src/main/resources folder and parse that
* Create a demo.bat and a demo.sh script to run the demo but document only the demo.sh script
* Create gradlew.bat and gradlew scripts to run the demo but document only the gradlew script
* Add a CODEOWNERS file to document the owners of the code
