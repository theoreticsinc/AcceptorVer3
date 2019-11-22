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

    String version = "v.2.0.4";
    String entranceID = "Acceptor CARD READER 2";

    String cardFromReader = "";

    ArrayList<String> cards;
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
    final GpioPinDigitalOutput led1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_21, "HDDLED", PinState.LOW);
    final GpioPinDigitalOutput led2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_26, "POWERLED", PinState.LOW);

//    final GpioPinDigitalInput pinDispenserCardOK = gpio.provisionDigitalInputPin(RaspiPin.GPIO_23, PinPullResistance.PULL_UP);
//    final GpioPinDigitalInput pinDispenserLack = gpio.provisionDigitalInputPin(RaspiPin.GPIO_24, PinPullResistance.PULL_UP);
//    final GpioPinDigitalInput pinDispenserEmpty = gpio.provisionDigitalInputPin(RaspiPin.GPIO_25, PinPullResistance.PULL_UP);
//    final GpioPinDigitalInput btnPower = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_UP);
//    final GpioPinDigitalInput btnReset = gpio.provisionDigitalInputPin(RaspiPin.GPIO_11, PinPullResistance.PULL_UP);
//    final GpioPinDigitalInput btnDispense = gpio.provisionDigitalInputPin(RaspiPin.GPIO_29, PinPullResistance.PULL_UP);
    final GpioPinDigitalOutput relayBarrier = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_08, "BARRIER", PinState.HIGH);
    final GpioPinDigitalOutput relayLights = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_09, "LIGHTS", PinState.HIGH);
    final GpioPinDigitalOutput relayFan = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_30, "FAN", PinState.HIGH);

    final GpioPinDigitalOutput transistorAcceptor = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_27, "DISPENSE", PinState.LOW);
    final GpioPinDigitalOutput transistorReject = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_28, "REJECT", PinState.LOW);

    public void startProgram() {
        System.out.println(entranceID + " Tap Card Listener " + version);
//        System.out.println(entranceID + " Tap Card Listener " + version);

        try {
            welcomeAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/addas.wav"));
            welcomeClip = AudioSystem.getClip();
            welcomeClip.open(welcomeAudioIn);
        } catch (Exception ex) {
            notifyError(ex);
        }
        try {
            pleasewaitAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/plswait.wav"));
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
        try {
            errorAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/beep.wav"));
            errorClip = AudioSystem.getClip();
            errorClip.open(errorAudioIn);
        } catch (Exception ex) {
            notifyError(ex);
        }

        try {
            bgAudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/bgmusic.wav"));
            bgClip = AudioSystem.getClip();
            bgClip.open(bgAudioIn);
        } catch (Exception ex) {
            notifyError(ex);
        }

        try {
            insufficientaudioIn = AudioSystem.getAudioInputStream(MainStart.class.getResource("/sounds/Insufficient Payment.wav"));
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

        this.cards = new ArrayList<String>();

        DataBaseHandler dbh = new DataBaseHandler(CONSTANTS.serverIP);

        Scanner scan = new Scanner(System.in);

        String text = null;
        String cardUID = null;
        System.out.println("Reader Ready!");
//        transistorDispense.pulse(1000, true);
//        Gpio.delay(2000);
//        transistorReject.pulse(1000, true);
        //Testing Remotely
//        cards.add("ABC1234");
        while (true) {
            //System.out.print("!");
            strUID = "";
            text = scan.nextLine();
            if (null != text) {
                try {
                    System.out.println("RAW: " + text);
                    cardUID = Long.toHexString(Long.parseLong(text));
                    //cardUID = Integer.toHexString(Integer.parseInt(text));
                    cardUID = cardUID.toUpperCase();
                    strUID = cardUID.substring(6, 8) + cardUID.substring(4, 6) + cardUID.substring(2, 4) + cardUID.substring(0, 2);
                    System.out.println("UID: " + cardUID.substring(6, 8) + cardUID.substring(4, 6) + cardUID.substring(2, 4) + cardUID.substring(0, 2));
                } catch (Exception ex) {
                    System.err.println("Card Conversion: " + ex);
                }
                if (text.compareTo("X") == 0) {

                    transistorReject.setState(PinState.LOW);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        java.util.logging.Logger.getLogger(MainStart.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    transistorReject.setState(PinState.HIGH);

                }
                else {
                    transistorAcceptor.setState(PinState.LOW);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        java.util.logging.Logger.getLogger(MainStart.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    transistorAcceptor.setState(PinState.HIGH);
                }
                //System.out.println("" + stats);
//                strUID = Convert.bytesToHex(tagid);
                if (prevUID.compareToIgnoreCase(strUID) != 0) {
                    //Uncomment Below to disable Read same Card
//                    prevUID = strUID;
                    System.out.println("Card Read UID:" + strUID.substring(0, 8));
                    cardFromReader = strUID.substring(0, 8).toUpperCase();
//
                    if (cardFromReader.compareToIgnoreCase("") != 0) {
                        cards.add(cardFromReader);
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
                            try {
                                relayBarrier.setState(PinState.LOW);
                                Thread.sleep(1000);
                                relayBarrier.setState(PinState.HIGH);
                                dbh.deleteValidCard(cardFromReader);
                                Thread.sleep(2000);

                            } catch (InterruptedException ex) {
                                java.util.logging.Logger.getLogger(MainStart.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } else {
                            System.out.print("Sent InValid");
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
            }
//            strUID = null;
//
            Date now = new Date();
//            transistorDispense.pulse(500, true);
//        transistorReject.pulse(500, true);
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
//                    transistorDispense.pulse(500, true);
                } else {
//                    relayFan.high();
//                    relayBarrier.high();
                }
            } catch (Exception ex) {
            }

//            if (null != strUID) {
//                if (strUID.compareTo("") == 0) {
//                    transistorDispense.pulse(500, true);
//                }
//            } else {
//                transistorDispense.pulse(500, true);
//            }
            if (led1.isLow()) {
                led1.high();
            }
            if (led2.isLow()) {
                led2.high();
            }

            try {
//                Thread.sleep(500);
//                rc522 = null;
//                Thread.sleep(3200);
//                Thread.yield();
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
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

//        relayBarrier.setShutdownOptions(true, PinState.LOW);
//        relayFan.setShutdownOptions(true, PinState.LOW);
//        relayLights.setShutdownOptions(true, PinState.LOW);
//        btnDispense.setMode(PinMode.DIGITAL_INPUT);
//        btnDispense.setPullResistance(PinPullResistance.PULL_UP);
//        btnPower.setMode(PinMode.DIGITAL_INPUT);
//        btnPower.setPullResistance(PinPullResistance.PULL_UP);
//        btnReset.setMode(PinMode.DIGITAL_INPUT);
//        btnReset.setPullResistance(PinPullResistance.PULL_UP);
//
//        // set shutdown state for this input pin
//        btnDispense.setShutdownOptions(true);
//        btnPower.setShutdownOptions(true);
//        btnReset.setShutdownOptions(true);
        led1.high(); //Show POWER is ON led1.high
        led2.blink(100, 2000);

        transistorReject.pulse(500, true);

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
