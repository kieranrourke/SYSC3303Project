import java.io.IOException;
import java.net.*;

public class MainSystem {

    private DataPacket schedulerAndFloorData = null;
    private DataPacket schedulerAndElevatorData = null;

    public static int Scheduler_Floor_Port_Number = 100;
    public static int Scheduler_Elevator_Port_Number = 68;
	 public static int Elevator_Port_Number = 96;
	 public static int Floor_Port_Number = 420;
    public static InetAddress address;

	 static {
		  try {
				address = InetAddress.getLocalHost();
		  } catch (UnknownHostException e) {
				throw new RuntimeException(e);
		  }
	 }

	 public static int buffer_size = 100;

	 /**
     * Method to allow floor and scheduler to update their data packet
     * @param packet - DataPacket object
     */
    public synchronized void updateSchedulerAndFloorData(DataPacket packet){
        while(schedulerAndFloorData != null) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.print(e);
            }
        }

        schedulerAndFloorData = packet;
        notifyAll();
    }

    /**
     * Method to allow floor and scheduler to get their data packet
     * @return DataPacket object
     */
    public synchronized DataPacket getSchedulerAndFloorData(){
        while(schedulerAndFloorData == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.print(e);
            }
        }
        DataPacket finalPacket = schedulerAndFloorData;
        schedulerAndFloorData = null;
        notifyAll();
        return finalPacket;
    }


    /**
     * Method to allow scheduler and elevator to update their data packet
     * @param packet - DataPacket object
     */
    public synchronized void updateSchedulerAndElevatorData(DataPacket packet) {
        while (schedulerAndElevatorData != null) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.print(e);
            }
        }
        schedulerAndElevatorData = packet;
        notifyAll();
    }

    /**
     * Method to allow scheduler and elevator to get their data packet
     * @return DataPacket object
     */
    public synchronized DataPacket getSchedulerAndElevatorData() {
        while (schedulerAndElevatorData == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.print(e);
            }
        }
        DataPacket finalPacket = schedulerAndElevatorData;
        schedulerAndElevatorData = null;
        notifyAll();
        return finalPacket;
    }

	 public static void rpc_send(DatagramPacket request, DatagramPacket response){
		  // Send the datagram packet to the client via the send socket.
		  try {
				System.out.println("Sending get request to Scheduler on port " + request.getPort() );
				DatagramSocket tempSendSocket = new DatagramSocket();
				tempSendSocket.send(request);


				System.out.println("Receiving from host port " + response.getPort() + "\n");
				DatagramSocket tempReceiveSocket = new DatagramSocket(response.getPort());
				tempReceiveSocket.receive(response);

				tempSendSocket.close();
				tempReceiveSocket.close();

				//Get Response
		  } catch (SocketException e) {
				throw new RuntimeException(e);
		  } catch (IOException e) {
				throw new RuntimeException(e);
		  }
	 }

	 /**
	  * Sends an acknowledgment packet to the client in response to a received packet.
	  *
	  * @param receivedPacket The DatagramPacket received from the client that triggered the acknowledgment.
	  */
	  public static void sendAcknowledgment(DatagramPacket receivedPacket) {
		  // Extract the content of the received packet
		  String received = new String(receivedPacket.getData(), 0, receivedPacket.getLength());

		  // Construct acknowledgment data including the content of the received packet
		  byte acknowledgmentData[] = ("ACK " + received).getBytes();

		  // Create a DatagramPacket for the acknowledgment
		  DatagramPacket sendPacket = new DatagramPacket(acknowledgmentData, acknowledgmentData.length,
					 receivedPacket.getAddress(), receivedPacket.getPort());

		  // Send the acknowledgment packet
			DatagramSocket tempSendSocket = null;
			try {
				 tempSendSocket = new DatagramSocket();
			} catch (SocketException e) {
				 throw new RuntimeException(e);
			}
			try {
				tempSendSocket.send(sendPacket);
		  } catch (IOException e) {
				e.printStackTrace();
				System.exit(1); // Consider handling this exception more gracefully in a production environment
		  }
			tempSendSocket.close();

	 }

	 public static void waitForAck (DatagramSocket socket) {
		  DatagramPacket receivePacket = new DatagramPacket(new byte[MainSystem.buffer_size], MainSystem.buffer_size);
		  try {
				socket.receive(receivePacket);
				MainSystem.printReceivePacketData(receivePacket);
		  } catch (IOException e) {
				throw new RuntimeException(e);
		  }

	 }

	 private synchronized static void printPacketData(DatagramPacket packet){
		  //Output data
		  System.out.print("Containing... as a string: ");
		  System.out.println(new String(packet.getData(),0, packet.getLength()));
		  System.out.print("Containing... as bytes: ");
		  for(int i=0; i< packet.getData().length ; i++) {
				System.out.print(packet.getData()[i] +" ");
		  }
		  System.out.println("\n");
	 }

	 public synchronized static void printReceivePacketData(DatagramPacket packet){
		  // Process the received datagram.
		  System.out.println("Packet received:");
		  System.out.println("From host: " + packet.getAddress());
		  System.out.println("Host port: " + packet.getPort());
		  int len = packet.getLength();
		  System.out.println("Length: " + len);
		  printPacketData(packet);
	 }

	 public synchronized static void printSendPacketData(DatagramPacket packet){
		  System.out.println("Sending packet:");
		  System.out.println("To host: " + packet.getAddress());
		  System.out.println("Destination host port: " + packet.getPort());
		  int len = packet.getLength();
		  System.out.println("Length: " + len);
		  printPacketData(packet);
	 }


    public static void main(String[] args) throws InterruptedException {
        MainSystem mainSystem = new MainSystem();
        Thread floor = new Thread(new Floor(mainSystem), "Floor");
        Thread elevator = new Thread(new Elevator(mainSystem), "Elevator");
        Thread scheduler = new Thread(new Scheduler(mainSystem), "Scheduler");


        floor.start();
        scheduler.start();
		  Thread.sleep(1000);
        elevator.start();
    }
}