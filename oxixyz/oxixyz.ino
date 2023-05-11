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
#include "ssd1306h.h"
#include "MAX30102.h"
#include "Pulse.h"
#include <avr/pgmspace.h>
#include <EEPROM.h>

SoftwareSerial BT(10, 11);//Rx Tx
 const int MPU_addr = 0x68;
MAX30105 particleSensor;
Adafruit_MPU6050 mpu;
Pulse pulseIR;
Pulse pulseRed;
MAFilter bpm;

/*oxi define */
// Routines to clear and set bits 
#ifndef cbi
#define cbi(sfr, bit) (_SFR_BYTE(sfr) &= ~_BV(bit))
#endif
#ifndef sbi
#define sbi(sfr, bit) (_SFR_BYTE(sfr) |= _BV(bit))
#endif

#define MAX_BRIGHTNESS 255

#define LED LED_BUILTIN
#define OPTIONS 7

//add
int  beatAvg;
int  SPO2, SPO2f;
int  voltage;
bool filter_for_graph = false;
bool draw_Red = false;
const uint8_t MAXWAVE = 72;

byte pulseLED = 11; //Must be on PWM pin
byte readLED = 13; //Blinks with each data read


/*oxi define end*/

//spo2_table is approximated as  -45.060*ratioAverage* ratioAverage + 30.354 *ratioAverage + 94.845 ;
const uint8_t spo2_table[184] PROGMEM =
        { 95, 95, 95, 96, 96, 96, 97, 97, 97, 97, 97, 98, 98, 98, 98, 98, 99, 99, 99, 99, 
          99, 99, 99, 99, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 
          100, 100, 100, 100, 99, 99, 99, 99, 99, 99, 99, 99, 98, 98, 98, 98, 98, 98, 97, 97, 
          97, 97, 96, 96, 96, 96, 95, 95, 95, 94, 94, 94, 93, 93, 93, 92, 92, 92, 91, 91, 
          90, 90, 89, 89, 89, 88, 88, 87, 87, 86, 86, 85, 85, 84, 84, 83, 82, 82, 81, 81, 
          80, 80, 79, 78, 78, 77, 76, 76, 75, 74, 74, 73, 72, 72, 71, 70, 69, 69, 68, 67, 
          66, 66, 65, 64, 63, 62, 62, 61, 60, 59, 58, 57, 56, 56, 55, 54, 53, 52, 51, 50, 
          49, 48, 47, 46, 45, 44, 43, 42, 41, 40, 39, 38, 37, 36, 35, 34, 33, 31, 30, 29, 
          28, 27, 26, 25, 23, 22, 21, 20, 19, 17, 16, 15, 14, 12, 11, 10, 9, 7, 6, 5, 
          3, 2, 1 } ;

/*
 *   Record, scale  and display PPG Wavefoem
 */


class Waveform {
  public:
    Waveform(void) {wavep = 0;}

      void record(int waveval) {
        waveval = waveval/8;         // scale to fit in byte  缩放以适合字节
        waveval += 128;              //shift so entired waveform is +ve  
        waveval = waveval<0? 0 : waveval;
        waveform[wavep] = (uint8_t) (waveval>255)?255:waveval; 
        wavep = (wavep+1) % MAXWAVE;
      }
  
      void scale() {
        uint8_t maxw = 0;
        uint8_t minw = 255;
        for (int i=0; i<MAXWAVE; i++) { 
          maxw = waveform[i]>maxw?waveform[i]:maxw;
          minw = waveform[i]<minw?waveform[i]:minw;
        }
        uint8_t scale8 = (maxw-minw)/4 + 1;  //scale * 8 to preserve precision
        uint8_t index = wavep;
        for (int i=0; i<MAXWAVE; i++) {
          disp_wave[i] = 31-((uint16_t)(waveform[index]-minw)*8)/scale8;
          index = (index + 1) % MAXWAVE;
        }
      }

private:
    uint8_t waveform[MAXWAVE];
    uint8_t disp_wave[MAXWAVE];
    uint8_t wavep = 0;
    
} wave;

