/*
===========================================================================
=
Name : testcan.c
Authors : Muhammad Hussain, Asher Ali
Version :
Description :

===========================================================================
*/
#include <sched.h>
#include <errno.h>
#include <stdarg.h>
#include <syslog.h>
#include <sys/epoll.h>
#include <net/if.h>
#include <linux/reboot.h>
#include <sys/reboot.h>


#include "CANopen.h"
#include "OD.h"
#include "301/CO_driver.h"
#include "309/CO_gateway_ascii.h"
#include "CO_error.h"
#include "CO_epoll_interface.h"
#include "CO_storageLinux.h"

#include "can_sensor.h"
#include "res_sensor.h"
#include "uimodule.h"

/* Interval of main-line and real-time thread in microseconds */
#ifndef MAIN_THREAD_INTERVAL_US
#define MAIN_THREAD_INTERVAL_US 100000
#endif

#ifndef TMR_THREAD_INTERVAL_US
#define TMR_THREAD_INTERVAL_US 1000
#endif

/* default values for CO_CANopenInit() */
#ifndef NMT_CONTROL
#define NMT_CONTROL \
            CO_NMT_STARTUP_TO_OPERATIONAL \
          | CO_NMT_ERR_ON_ERR_REG \
          | CO_ERR_REG_GENERIC_ERR \
          | CO_ERR_REG_COMMUNICATION
#endif

#ifndef FIRST_HB_TIME
#define FIRST_HB_TIME 500
#endif

#ifndef SDO_SRV_TIMEOUT_TIME
#define SDO_SRV_TIMEOUT_TIME 1000
#endif

#ifndef SDO_CLI_TIMEOUT_TIME
#define SDO_CLI_TIMEOUT_TIME 500
#endif

#ifndef SDO_CLI_BLOCK
#define SDO_CLI_BLOCK false
#endif

#ifndef OD_STATUS_BITS
#define OD_STATUS_BITS NULL
#endif

/* CANopen gateway enable switch for CO_epoll_processMain() */
#ifndef GATEWAY_ENABLE
#define GATEWAY_ENABLE true
#endif
/* Interval for time stamp message in milliseconds */
#ifndef TIME_STAMP_INTERVAL_MS
#define TIME_STAMP_INTERVAL_MS 10000
#endif

/* Definitions for application specific data storage objects */
#ifndef CO_STORAGE_APPLICATION
#define CO_STORAGE_APPLICATION
#endif
/* Interval for automatic data storage in microseconds */
#ifndef CO_STORAGE_AUTO_INTERVAL
#define CO_STORAGE_AUTO_INTERVAL 60000000
#endif

/* CANopen object */
CO_t *CO = NULL;
uint16_t pendingBitRate = 500;
// Pending CANopen NodeId, can be set by argument or LSS slave.
uint8_t pendingNodeId = 1;
static uint8_t CO_activeNodeId;

/*real-time thread handler*/
static void* rt_thread(void* arg);

/*Real-time thread ID*/
pthread_t rt_thread_id;

/*Separate EPOLL objects for main-line, real-time and gateway functions*/
CO_epoll_t epMain, epRT;
CO_epoll_gtw_t epGtw;
CO_ReturnError_t err;
CO_NMT_reset_cmd_t reset = CO_RESET_NOT;


const char *CANbus = "can0";
long int CANopen_timestamp;
unsigned int CANopen_RPDO_val;
unsigned int end_program = 0;

/*Enabling interaction capability through console/terminal*/
int32_t commandInterface = CO_COMMAND_IF_STDIO;
//int32_t commandInterface = CO_COMMAND_IF_DISABLED;

//CANOpen Object Pointer to be referenced for every protocol and CAN bus
CO_CANptrSocketCan_t CANptr = {0};
/*Disabling device control over socket*/
char *localSocketPath = NULL;
uint32_t socketTimeout_ms = 0;

