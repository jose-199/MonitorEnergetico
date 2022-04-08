from posixpath import split
import paho.mqtt.client as mqtt
import logging
import time
import os 
import threading
import sys  
from datetime import datetime
from dateutil.relativedelta import relativedelta
from datetime import date
import pytz 
from broker_data import * 
from sql_comands import *

LOGFILE = 'server.log'

sql = db_manage()    #This object hand new users and logins  

#Configuracion inicial de logging
logging.basicConfig(
    level = logging.INFO, 
    format = '[%(levelname)s] (%(threadName)-10s) %(message)s'
    )


#verify reset date for all devices, delete old
#registers (registers of one year ago) and send mqtt msgs
def mng_data():
    sql_check = db_manage()    #This object make request to database
    old_date = datetime.now().date()
    while True:
        today = datetime.now()

        #send a mqtt message every 10 minutes to all devices in order 
        #to save their current consumption
        if(today.minute%10==8 and today.second==0):
            client.publish("energy/devices", "save-consum", 0, False)
        
        #check if devices need reset every hour
        if(today.minute==0 and today.second==1):
            dates = sql_check.get_next_reset()   #get all next reset dates add devices
            for i in dates:
                if (i[1]!=None):           #check if date reset is empty 
                    #get current date from time zone 
                    try :
                        local_today = datetime.now(pytz.timezone(i[2])) 
                        if (local_today.date()>i[1]):       
                            #compares current date wiht next reset date
                            next_date=(str(i[1].year)+"-"+
                                       str(i[1].month+1)+"-"+
                                       str(i[1].day))
                            #set a new next reset date
                            resp = sql.set_next_reset(i[0],next_date)  
                            client.publish("energy/devices/"+i[0], "reset", 2, False)    
                            log = (str(datetime.now())+" Update next date reset of " + 
                                    i[0] + " return: " + resp)
                            logging.info(log)
                            save_log(log)
                    except:
                        log = str(datetime.now())+" Invalid zone time for " + i[0]
                        logging.info(log)
                        save_log(log)

        if (today.date()>old_date and today.second==2):
            old_date = today.date()
            delete_date = today - relativedelta(years=1)
            sql_check.delete_old_registers(delete_date)

        time.sleep(1)

#Callback que se ejecuta cuando nos conectamos al broker
def on_connect(client, userdata, flags, rc):
    log = str(datetime.now())+" Running..."
    logging.info(log)
    save_log(log)

#Callback que se ejecuta cuando llega un mensaje al topic suscrito
def on_message(client, userdata, msg):    
    inf = msg.payload.decode().replace("'","").split('\n')

    if (str(msg.topic).split("/")[1]=="devices"):
        if (inf[0]=="req"): 
            device = inf[1]
            #ask if device need energy reset
            if (sql.need_reset(device)):
                client.publish("energy/devices/"+device, "reset", 2, False)
                log = (str(datetime.now())+" insert data of " + inf[1] + 
                " return: "+"device need reset and data wasn't added")
                logging.info(log)
                save_log(log)
            else :
                name = inf[2]
                rate = inf[3]
                currency = inf[4]
                payday = inf[5]
                timezone = inf[6]
                volt = inf[7]
                current = inf[8]
                power = inf[9]
                energy = inf[10]
                charge = inf[11]
                reg_date = datetime.now(pytz.timezone(timezone)).date()
                reg_time = datetime.now(pytz.timezone(timezone)).time()
                #insert a new measurement
                resp = sql.insert_data(device,name,rate,currency,payday,volt,current,
                                    power,energy,charge,reg_date,reg_time,timezone)    
                log = str(datetime.now())+" insert data of " + inf[1] + " return: " + resp  
                logging.info(log)
                save_log(log)
        if(inf[0]=="ok-reset"):    
            #measure device indicates its energy counter was reestarted
            sql.reg_reset(str(msg.topic).split("/")[2],0)  
            #regist new reset energy counter
        if(inf[0]=="req-data" or inf[0]=="req-data-day"):     
            #measure device ask for energy consumption saved
            device = str(msg.topic).split("/")[2]
            name = inf[1]
            date_ini = date(int(inf[2].split("-")[0]),
                             int(inf[2].split("-")[1]),
                             int(inf[2].split("-")[2]))
            date_final = None
            if inf[0]=="req-data":
                date_final = date(int(inf[3].split("-")[0]),
                             int(inf[3].split("-")[1]),
                             int(inf[3].split("-")[2]))

            resp = sql.histroy(device,name,date_ini,date_final)
            if date_final==None:
                    date_conf=str(date_ini)
            else:    
                date_conf=str(date_ini)+"-"+str(date_final)
            if resp != ():
                cons = (str(resp[0])+" kWh"+'\n'+str(resp[1][0][0])+resp[1][0][1]
                        +str(resp[1][1][0])+resp[1][1][1])
                client.publish("energy/devices/"+device, "res-data"+'\n'+date_conf
                +'\n'+cons, 2, False)  
            else:
                client.publish("energy/devices/"+device, "res-data"+'\n'+date_conf
                +"\n"+"0"+'\n'+"0", 2, False)
            log = str(datetime.now())+" "+device+" ask for energy consumption saved"
            logging.info(log)
            save_log(log)

    if (str(msg.topic)=="energy/update"):
        if (inf[0]=="req"):
            resp = sql.update(inf[1],inf[2],inf[3],inf[4],inf[5],inf[6])
            log = str(datetime.now())+" Update data of " + inf[1] + " return: " + resp
            if resp=="ok":
                client.publish("energy/devices/"+inf[1], "ok-update", 2, False)
            logging.info(log)
            save_log(log)

    if (str(msg.topic)=="energy/control/add_device"):
        resp = sql.add_device(inf[0])
        if (resp=="ok"):
            client.subscribe(("energy/"+inf[0], 0))
        log = str(datetime.now())+ "add device " + inf[0] + " return: " + resp
        logging.info(log)
        save_log(log)

    if (str(msg.topic)=="energy/control/delete_device"):
        resp = sql.delete_device(inf[0])
        if (resp=="ok"):
            client.unsubscribe("energy/"+inf[0])
        log = str(datetime.now())+ "delete device " + inf[0] + " return: " + resp
        logging.info(log)
        save_log(log)
    
def save_log(data):
    logCommand = 'echo "' + data + '" >> ' + LOGFILE
    os.system(logCommand)

#mqtt configuration
client = mqtt.Client(clean_session=True) 
client.on_connect = on_connect 
client.on_message = on_message 
client.username_pw_set(cred_mqtt[2], cred_mqtt[3]) 
client.connect(host=cred_mqtt[0], port = int(cred_mqtt[1])) 
cred_mqtt=""
qos = 0   #qos MQTT

#Subscripcion to MQTT topics
client.subscribe([("energy/update", qos),("energy/control/add_device", qos),
    ("energy/control/delete_device", qos),("energy/devices/+",qos)])

#start thread to receive mqtt messages
client.loop_start()

#thread to update reset dates
thread_next_reset = threading.Thread(name = 'thread to update reset dates',
                        target = mng_data,
                        args = (),
                        daemon = True
                        )
thread_next_reset.start()


# avoid script ends.
try:
    while True:
        pass


except KeyboardInterrupt:
    log = str(datetime.now())+" MQTT broker disconnecting"
    logging.warning(log)
    save_log(log)
    if thread_next_reset.isAlive():
        thread_next_reset._stop()

finally:
    #close mqtt services
    client.loop_stop()
    client.disconnect() 
    log = str(datetime.now())+" Closing"
    logging.info(log)
    save_log(log)
    sys.exit()