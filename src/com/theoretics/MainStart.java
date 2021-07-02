package com.theoretics;

import com.pi4j.wiringpi.Spi;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.platform.PlatformManager;
import com.pi4j.system.NetworkInfo;
import com.pi4j.system.SystemInfo;
import com.pi4j.wiringpi.Gpio;
import com.theoretics.Convert;
import com.theoretics.DateConversionHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainStart {

    String version = "v.3.0.2";
    String entranceID = "Acceptor CARD READER 2";

    String cardFromReader = "";

    //ArrayList<String> cards;
    private static Logger log = LogManager.getLogger(MainStart.class.getName());
    DateConversionHandler dch = new DateConversionHandler();
    private Thread ThrNetworkClock;
//    final GpioPinDigitalOutput pin1;

    AudioInputStream welcomeAudioIn = null;
    AudioInputStream thankyouAudioIn = null;
    AudioInputStream pleasewaitAudioIn = null;
    AudioInputStream errorAudioIn = null;
    AudioInputStream beepAudioIn = null;
    AudioInputStream takeCardAudioIn = null;
    AudioInputStream bgAudioIn = null;
    AudioInputStream insufficientaudioIn = null;
    Clip insufficientclip = null;
    Clip welcomeClip = null;
    Clip pleaseWaitClip = null;
    Clip thankyouClip = null;
    Clip beepClip = null;
    Clip takeCardClip = null;
    Clip errorClip = null;
    Clip bgClip = null;

    String strUID = "";
    String prevUID = "0";

    final GpioController gpio = GpioFactory.getInstance();

    // provision gpio pin #01 as an output pin and turn on
    final GpioPinDigitalOutput led1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_11, "HDDLED", PinState.LOW);
    final GpioPinDigitalOutput led2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_12, "POWERLED", PinState.LOW);

    final GpioPinDigitalOutput relayBarrier = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_08, "BARRIER", PinState.HIGH);
    final GpioPinDigitalOutput relayLights = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_09, "LIGHTS", PinState.HIGH);
    final GpioPinDigitalOutput relayFan = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_30, "FAN", PinState.HIGH);

    final GpioPinDigitalInput btnPower = gpio.provisionDigitalInputPin(RaspiPin.GPIO_26, PinPullResistance.PULL_UP);
    final GpioPinDigitalInput btnReset = gpio.provisionDigitalInputPin(RaspiPin.GPIO_27, PinPullResistance.PULL_UP);
    
    final GpioPinDigitalInput busy = gpio.provisionDigitalInputPin(RaspiPin.GPIO_22, PinPullResistance.PULL_DOWN);
    final GpioPinDigitalInput rejected = gpio.provisionDigitalInputPin(RaspiPin.GPIO_23, PinPullResistance.PULL_DOWN);
    final GpioPinDigitalInput received = gpio.provisionDigitalInputPin(RaspiPin.GPIO_24, PinPullResistance.PULL_DOWN);
    final GpioPinDigitalInput receivedDN = gpio.provisionDigitalInputPin(RaspiPin.GPIO_25, PinPullResistance.PULL_DOWN);
    
    final GpioPinDigitalInput carDetected = gpio.provisionDigitalInputPin(RaspiPin.GPIO_28, PinPullResistance.PULL_UP);
    
    final GpioPinDigitalOutput transistorAccept = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "ACCEPT", PinState.LOW);
    final GpioPinDigitalOutput transistorReject = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "REJECT", PinState.LOW);

    public void startProgram() {
        System.out.println(entranceID + " Tap Card Listener " + version);
//        System.out.println(entranceID + " Tap Card Listener " + version);

        try {
            welcomeAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/pleasecomeagain.wav"));
            welcomeClip = AudioSystem.getClip();
            welcomeClip.open(welcomeAudioIn);
        } catch (Exception ex) {
            notifyError(ex);
        }
        try {
            pleasewaitAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/plswaitGB.wav"));
            pleaseWaitClip = AudioSystem.getClip();
            pleaseWaitClip.open(pleasewaitAudioIn);
        } catch (Exception ex) {
            notifyError(ex);
        }
        try {
            thankyouAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/thankyou.wav"));
            thankyouClip = AudioSystem.getClip();
            thankyouClip.open(thankyouAudioIn);
        } catch (Exception ex) {
            notifyError(ex);
        }
        try {
            beepAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/beep.wav"));
            beepClip = AudioSystem.getClip();
            beepClip.open(beepAudioIn);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        try {
            takeCardAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/takecard.wav"));
            takeCardClip = AudioSystem.getClip();
            takeCardClip.open(takeCardAudioIn);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
//        try {
//            errorAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/beep.wav"));
//            errorClip = AudioSystem.getClip();
//            errorClip.open(errorAudioIn);
//        } catch (Exception ex) {
//            notifyError(ex);
//        }

//        try {
//            bgAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/bgmusic.wav"));
//            bgClip = AudioSystem.getClip();
//            bgClip.open(bgAudioIn);
//        } catch (Exception ex) {
//            notifyError(ex);
//        }

        try {
            insufficientaudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/pleasepay.wav"));
            insufficientclip = AudioSystem.getClip();
            insufficientclip.open(insufficientaudioIn);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        try {
            if (welcomeClip.isActive() == false) {
                welcomeClip.setFramePosition(0);
                welcomeClip.start();
                System.out.println("Welcome Message OK");
            }
        } catch (Exception ex) {
            notifyError(ex);
        }

//        this.cards = new ArrayList<String>();
        DataBaseHandler dbh = new DataBaseHandler(CONSTANTS.serverIP);

        Scanner scan = new Scanner(System.in);

        String text = null;
        String cardUID = null;
        System.out.println("Reader Ready!");
        
        transistorAccept.high();
//        transistorReject.high();
        Gpio.delay(1000);
        transistorAccept.low();
//        transistorReject.low();
        
//        transistorReject.pulse(300, true);
        System.out.println("Transistors Tested!");
//        transistorDispense.pulse(3000, true);
//        Gpio.delay(3000);
//        transistorReject.pulse(3000, true);
        //Testing Remotely
//        cards.add("ABC1234");

while (true) {
            System.out.print("!");                                            
            strUID = "";
            text = scan.nextLine();
            if (null != text) {
                try {
                    System.out.println("RAW: " + text);
                    cardUID = Long.toHexString(Long.parseLong(text));
                    if (text.startsWith("0")) {
                        cardUID = "0" + cardUID;
                    } else if (text.startsWith("00")) {
                        cardUID = "00" + cardUID;
                    } else if (text.startsWith("000")) {
                        cardUID = "000" + cardUID;
                    } else if (text.startsWith("0000")) {
                        cardUID = "0000" + cardUID;
                    }
                    //cardUID = Integer.toHexString(Integer.parseInt(text));
                    cardUID = cardUID.toUpperCase();
                    strUID = cardUID.substring(6, 8) + cardUID.substring(4, 6) + cardUID.substring(2, 4) + cardUID.substring(0, 2);
                    System.out.println("UID: " + cardUID.substring(6, 8) + cardUID.substring(4, 6) + cardUID.substring(2, 4) + cardUID.substring(0, 2));
                } catch (Exception ex) {
                    System.err.println("Card Conversion: " + ex);
                }
                //System.out.println("" + stats);
//                strUID = Convert.bytesToHex(tagid);
                try {
                    if (prevUID.compareToIgnoreCase(strUID) != 0) {
                        try {
                            if (pleaseWaitClip.isActive() == false) {
                                //haltButton = false;
                                pleaseWaitClip.setFramePosition(0);
                                pleaseWaitClip.start();
                            }

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        //Uncomment Below to disable Read same Card
//                    prevUID = strUID;
                        System.out.println("Card Read UID:" + strUID.substring(0, 8));
                        cardFromReader = strUID.substring(0, 8).toUpperCase();
//                        
                        if (cardFromReader.compareToIgnoreCase("") != 0) {
//                            cards.add(cardFromReader);
                            boolean isValid = false;
                            boolean isUpdated = false;
                            Date serverTime = dbh.getServerDateTime();
                            System.out.println("Time On Card*" + cardFromReader + "* :: " + serverTime);
//                            boolean alreadyExists = dbh.findCGHCard(cardFromReader);                        
                            try {
                                //isValid = dbh.writeManualEntrance(exitID, cardFromReader, "R", d2, timeStampIN, startCapture);
                                isValid = dbh.isExitValid(serverTime, cardFromReader);

                                //isValid = true;
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                            if (isValid) {
                                System.out.print("Sent Success");
                                transistorAccept.setState(PinState.LOW);
                                Gpio.delay(300);
                                transistorAccept.setState(PinState.HIGH);
//                                try {
//                                    Thread.sleep(3000);
//                                } catch (InterruptedException ex) {
//                                    java.util.logging.Logger.getLogger(MainStart.class.getName()).log(Level.SEVERE, null, ex);
//                                }
//                                Gpio.delay(300);
//                                transistorAccept.setState(PinState.HIGH);
                                Gpio.delay(700);
                                transistorAccept.setState(PinState.LOW);
                                try {
                                    relayBarrier.setState(PinState.LOW);
                                    Gpio.delay(300);
                                    relayBarrier.setState(PinState.HIGH);
                                    dbh.deleteValidCard(cardFromReader);
                                    try {
                                        if (thankyouClip.isActive() == false) {
                                            //haltButton = false;
                                            thankyouClip.setFramePosition(0);
                                            thankyouClip.start();
                                        }

                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                    Gpio.delay(300);

                                } catch (Exception ex) {
                                    java.util.logging.Logger.getLogger(MainStart.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                System.out.print("Sent InValid");
                                transistorReject.setState(PinState.LOW);
                                Gpio.delay(300);
                                transistorReject.setState(PinState.HIGH);
                                Gpio.delay(700);
//                                Gpio.delay(300);                                
//                                transistorReject.setState(PinState.HIGH);
                                Gpio.delay(300);                               
                                transistorReject.setState(PinState.LOW);

                                try {
                                    if (insufficientclip.isActive() == false) {
                                        //haltButton = false;
                                        insufficientclip.setFramePosition(0);
                                        insufficientclip.start();
                                    }

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }

                        }

                        //led1.pulse(1250, true);
//                    System.out.println("LED Open!");
                        //led2.pulse(1250, true);
                        // turn on gpio pin1 #01 for 1 second and then off
                        //System.out.println("--> GPIO state should be: ON for only 3 second");
                        // set second argument to 'true' use a blocking call
//                    c.showWelcome(700, false);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
//            strUID = null;
//
                Date now = new Date();
//            transistorDispense.pulse(300, true);
//        transistorReject.pulse(300, true);
//        System.out.println("Test Dispense");
                //System.out.println("Hour :  " + now.getHours());
                if (now.getHours() >= 18) {
                    //relayLights.low();
                }
                try {
                    if (SystemInfo.getCpuTemperature() >= 65) {
                        System.out.println("CPU Temperature   :  " + SystemInfo.getCpuTemperature());
//                    relayFan.low();
//                    relayBarrier.low();
//                    transistorDispense.pulse(300, true);
                    } else {
//                    relayFan.high();
//                    relayBarrier.high();
                    }
                } catch (Exception ex) {
                }

//            if (null != strUID) {
//                if (strUID.compareTo("") == 0) {
//                    transistorDispense.pulse(300, true);
//                }
//            } else {
//                transistorDispense.pulse(300, true);
//            }
                if (led1.isLow()) {
                    led1.high();
                } 
                if (led2.isLow()) {
                    led2.high();
                }

                try {
//                Thread.sleep(300);
//                rc522 = null;
//                Thread.sleep(3300);
//                Thread.yield();
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }

        }

    }

    private void notifyError(Exception ex) {
        System.out.println(ex.getMessage());
        try {
            if (errorClip.isActive() == false) {
                //haltButton = false;
                errorClip.setFramePosition(0);
                errorClip.start();
            }
        } catch (Exception ex2) {
            System.out.println(ex2.getMessage());
        }
    }

    public void testCard() {
        //读卡，得到序列号
//        if(rc522.Request(RaspRC522.PICC_REQIDL, back_bits) == rc522.MI_OK)
//            System.out.println("Detected:"+back_bits[0]);
//        if(rc522.AntiColl(tagid) != RaspRC522.MI_OK)
//        {
//            System.out.println("anticoll error");
//            return;
//        }
//
//        //Select the scanned tag，选中指定序列号的卡
//        int size=rc522.Select_Tag(tagid);
//        System.out.println("Size="+size);
//有两块(8*8)的屏幕
//		Led c = new Led((short)4);
//		c.brightness((byte)10);
        //打开设备
//		c.open();
        //旋转270度，缺省两个屏幕是上下排列，我需要的是左右排
//		c.orientation(270);
        //DEMO1: 输出两个字母
        //c.letter((short)0, (short)'Y',false);
        //c.letter((short)1, (short)'C',false);
//		c.flush();
        //c.showWelcome(700, false);
//		c.flush();
        //DEMO3: 输出一串字母
//		c.showMessage("Hello 0123456789$");
        //try {
        //	System.in.read();
        //	c.close();
        //} catch (IOException e) {
        // TODO Auto-generated catch block
        //	e.printStackTrace();
        //}

        //        System.out.println("Card Read UID:" + strUID.substring(0,2) + "," +
//                strUID.substring(2,4) + "," +
//                strUID.substring(4,6) + "," +
//                strUID.substring(6,8));
/*
        //default key
        byte []keyA=new byte[]{(byte)0x03,(byte)0x03,(byte)0x00,(byte)0x01,(byte)0x02,(byte)0x03};
        byte[] keyB=new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};


        //Authenticate,A密钥验证卡,可以读数据块2
        byte data[]=new byte[16];
        status = rc522.Auth_Card(RaspRC522.PICC_AUTHENT1A, sector,block, keyA, tagid);
        if(status != RaspRC522.MI_OK)
        {
            System.out.println("Authenticate A error");
            return;
        }

        status=rc522.Read(sector,block,data);
        //rc522.Stop_Crypto();
        System.out.println("Successfully authenticated,Read data="+Convert.bytesToHex(data));
        status=rc522.Read(sector,(byte)3,data);
        System.out.println("Read control block data="+Convert.bytesToHex(data));


        for (i = 0; i < 16; i++)
        {
            data[i]=(byte)0x00;
        }

        //Authenticate,B密钥验证卡,可以写数据块2
        status = rc522.Auth_Card(RaspRC522.PICC_AUTHENT1B, sector,block, keyB, tagid);
        if(status != RaspRC522.MI_OK)
        {
            System.out.println("Authenticate B error");
            return;
        }

        status=rc522.Write(sector,block,data);
        if( status== RaspRC522.MI_OK)
            System.out.println("Write data finished");
        else
        {
            System.out.println("Write data error,status="+status);
            return;
        }
         */
//        byte buff[]=new byte[16];
//
//        for (i = 0; i < 16; i++)
//        {
//            buff[i]=(byte)0;
//        }
//        status=rc522.Read(sector,block,buff);
//        if(status == RaspRC522.MI_OK)
//            System.out.println("Read Data finished");
//        else
//        {
//            System.out.println("Read data error,status="+status);
//            return;
//        }
//
//        System.out.print("sector"+sector+",block="+block+" :");
//        String strData= Convert.bytesToHex(buff);
//        for (i=0;i<16;i++)
//        {
//            System.out.print(strData.substring(i*2,i*2+2));
//            if(i < 15) System.out.print(",");
//            else System.out.println("");
//        }
    }

    public void setupLED() {
        System.out.println("Setting Up GPIO!");
        if (Gpio.wiringPiSetup() == -1) {
            System.out.println(" ==>> GPIO SETUP FAILED");
            return;
        }

        led1.setShutdownOptions(true, PinState.LOW);
        led2.setShutdownOptions(true, PinState.LOW);

        relayFan.high();
        relayBarrier.high();
        relayLights.high();

        transistorReject.setShutdownOptions(true, PinState.LOW);
        transistorAccept.setShutdownOptions(true, PinState.LOW);

        transistorReject.setShutdownOptions(true);
        transistorAccept.setShutdownOptions(true);
        
        rejected.setMode(PinMode.DIGITAL_INPUT);
        rejected.setPullResistance(PinPullResistance.PULL_UP);
        busy.setMode(PinMode.DIGITAL_INPUT);
        busy.setPullResistance(PinPullResistance.PULL_UP);
        received.setMode(PinMode.DIGITAL_INPUT);
        received.setPullResistance(PinPullResistance.PULL_UP);
        receivedDN.setMode(PinMode.DIGITAL_INPUT);
        receivedDN.setPullResistance(PinPullResistance.PULL_UP);
        
        carDetected.setMode(PinMode.DIGITAL_INPUT);
        carDetected.setPullResistance(PinPullResistance.PULL_UP);
        btnPower.setMode(PinMode.DIGITAL_INPUT);
        btnPower.setPullResistance(PinPullResistance.PULL_UP);
        btnReset.setMode(PinMode.DIGITAL_INPUT);
        btnReset.setPullResistance(PinPullResistance.PULL_UP);

//        relayBarrier.setShutdownOptions(true, PinState.LOW);
//        relayFan.setShutdownOptions(true, PinState.LOW);
//        relayLights.setShutdownOptions(true, PinState.LOW);
//        btnDispense.setMode(PinMode.DIGITAL_INPUT);
//        btnDispense.setPullResistance(PinPullResistance.PULL_UP);
//
//        // set shutdown state for this input pin
//        btnDispense.setShutdownOptions(true);
        btnPower.setShutdownOptions(true);
        btnReset.setShutdownOptions(true);
        
        rejected.setShutdownOptions(true);
        busy.setShutdownOptions(true);
        received.setShutdownOptions(true);
        receivedDN.setShutdownOptions(true);
        carDetected.setShutdownOptions(true);
        btnReset.setShutdownOptions(true);
        led1.high(); //Show POWER is ON led1.high
        led2.blink(300, 3000);

        transistorAccept.low();
        transistorReject.low();        
                

        btnPower.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                System.out.println("POWER LED Pressed!");
                led1.pulse(5000);
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
                try {
                    Runtime r = Runtime.getRuntime();
                    Process p = r.exec("sudo reboot now");//u - update f - force
                    Thread.sleep(30000);

                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }

        });

        // create and register gpio pin listener
        btnReset.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                System.out.println("RESET LED!");
                led1.pulse(5000);
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
                try {
                    Runtime r = Runtime.getRuntime();
                    Process p = r.exec("sudo shutdown now");//u - update f - force
                    Thread.sleep(30000);

                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }

        });
        
        
        busy.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                if (event.getState() == PinState.LOW) {
                    System.out.println("IS ON THE MOUTH");
                }                
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());                
            }

        });

        rejected.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                if (event.getState() == PinState.LOW) {
                    System.out.println("CARD is REJECTED");
                    //LOW = REJECTED AND ON THE MOUTH
                    //HIGH = REJECTED AND TAKEN BY PARKER
                } else if (event.getState() == PinState.HIGH) {
                    System.out.println("REJECTED AND PRESENTED TO PARKER");
                }
                //System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());                
            }

        });

        received.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                if (event.getState() == PinState.LOW) {
                    System.out.println("CARD is RECEIVED");
                    //LOW = RECEIVED AND READY FOR CARD SCANNER
                    //HIGH = RECEIVED AND LEFT THE SCANNING AREA
                    Gpio.delay(300);
                    transistorAccept.low();
                    Gpio.delay(300);
                    transistorAccept.high();
                    Gpio.delay(300);
                    transistorAccept.low();
                } else if (event.getState() == PinState.HIGH) {
                    System.out.println("RECEIVED AND LEFT THE SCANNING AREA");
                }
                
                //System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());                
            }

        });


        receivedDN.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                if (event.getState() == PinState.LOW) {
                    System.out.println("CARD IS RECEIVED DOWN");                               
                }
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());                
            }

        });

        
        carDetected.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                if (event.getState() == PinState.LOW) {
                    System.out.println("CAR is now Present");
                }
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());                
            }

        });

        
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void main(String[] args) throws InterruptedException {
        MainStart m = new MainStart();
        m.setupLED();
        InfoClass i = new InfoClass();
        i.showInfo();
        m.startProgram();
//        while (true) {
//            Thread.sleep(5000L);
//        }
//
    }

}