/*Flags to control the flow of the application*/
unsigned int PDOFlag = 0;
unsigned lastRun = 0;
/*---------------------------------------*/
/* Message logging function */
void log_printf(int priority, const char *format, ...) {
 va_list ap;
 va_start(ap, format);
 vsyslog(priority, format, ap);
 va_end(ap);
#if (CO_CONFIG_GTW) & CO_CONFIG_GTW_ASCII_LOG
    if (CO != NULL) {
        char buf[200];
        time_t timer;
        struct tm* tm_info;
        size_t len;

        timer = time(NULL);
        tm_info = localtime(&timer);
        len = strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S: ", tm_info);

        va_start(ap, format);
        vsnprintf(buf + len, sizeof(buf) - len - 2, format, ap);
        va_end(ap);
        strcat(buf, "\r\n");
        CO_GTWA_log_print(CO->gtwa, buf);
    }
#endif
}

#if (CO_CONFIG_EM) & CO_CONFIG_EM_CONSUMER
/* Callback for Emergency Messages */
static void EmergencyRxCallback(const uint16_t ident,
 const uint16_t errorCode,
 const uint8_t errorRegister,
 const uint8_t errorBit,
 const uint32_t infoCode)
{
 int16_t nodeIdRx = ident ? (ident&0x7F) : CO_activeNodeId;
 log_printf(LOG_NOTICE, DBG_EMERGENCY_RX, nodeIdRx, errorCode,
 errorRegister, errorBit, infoCode);
}
#endif

#if ((CO_CONFIG_NMT) & CO_CONFIG_NMT_CALLBACK_CHANGE) \
|| ((CO_CONFIG_HB_CONS) & CO_CONFIG_HB_CONS_CALLBACK_CHANGE)
/* Return string description of NMT state. */
static char *NmtState2Str(CO_NMT_internalState_t state)
{
 switch(state) {
 case CO_NMT_INITIALIZING: return "initializing";
 case CO_NMT_PRE_OPERATIONAL: return "pre-operational";
 case CO_NMT_OPERATIONAL: return "operational";
 case CO_NMT_STOPPED: return "stopped";
 default: return "unknown";
 }
}
#endif

#if (CO_CONFIG_NMT) & CO_CONFIG_NMT_CALLBACK_CHANGE
/* Callback for NMT change messages */
static void NmtChangedCallback(CO_NMT_internalState_t state)
{
 log_printf(LOG_NOTICE, DBG_NMT_CHANGE, NmtState2Str(state), state);
}
#endif


#if (CO_CONFIG_HB_CONS) & CO_CONFIG_HB_CONS_CALLBACK_CHANGE
/* Callback for monitoring Heart-beat remote NMT state change */
static void HeartbeatNmtChangedCallback(uint8_t nodeId, uint8_t idx,
 CO_NMT_internalState_t state,
void *object)
{
 (void)object;
 log_printf(LOG_NOTICE, DBG_HB_CONS_NMT_CHANGE,
 nodeId, idx, NmtState2Str(state), state);
}

#endif