int getVCC() {
  //reads internal 1V1 reference against VCC
  #if defined(__AVR_ATmega1284P__)
    ADMUX = _BV(REFS0) | _BV(MUX4) | _BV(MUX3) | _BV(MUX2) | _BV(MUX1);  // For ATmega1284
  #else
    ADMUX = _BV(REFS0) | _BV(MUX3) | _BV(MUX2) | _BV(MUX1);  // For ATmega328
  #endif
  delay(2); // Wait for Vref to settle
  ADCSRA |= _BV(ADSC); // Convert
  while (bit_is_set(ADCSRA, ADSC));
  uint8_t low = ADCL;
  unsigned int val = (ADCH << 8) | low;
  //discard previous result
  ADCSRA |= _BV(ADSC); // Convert
  while (bit_is_set(ADCSRA, ADSC));
  low = ADCL;
  val = (ADCH << 8) | low;
  
  return (((long)1024 * 1100) / val)/100;  
}


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
  filter_for_graph = EEPROM.read(OPTIONS);
  draw_Red = EEPROM.read(OPTIONS+1);
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

long lastBeat = 0;    //Time of the last beat 
long displaytime = 0; //Time of the last display update
bool led_on = false;

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
  bool ct = 0;
  /*xyz step up*/

  /*millis set up*/
  unsigned long timeNow = millis();
  unsigned long forBeats = millis();

  int distate = 0;
  int period=1000;
  /*millis set up end*/



  //Continuously taking samples
  while (1)
  {

    /*oxi measure*/
    while (particleSensor.available() == false) //do we have new data?
      particleSensor.check(); //Check the sensor for new data

    digitalWrite(readLED, !digitalRead(readLED)); //Blink onboard LED with every data read

    uint32_t redValue = particleSensor.getRed();
    uint32_t irValue = particleSensor.getIR();
    long now = millis();
    particleSensor.nextSample(); //We're finished with this sample so move to next sample
    if (irValue<140000) {
      voltage = getVCC();    
    } else 
    {
      // remove DC element移除直流元件
      int16_t IR_signal, Red_signal;
      bool beatRed, beatIR;
      if (!filter_for_graph) 
      {//图形过滤器
        IR_signal =  pulseIR.dc_filter(irValue) ;
        Red_signal = pulseRed.dc_filter(redValue);
        beatRed = pulseRed.isBeat(pulseRed.ma_filter(Red_signal));
        beatIR =  pulseIR.isBeat(pulseIR.ma_filter(IR_signal));        
      } else 
      {
        IR_signal =  pulseIR.ma_filter(pulseIR.dc_filter(irValue)) ;
        Red_signal = pulseRed.ma_filter(pulseRed.dc_filter(redValue));
        beatRed = pulseRed.isBeat(Red_signal);
        beatIR =  pulseIR.isBeat(IR_signal);
      }
      // invert waveform to get classical BP waveshape
      wave.record(draw_Red ? -Red_signal : -IR_signal ); 
      // check IR or Red for heartbeat     
      if (draw_Red ? beatRed : beatIR)
      {
        long btpm = 60000/(now - lastBeat);
        if (btpm > 0 && btpm < 200) beatAvg = bpm.filter((int16_t)btpm);
        lastBeat = now; 
        digitalWrite(LED, HIGH); 
        led_on = true;
        // compute SpO2 ratio
        long numerator   = (pulseRed.avgAC() * pulseIR.avgDC())/256;
        long denominator = (pulseRed.avgDC() * pulseIR.avgAC())/256;
        int RX100 = (denominator>0) ? (numerator * 100)/denominator : 999;
        // using formula
        SPO2f = (10400 - RX100*17+50)/100;  
        // from table
        if ((RX100>=0) && (RX100<184))
          SPO2 = pgm_read_byte_near(&spo2_table[RX100]);
      }
    }
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
      BT.print(" ");
      BT.print(SPO2, DEC);
      BT.print("S");
        
      Serial.print(SPO2, DEC);
      Serial.print("S");
        
      timeNow = millis();
      distate = 1;

    }
    else if(period <= millis() - timeNow && distate == 1)
    {
      BT.print(" ");
      BT.print(beatAvg, DEC);
      BT.print("H");

      Serial.print(beatAvg, DEC);
      Serial.print("H");

      timeNow = millis();
      distate = 2;
    }
    else if(period <= millis() - timeNow && distate == 2)
    {
      if(fallWarn == true)
      {
        BT.print(F(" 1F"));//Fell!!!!!
        ct ++;
        }else
      {
        BT.print(F(" 0F"));//all good
      }
        
      if(fallWarn == true)
        Serial.print(F("1"));//Fell!!!!!
      else
        Serial.print(F("0"));//all good
      Serial.println("F");

      if(ct == 1)
      {
        fallWarn = false;
        step1 = false;
        step2 = false;
        step3 = false;
        ct =0;
      }
      timeNow = millis();
      distate = 0;
    }
    /*display end*/
  }
}






