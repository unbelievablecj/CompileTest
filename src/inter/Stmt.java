package inter;
/**
 * 
 * @author unbel
 *  每个语句构造被实现为Stmt的一个子类
 */
public class Stmt extends Node {
	public Stmt(){}
	public static Stmt Null =new Stmt();
	public void gen(int b,int a){}
	int after=0;
	public static Stmt Enclosing=Stmt.Null;
}
