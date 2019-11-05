package rpc;
public class Client {
  public static void main(String [] arg) throws Exception {
    /*
    Matlab m = new Matlab(10);
    Result res = m.calcul(3);
    System.out.println("->" + res);
    */

    java.net.Socket s = new java.net.Socket("localhost", 1234);
    java.io.DataOutputStream dos = new java.io.DataOutputStream(s.getOutputStream());
    java.io.ObjectInputStream ois = new java.io.ObjectInputStream(s.getInputStream());
    dos.writeUTF("constructeur");
    dos.writeInt(10);
    dos.writeUTF("calcul");
    dos.writeInt(3);
    
    System.out.println("->" + ois.readObject());
  }
}
 

