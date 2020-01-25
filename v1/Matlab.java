package rpc;

// écrire une interface => public class Matlab implements MatlabIfc
// dans MatlabIfc.java, il y a JUSTE les fonctions de Matlab avec leur input et output
// package rpc;
// public inteface MatlabIfc {
//     public Result calcul(int res);
// }
public class Matlab{
	private int i;

	public Matlab(int i) {
		this.i = i;
	}

	public Result calcul(int in) {
		return new Result(in * this.i);
	}
}
