/*
===========================================================================
=
Name : testcan.c
Authors : Muhammad Hussain
Version :
Description :

===========================================================================
*/

#ifndef can_sensor_H_
#define can_sensor_H_


extern unsigned int terminate;
extern unsigned int sensID;
extern unsigned int lastRun;
void* CANOpen_thread();

#endif
