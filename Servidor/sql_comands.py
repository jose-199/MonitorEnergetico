from locale import currency
import pymysql
import Crypto
import binascii
from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP
from datetime import datetime
from datetime import date
import pytz 

class db_manage(object):

    def __init__(self):
        self.db = ""
        self.cursor = "" 
        #-------------------------------- Decrypt credentials --------------------------------------
        prikey = open("privateKey.txt", "r")
        private_key = prikey.read()
        prikey.close()

        private_key = RSA.importKey(binascii.unhexlify(private_key))

        encrypted_file = open("credentials_db.txt", "rb")
        encrypted_credentials = encrypted_file.read()
        encrypted_file.close()

        cipher = PKCS1_OAEP.new(private_key)
        self.credentials = (cipher.decrypt(encrypted_credentials)).decode()
        private_key=""
        self.credentials = self.credentials.split('\n')
        #--------------------------------------------------------------------------------------------
    
    def delete_old_registers(self,year_ago):
        self.start_conn()
        #get all tables
        sql = ("SHOW TABLES")
        self.cursor.execute(sql)
        info = self.cursor.fetchall()
        for tables in info:
            if tables[0]!="devices":
                sql = ("DELETE FROM " + tables[0].replace("-","_") + 
                        " WHERE date <= '" + str(year_ago) + "'")
                self.cursor.execute(sql)
        self.close_conn()  

    def histroy(self,device,name,date_ini,date_final=None):
        hisGTQ = self.request_data("GTQ",device,name,date_ini,date_final)
        hisUSD = self.request_data("USD",device,name,date_ini,date_final)
        if hisGTQ!=() and hisUSD!=():
            return (hisGTQ[0]+hisUSD[0],((hisGTQ[1]," GTQ + "),(hisUSD[1]," USD")))
        if hisGTQ!=() and hisUSD==():
            return (hisGTQ[0],((hisGTQ[1]," GTQ"),("","")))
        if hisGTQ==() and hisUSD!=():
            return (hisUSD[0],(("",""),(hisUSD[1]," USD")))
        return ()

    #get consumption history
    def request_data(self, currency,device,name,date_ini,date_final=None):
        self.start_conn()
        # get the first consumption of init date 
        # in case a substraction is necesary
        last_date = date_ini
        sql = ("SELECT payday, DATE, energy, charge, id FROM "+device.replace("-","_")+  
            " WHERE (date='"+ str(last_date) +"') AND (currency="+'"'+currency+'")' +
            'AND (name="'+name+'")')
        self.cursor.execute(sql)
        last_info = self.cursor.fetchall()

        if date_final==None: #Consult only one day of energy data
            sql = ("SELECT payday, DATE, energy, charge, id FROM "+device.replace("-","_")+  
                    " WHERE (date='"+ str(date_ini) +"') AND (currency="+'"'+currency+'")' +
            'AND (name="'+name+'")')
            self.cursor.execute(sql)
            info = self.cursor.fetchall()
            if info != (): 
                if ((info[-1][0] == info[-1][1].day) or (info[-1][0] != info[0][0])):
                    energy = info[-1][2]
                    charge = info[-1][3]
                else:
                    energy = info[-1][2]-last_info[0][2]
                    charge = info[-1][3]-last_info[0][3]
                return (round(energy,3),round(charge,3))
            return ()
        else:                #Consult energy data between 2 dates            
            sql = ("SELECT payday, DATE, energy, charge, id FROM "+device.replace("-","_")+ 
                    " WHERE (date BETWEEN '"+str(date_ini)+"' AND '"+str(date_final) +
                    "') AND (currency="+'"'+currency+'")' + 'AND (name="'+name+'")')
            self.cursor.execute(sql)
            info = self.cursor.fetchall()

            if info != ():            
                prev_energy = 0
                prev_charge = 0
                if last_info != ():
                    #if first consumption of init date has registers get first 
                    #consumption register in case a substraction is necesary
                    prev_energy = last_info[0][2]
                    prev_charge = last_info[0][3]
                add_reg=""       #indicate when a data need to be added to counters 
                energy=0         #count to energy
                charge=0         #count to charge
                res=True         #indicate when a substraction is necesary

                for i in range(len(info)-2):
                    if i> 0:
                        if info[i-1][0]!=info[i][0]: #verify if payday changed
                            energy+=info[i-1][2]
                            charge+=info[i-1][3]
                            if res and date_ini.day!=(info[0][0]+1): 
                                #a substraction it is necesary because date_ini is
                                # not equal to payday and a substraction has not been
                                # applied yet 
                                energy -= prev_energy
                                charge -= prev_charge
                                res=False #indicates a substraction is not longer necesary        
                        if info[i][0] == info[i][1].day:
                            #indicates payday it is equal to register's day
                            #and next time when they changed it will be necesary
                            #to add values of the las equal register
                            add_reg="equal"
                        else:
                            if add_reg=="equal":
                                #add the values of the last register of payday
                                add_reg=""
                                energy += info[i-1][2]
                                charge += info[i-1][3]
                                if res and date_ini.day!=(info[0][0]+1):
                                    #a substraction it is necesary because date_ini is
                                    # not equal to payday and a substraction has not been
                                    # applied yet
                                    energy -= prev_energy
                                    charge -= prev_charge
                                    res=False #indicates a substraction is not longer necesary 
                #add values of last register to counters 
                energy += info[len(info)-1][2]
                charge += info[len(info)-1][3]
                if res and date_ini.day!=(info[0][0]+1):
                    # a substraction it is necesary because date_ini is
                    # not equal to payday and a substraction has not been
                    # applied yet 
                    energy -= prev_energy
                    charge -= prev_charge
                    self.close_conn()
                return (round(energy,3),round(charge,3))
        self.close_conn()
        return ()

    def insert_data(self,device,name,rate,currency,payday,volt,current,power,energy,
                    charge,date,time,timezone):
        self.start_conn()
        sql = ("INSERT INTO " + device.replace("-","_") + " VALUES(NULL," +
                                            '"' + device + '"' + "," +
                                            '"' + name + '"' + "," +
                                            '"' + str(rate) + '"' + "," +
                                            '"' + currency + '"' + "," +
                                            '"' + str(payday) + '"' + "," +
                                            '"' + str(volt) + '"' + "," +
                                            '"' + str(current) + '"' + "," +
                                            '"' + str(power) + '"' + "," +
                                            '"' + str(energy) + '"' + "," +
                                            '"' + str(charge) + '"' + "," +
                                            '"' + str(date) + '"' + "," +
                                            '"' + str(time) + '"' + "," +
                                            '"' + timezone + '"' + ")")
        try:
            if(self.cursor.execute(sql)==1):
                self.close_conn()
                return "ok"            
        except:
            self.close_conn()
            return "error adding data"

    #ask if some measurement device need reset
    def need_reset(self, device):
        self.start_conn()
        sql = "SELECT need_reset FROM devices WHERE device= "+'"'+device+'"'
        try:
            self.cursor.execute(sql)
            resp = self.cursor.fetchall()
            self.close_conn()
            return resp[0][0]            
        except:
            self.close_conn()
            return 0
    
    def reg_reset(self,device,value):
        self.start_conn()
        sql = ("UPDATE devices SET need_reset="+str(value)+
                                " WHERE device=" + '"' + device + '"')
        try:
            self.cursor.execute(sql)
            self.close_conn()
            return "ok"
        except:
            self.close_conn()
            return "error trying to update new reset"
        
    #get all measurement devices
    def get_devices(self):
        self.start_conn()
        sql = "SELECT device FROM devices"
        try:
            self.cursor.execute(sql)
            list = self.cursor.fetchall()
            self.close_conn()
            return list
        except:
            self.close_conn()
            return ()

    def get_next_reset(self):
        self.start_conn()
        sql = "SELECT device, next_reset, timezone FROM devices"
        try:
            self.cursor.execute(sql)
            list = self.cursor.fetchall()
            self.close_conn()
            return list
        except:
            self.close_conn()
            return ()

    #update measurement device's next reset
    def set_next_reset(self, device, next_reset):
        self.start_conn()
        sql = ("UPDATE devices SET next_reset=" + '"' + str(next_reset) + '"' +
                            ", need_reset=1 " +
                            "WHERE device=" + '"' + device + '"') 
        try:
            self.cursor.execute(sql)
            self.close_conn()
            return "ok"
        except:
            self.close_conn()
            return "error trying to update next date reset"

    #update measurement device's information 
    def update(self, device, name, rate, currency, payday, timezone):
        self.start_conn()
        today = datetime.now(pytz.timezone(timezone)).date()

        #determine payday
        if (int(payday)>=today.day):
            next_reset = str(today.year)+"-"+str(today.month)+"-"+payday
        else:
            next_reset = str(today.year)+"-"+str(int(today.month) + 1)+"-"+payday
        
        #verify if timezone changed 
        sql = 'SELECT timezone FROM devices WHERE device="'+device+'"'
        try:
            self.cursor.execute(sql) 
            old_timezone = self.cursor.fetchall()
            #if timezone change all registers of day will be deleted
            #to avoid date issues 
            if (old_timezone[0][0] != timezone):
                sql = ("DELETE FROM " + device.replace("-","_") + " WHERE date>=" + 
                        '"' + str(today) + '"')
                self.cursor.execute(sql)
        except:
            pass            
        
        sql = ("UPDATE devices SET name_device=" + '"' + name + '"' +
                                   ", rate=" + '"' + rate + '"' +
                                   ", currency=" + '"' + currency + '"' +
                                   ", payday=" + '"' + payday + '"' +
                                   ", timezone=" + '"' + timezone + '"' +
                                   ", next_reset=" + '"' + next_reset + '"' +     
                                " WHERE device=" + '"' + device + '"')
        try:
            self.cursor.execute(sql)
            self.close_conn()
            return "ok"
        except:
            self.close_conn()
            return "error"

    def add_device(self, device):
        self.start_conn()
        sql = ("INSERT INTO devices VALUES(" + '"' + device + '"' + 
                ",NULL,NULL,NULL,NULL,NULL,NULL,false)")
        sql2 = ("CREATE TABLE " + device.replace("-","_") +
                "(id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,"+
                "device VARCHAR(50),"+ 
                "name VARCHAR(50),"+ 
                "rate float,"+ 
                "currency VARCHAR(3),"+ 
                "payday TINYINT,"+ 
                "voltage float,"+ 
                "current float,"+ 
                "power float,"+ 
                "energy float,"+ 
                "charge float,"+ 
                "date DATE," + 
                "time TIME,"+
                "timezone TEXT,"+ 
                "foreign key (device) references devices(device))")
        try:
            if(self.cursor.execute(sql)==1):
                if(self.cursor.execute(sql2)==0):
                    self.close_conn()
                    return "ok"
                else:
                    self.close_conn()
                    return device + " could not create consumption table"    
            else:
                self.close_conn()
                return device + " could not be added to devices table"
        except:
            self.close_conn()
            return "error"

    def delete_device(self, device):
        self.start_conn()
        sql = "DELETE FROM devices WHERE device=" + '"' + device + '"'
        sql2 = "DROP TABLE " + device.replace("-","_")
        try:
            if(self.cursor.execute(sql2)==0):
                if(self.cursor.execute(sql)==1):
                    self.close_conn()
                    return "ok"
                else:
                    self.close_conn()
                    return device + " register could not be deleted"        
            else:
                self.close_conn()
                return device + " table could not be deleted"
        except:
            self.close_conn()
            return "error"
    
    #open connection to database 
    def start_conn(self):
        self.db=pymysql.connect(host=self.credentials[0],
            port=int(self.credentials[2]),
            user=self.credentials[1],
            passwd=self.credentials[3],
            db=self.credentials[4],
            charset='utf8') #connect database
        self.cursor = self.db.cursor()      # Get cursor() method
    
    #close connection to database 
    def close_conn(self):
        self.db.commit()
        self.cursor.close()   
        self.db.close()