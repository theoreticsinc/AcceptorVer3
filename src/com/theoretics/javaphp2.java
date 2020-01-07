/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.theoretics;

import java.io.BufferedReader;
import java.net.*;
import java.io.*;
/**
 *
 * @author Theoretics
 */
public class javaphp2 {
    private static ServerSocket socket;

    private static Socket connection;
    private static String command       = new String();
    private static String responseStr   = new String();

    private static int port = 4309;

    public static void main(String args[])  {
        System.out.println("Signal Server is running.");

        try  {
            socket = new ServerSocket(port);

            while (true)  {
                connection = socket.accept();

                InputStreamReader inputStream = new InputStreamReader(connection.getInputStream());
                DataOutputStream response = new DataOutputStream(connection.getOutputStream());
                BufferedReader input = new BufferedReader(inputStream);

                command = input.readLine();
                //System.out.println("The input is" + command);
                response.writeBytes(responseStr);
                response.flush();
                //response.close();

                System.out.println("Running");
            }
        } catch (IOException e)  {
            System.out.println("Fail!: " + e.toString());
        }

        System.out.println("Closing...");
    }
}







