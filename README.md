# Object_detection_and_recognition
Project is an application that 
1. captures video flow from video camera (web camera),
2. recognizes unique objects (up to 40 types, see in [coco.names]()) and draws a frame around it,
3. detects them move into red rectangle on screen (control frame)
4. forms a PDF report of results (number of unique objects of each type that crossed the control frame).

The program is still to be mastered and perfected for differend purposes when object detection is needed. 

Actual way to run the applicaion:
- From IntellijIdea: with  VM arguments of run configuration
- From cmd: run -jar file from project root with VM arguments (e. g.  java [VM arguments] -jar target/[jar-file].jar)
  
VM arguments: -Djava.library.path=lib\x64 --module-path lib/javafx-sdk-23.0.1/lib --add-modules javafx.controls,javafx.fxml
