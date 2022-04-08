import Crypto
import binascii
from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP
#-------------------------------- Decrypt credentials --------------------------------------
prikey = open("privateKey.txt", "r")
private_key = prikey.read()
prikey.close()

private_key = RSA.importKey(binascii.unhexlify(private_key))

encrypted_file = open("credentials_mqtt.txt", "rb")
encrypted_credentials = encrypted_file.read()
encrypted_file.close()

cipher = PKCS1_OAEP.new(private_key)
cred_mqtt = (cipher.decrypt(encrypted_credentials)).decode()
private_key=""
cred_mqtt = cred_mqtt.split('\n')
#--------------------------------------------------------------------------------------------