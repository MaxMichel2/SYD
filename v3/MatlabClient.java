package rpc;
public class MatlabClient {
	public static void main(String[] args) throws Exception{
		java.net.Socket s = new java.net.Socket(127.0.0.1, 1234);
		java.io.ObjectInputStream ois = new java.io.ObjectInputStream(s.getInputStream());
		java.io.DataOutputStream dos = new java.io.DataOutputStream(s.getOutputStream());
		dos.writeUTF("constructor");
		dos.writeInt(10);
	}
}
