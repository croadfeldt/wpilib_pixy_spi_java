Will fill more of this in later, but for now.

This is a complete eclipse project of wpilib java for the FRC 2017 season. This includes a subsystem that reads data from a Pixy camera over SPI.

Look over Vision.java in the subsystems directory for an example of how to loop through the pixy data.

Debugging to the SmartDashboard and to the console / FRC_userprogram_log is included, but turned off by default. Turning on debugging by changing the debug value in PixySPI.java. The code will drop data if you turn on logging / console debugging. It appears less data is lost using just SmartDashboard debugging. Debugging is off by default.

Values available.
Signature, center X, center Y, Width, Height for each object.
Code will return all available data for all found objects. Each loop through readPackets will return all the data currently available for all found objects of all the signatures. Filtering needs to be done in your code.

This code was derived from 

https://github.com/omwah/pixy_rpi
https://github.com/charmedlabs/pixy
and one more that I can't find at the moment, it's late, will give credit when I'm more fully coherent.

Code is of course GNU v2 licensed. License files incoming per gnu licensing guidelines.

To connect the pixy to your RoboRio, follow the pinout below.

MISO - pixy 1 - rio spi 7

SCK  -  pixy 3 - rio spi 3

MOSI - pixy 4 - rio spi 5

SS     - pixy 7 - rio spi 2

GND  - pixy 6 | 8 | 10 - rio spi 1

GND, not required but advised to use it, on the Pixy can be anyone of the pins shown, I normally use pixy 6 to keep the wires close. SS is not required, but put it in for future use. MISO, SCK and MOSI are required regardless.

Pin 1 on the Pixy is closet to the cut of corner of the silkscreen on the pcb. Pin 1 on the rio is GND. All odd pins run the long row on same side as pin 1, even numbers are the pins in the opposite row. So running in the 2x5 config, it would go, 1 and 2, then 3 and 4, then 5 and 6, then 7 and 8. In the 5x2 config it would go 1,3,5,7 then 2,4,6,8. Try to keep the cable length short, no more than 2 feet and that might be pushing it.
