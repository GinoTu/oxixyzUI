/*
  Hardware Connections (Breakoutboard to Arduino):
  -5V = 5V
  -GND = GND
  -SDA = 20
  -SCL = 21
  -Tx = 10
  -Rx = 11
*/

#include <Wire.h>
#include "MAX30105.h"
#include "spo2_algorithm.h"
#include <SoftwareSerial.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
SoftwareSerial BT(10, 11);//Rx Tx
 const int MPU_addr = 0x68;
MAX30105 particleSensor;
Adafruit_MPU6050 mpu;

/*oxi define */
#define MAX_BRIGHTNESS 255

#if defined(__AVR_ATmega328P__) || defined(__AVR_ATmega168__)
//Arduino Uno doesn't have enough SRAM to store 100 samples of IR led data and red led data in 32-bit format
//To solve this problem, 16-bit MSB of the sampled data will be truncated. Samples become 16-bit data.
uint16_t irBuffer[100]; //infrared LED sensor data
uint16_t redBuffer[100];  //red LED sensor data
#else
uint32_t irBuffer[100]; //infrared LED sensor data
uint32_t redBuffer[100];  //red LED sensor data
#endif

int32_t bufferLength; //data length
int32_t spo2; //SPO2 value
int8_t validSPO2; //indicator to show if the SPO2 calculation is valid
int32_t heartRate; //heart rate value
int8_t validHeartRate; //indicator to show if the heart rate calculation is valid

byte pulseLED = 11; //Must be on PWM pin
byte readLED = 13; //Blinks with each data read
/*oxi define end*/

void setup()
{
  /*Baud Rate initial*/
  Serial.begin(9600); // initialize serial communication at 115200 bits per second:
  BT.begin(9600);
  /*Baud Rate initial end*/

  /*check chips*/
    if (!particleSensor.begin(Wire, I2C_SPEED_FAST))
    {
    BT.println(F("MAX30105 was not found. Please check wiring/power."));
    while (1);
    }
    if (!mpu.begin())
    {
      BT.println(F("MPU6050 was not found. Please check wiring/power."));
      while (1);
    }
  /*check chips end*/

  /*oxi initial*/
  pinMode(pulseLED, OUTPUT);
  pinMode(readLED, OUTPUT);
  byte ledBrightness = 60; //Options: 0=Off to 255=50mA
  byte sampleAverage = 4; //Options: 1, 2, 4, 8, 16, 32
  byte ledMode = 2; //Options: 1 = Red only, 2 = Red + IR, 3 = Red + IR + Green
  byte sampleRate = 100; //Options: 50, 100, 200, 400, 800, 1000, 1600, 3200
  int pulseWidth = 411; //Options: 69, 118, 215, 411
  int adcRange = 4096; //Options: 2048, 4096, 8192, 16384
  particleSensor.setup(ledBrightness, sampleAverage, ledMode, sampleRate, pulseWidth, adcRange); //Configure sensor with these settings
  /*oxi initial end*/

  /*xyz initial*/
  mpu.setHighPassFilter(MPU6050_HIGHPASS_0_63_HZ);
  mpu.setMotionDetectionThreshold(1);
  mpu.setMotionDetectionDuration(20);
  mpu.setInterruptPinLatch(true);	// Keep it latched.  Will turn off when reinitialized.
  mpu.setInterruptPinPolarity(true);
  mpu.setMotionInterrupt(true);
  /*xyz initial end*/
}