/**************************************************************************
*****
* Main-line thread
***************************************************************************
***/
void* CANOpen_thread() {

 bool_t firstRun = true;
 reset = CO_RESET_NOT;

 /* To extract CAN bus index from linux system */
CANptr.can_ifindex = if_nametoindex(CANbus);
	if (CANptr.can_ifindex == 0) {
	printf("CAN Interface %s inactive in linux system\n\r", CANbus);
	reset = CO_RESET_QUIT;
		}

 /* Allocate memory for CANopen objects */
 uint32_t heapMemoryUsed = 0;
 CO_config_t *config_ptr = NULL;

 CO = CO_new(config_ptr, &heapMemoryUsed);
 //CO = CO_new(NULL,0);
  if (CO == NULL) { //if(CO == !CO_ERROR_NO){
     	printf("Can memory allocation failed.\n\r");
     	reset = CO_RESET_QUIT;
  }

 /* Epoll function creation */
 err = CO_epoll_create(&epMain, MAIN_THREAD_INTERVAL_US);
 if(err != CO_ERROR_NO) {
 printf("Error: CO_epoll_create(main) failed\n\r");
 reset = CO_RESET_QUIT;
 	 }

 err = CO_epoll_create(&epRT, TMR_THREAD_INTERVAL_US);
 if(err != CO_ERROR_NO) {
 printf("Error: CO_epoll_create(RT) failed\n\r");
 reset = CO_RESET_QUIT;
 	 }
 // Assigning real-time epoll file descriptor as the default file
CANptr.epoll_fd = epRT.epoll_fd;

#if (CO_CONFIG_GTW) & CO_CONFIG_GTW_ASCII

    err = CO_epoll_createGtw(&epGtw, epMain.epoll_fd, commandInterface,
                              socketTimeout_ms, localSocketPath);
    if(err != CO_ERROR_NO) {
    	 printf("Error: CO_epoll_createGtw failed\n\r");
    	 reset = CO_RESET_QUIT;
    }

#endif

 /* Initialization loop
 * First run and during CANopen Device Communication Reset
 * check for any failure in Initialization*/
 while(reset != CO_RESET_APP && reset != CO_RESET_QUIT && end_program == 0) {
 uint32_t errInfo;
 reset = CO_RESET_NOT;


/* Wait rt_thread. */
 if(!firstRun) {
     CO_LOCK_OD(CO->CANmodule);
     CO->CANmodule->CANnormal = false;
     CO_UNLOCK_OD(CO->CANmodule);
 }


/* Enter CAN configuration. */
	CO_CANsetConfigurationMode((void *)&CANptr);
	CO_CANmodule_disable(CO->CANmodule);

  
 /* Initialize CAN Module, LSS and CANOpen_Node */
 err = CO_CANinit(CO, (void *)&CANptr, 0 /* bit rate not used */);
 if (err != CO_ERROR_NO) {
 printf("Error: CAN Module initialization failed\n\r");
 reset = CO_RESET_QUIT;

}
 CO_LSS_address_t lssAddress = {.identity = {
             .vendorID = OD_PERSIST_COMM.x1018_identity.vendor_ID,
             .productCode = OD_PERSIST_COMM.x1018_identity.productCode,
             .revisionNumber = OD_PERSIST_COMM.x1018_identity.revisionNumber,
             .serialNumber = OD_PERSIST_COMM.x1018_identity.serialNumber
         }};


 err = CO_LSSinit(CO, &lssAddress,
                  &pendingNodeId, &pendingBitRate);
 if(err != CO_ERROR_NO) {
     log_printf(LOG_CRIT, DBG_CAN_OPEN, "CO_LSSinit()", err);
     printf("LSS Initialization failed.\n\r");
     reset = CO_RESET_QUIT;
 }

 CO_activeNodeId = pendingNodeId;
 errInfo = 0;

 err = CO_CANopenInit(CO,                // CANopen object
                      NULL,              // alternate NMT
                      NULL,              // alternate em
                      OD,                // Object dictionary
                      OD_STATUS_BITS,    // Optional OD_statusBits
                      NMT_CONTROL,       // CO_NMT_control_t
                      FIRST_HB_TIME,     // firstHBTime_ms
                      SDO_SRV_TIMEOUT_TIME, // SDOserverTimeoutTime_ms
                      SDO_CLI_TIMEOUT_TIME, // SDOclientTimeoutTime_ms
                      SDO_CLI_BLOCK,     // SDOclientBlockTransfer
                      CO_activeNodeId,
                      &errInfo);
 if(err != CO_ERROR_NO && err != CO_ERROR_NODE_ID_UNCONFIGURED_LSS) {
     if (err == CO_ERROR_OD_PARAMETERS) {
         log_printf(LOG_CRIT, DBG_OD_ENTRY, errInfo);
     }
     else {
         log_printf(LOG_CRIT, DBG_CAN_OPEN, "CO_CANopenInit()", err);
     }
     reset = CO_RESET_QUIT;
 }


 /* initialize part of threadMain and callbacks */
 CO_epoll_initCANopenMain(&epMain, CO);
// -------------------------------------------------
#if (CO_CONFIG_GTW) & CO_CONFIG_GTW_ASCII
        CO_epoll_initCANopenGtw(&epGtw, CO);
#endif


if(!CO->nodeIdUnconfigured) {
	if(errInfo != 0) {
		CO_errorReport(CO->em, CO_EM_INCONSISTENT_OBJECT_DICT,
					   CO_EMC_DATA_SET, errInfo);
	}

#if (CO_CONFIG_EM) & CO_CONFIG_EM_CONSUMER
            CO_EM_initCallbackRx(CO->em, EmergencyRxCallback);
#endif
#if (CO_CONFIG_NMT) & CO_CONFIG_NMT_CALLBACK_CHANGE
            CO_NMT_initCallbackChanged(CO->NMT, NmtChangedCallback);
#endif
#if (CO_CONFIG_HB_CONS) & CO_CONFIG_HB_CONS_CALLBACK_CHANGE
          CO_HBconsumer_initCallbackNmtChanged(CO->HBcons, 0, NULL,
                                                HeartbeatNmtChangedCallback);

#endif

        log_printf(LOG_INFO, DBG_CAN_OPEN_INFO, CO_activeNodeId, "communication reset");
        }

        else {

            log_printf(LOG_INFO, DBG_CAN_OPEN_INFO, CO_activeNodeId, "node-id not initialized");
        	printf("CANopen device node-id: %d Initialization failed.\n\r",CO_activeNodeId);
            reset = CO_RESET_QUIT;
        }

        /* First time only initialization. */
        if(firstRun) {
            firstRun = false;

            if(pthread_create(&rt_thread_id, NULL, rt_thread, NULL) != 0) {
            		                log_printf(LOG_CRIT, DBG_ERRNO, "pthread_create(rt_thread)");
            		                printf("Real time thread creation failed.\n\r");
            		                reset = CO_RESET_QUIT;
            		            }

        } /* if(firstRun) */

		        errInfo = 0;
		        err = CO_CANopenInitPDO(CO,             /* CANopen object */
		                                CO->em,         /* emergency object */
		                                OD,             /* Object dictionary */
		                                CO_activeNodeId,
		                                &errInfo);

		        if(err != CO_ERROR_NO ) {
		        	printf("PDO initialization failed.\n\r");

		            reset = CO_RESET_QUIT;
		        }

		        /* start CAN */
		        CO_CANsetNormalMode(CO->CANmodule);
		        reset = CO_RESET_NOT;
		        printf("Info: CAN is running \n\r");


 while(reset == CO_RESET_NOT && end_program == 0) {
 /* loop for normal program execution ******************************************/

 	if(lastRun == 1)
 	{
 		end_program = 1;
 		break;
 	}

     CO_epoll_wait(&epMain);
     CO_epoll_processGtw(&epGtw, CO, &epMain);
     CO_epoll_processMain(&epMain, CO, GATEWAY_ENABLE, &reset);
     CO_epoll_processLast(&epMain);
 }

 }

    	/* program exit , join threads */


	if (pthread_join(rt_thread_id, NULL) != 0) {
		printf("RT thread joining failed.\n\r");
	}

		/* delete epoll objects */
		CO_epoll_close(&epRT);
		CO_epoll_close(&epMain);
		CO_epoll_closeGtw(&epGtw);
		/* delete CAN object memory */
		CO_CANsetConfigurationMode((void *)&CANptr);
		//CO_delete((void *)&CANptr);
		CO_delete(CO);
		printf("Info: CANopen thread dead\n\r");
	return NULL;
}


/*******************************************************************************
 * Real time thread for CAN receive and threadTmr
 ******************************************************************************/
static void* rt_thread(void* arg) {
    (void)arg;
    while(reset == CO_RESET_NOT && end_program == 0) {

        CO_epoll_wait(&epRT);
        CO_epoll_processRT(&epRT, CO, true);
        CO_epoll_processLast(&epRT);

        if(CO->RPDO->CANrxNew[0]) {
        /* Set flag every time new PDO is received at the buffer */
        	printf("PDO flag is set \n");

        CANopen_timestamp =
		(CO->CANrx->timestamp.tv_nsec/1000) +
		(CO->CANrx->timestamp.tv_sec*1000000);
        //(CO->CANmodule->rxArray[4].timestamp.tv_nsec/1000) +
        //(CO->CANmodule->rxArray[4].timestamp.tv_sec*1000000);

        CANopen_RPDO_val = (int)((CO->RPDO->CANrxData[0][1] << 8) | CO->RPDO->CANrxData[0][0]);
        PDOFlag = 1;
       }
    }

    return NULL;
}