void loop()
{
  Serial.println("initing...");
  /*xyz step up*/
  float AcX, AcY, AcZ; 
  int GyX, GyY, GyZ, Tmp;
  bool step1 = false;
  bool step2 = false;
  bool step3 = false;
  bool fallWarn = false;
  int timeCounter = 0;
  /*xyz step up*/

  /*millis set up*/
  unsigned long timeNow = millis();
  int distate = 0;
  int period=280;
  /*millis set up end*/

  /*oxi measure pt.1*/
  bufferLength = 100; //buffer length of 100 stores 4 seconds of samples running at 25sps
  for (byte i = 0 ; i < bufferLength ; i++)
  {
    while (particleSensor.available() == false) //do we have new data?
      particleSensor.check(); //Check the sensor for new data

    redBuffer[i] = particleSensor.getRed();
    irBuffer[i] = particleSensor.getIR();
    particleSensor.nextSample(); //We're finished with this sample so move to next sample
  }
  //calculate heart rate and SpO2 after first 100 samples (first 4 seconds of samples)
  maxim_heart_rate_and_oxygen_saturation(irBuffer, bufferLength, redBuffer, &spo2, &validSPO2, &heartRate, &validHeartRate);
  /*oxi measure pt.1 end*/


  //Continuously taking samples
  while (1)
  {
    /*oxi measure pt.2*/
    for (byte i = 25; i < 100; i++)
    {
      redBuffer[i - 25] = redBuffer[i];
      irBuffer[i - 25] = irBuffer[i];
    }
    /*oxi measure pt.2 end*/

    //take 25 sets of samples before calculating the heart rate.
    for (byte i = 75; i < 100; i++)
    {
      /*oxi measure pt.3*/
      while (particleSensor.available() == false) //do we have new data?
        particleSensor.check(); //Check the sensor for new data

      digitalWrite(readLED, !digitalRead(readLED)); //Blink onboard LED with every data read

      redBuffer[i] = particleSensor.getRed();
      irBuffer[i] = particleSensor.getIR();
      particleSensor.nextSample(); //We're finished with this sample so move to next sample
      /*oxi measure pt.3 end*/

      /*xyz measure*/
      Wire.beginTransmission(MPU_addr);
      Wire.write(0x3B);  // starting with register 0x3B (ACCEL_XOUT_H)
      Wire.endTransmission(false);
      Wire.requestFrom(MPU_addr, 14, true); // request a total of 14 registers
      AcX = ((Wire.read() << 8 | Wire.read()) - 2050) / 16384.00; // 0x3B (ACCEL_XOUT_H) & 0x3C (ACCEL_XOUT_L)
      AcY = ((Wire.read() << 8 | Wire.read()) - 77) / 16384.00; // 0x3D (ACCEL_YOUT_H) & 0x3E (ACCEL_YOUT_L)
      AcZ = ((Wire.read() << 8 | Wire.read()) - 1947) / 16384.00; // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)
      Tmp = ((Wire.read() << 8 | Wire.read())); // 0x41 (TEMP_OUT_H) & 0x42 (TEMP_OUT_L)
      GyX = ((Wire.read() << 8 | Wire.read()) + 270) / 131.07; // 0x43 (GYRO_XOUT_H) & 0x44 (GYRO_XOUT_L)
      GyY = ((Wire.read() << 8 | Wire.read()) - 351) / 131.07; // 0x45 (GYRO_YOUT_H) & 0x46 (GYRO_YOUT_L)
      GyZ = ((Wire.read() << 8 | Wire.read()) +136) / 131.07; // 0x47 (GYRO_ZOUT_H) & 0x48 (GYRO_ZOUT_L)

      float fall = pow(pow(AcX, 2) + pow(AcY, 2) + pow(AcZ, 2), 0.5) * 10;
      float angleChange = pow(pow(GyX, 2) + pow(GyY, 2) + pow(GyZ, 2), 0.5);

       /*三階段判斷 : 失重 -> 撞擊 -> 靜止*/
      if(step3 == false)
      {
        if(fall < 4 && step2 == false)
        {
        
          step1 = true;
          timeCounter = millis();
        }
        if(fall > 14 && step1 == true)
        {
          step2 = true;
          step1 = false;
        }
        if(step2 == true && angleChange >= 30 && angleChange <= 400)
        {
          step3 = true;
          step2 = false;
        }
      }
      else
      {
        if((millis() - timeCounter) < 5000 && angleChange >= 10)
        { 
          step3 = false;
        }
        else if((millis() - timeCounter) > 5000)
        {
          fallWarn = true;
        }
      }
      /*xyz measure end*/

      /*display*/
      if(period <= millis() - timeNow && distate == 0)
      {
        BT.print(spo2, DEC);
        BT.print("S");
        
        Serial.print(spo2, DEC);
        Serial.print("S");
        
        timeNow = millis();
        distate = 1;

      }
      else if(period <= millis() - timeNow && distate == 1)
      {
        BT.print(heartRate, DEC);
        BT.print("H");

        Serial.print(heartRate, DEC);
        Serial.print("H");

        timeNow = millis();
        distate = 2;
      }
      else if(period <= millis() - timeNow && distate == 2)
      {
        if(fallWarn == true)
          BT.print(F("1"));//Fell!!!!!
        else
          BT.print(F("0"));//all good
        BT.print("F");

        if(fallWarn == true)
          Serial.print(F("1"));//Fell!!!!!
        else
          Serial.print(F("0"));//all good
        Serial.println("F");

        timeNow = millis();
        distate = 0;
      }
      /*display end*/
    }

    //oxi measure pt.4
    maxim_heart_rate_and_oxygen_saturation(irBuffer, bufferLength, redBuffer, &spo2, &validSPO2, &heartRate, &validHeartRate);
  }
}



