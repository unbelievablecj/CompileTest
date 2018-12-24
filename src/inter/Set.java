package inter;

import symbols.Type;
/**
 * 实现了左部为标识符且右部为一个表达式的赋值语句
 * @author unbel
 *
 */
public class Set extends Stmt {
	public Expr expr;
	public Id id;
	public Set(Id i,Expr x) {
		expr=x;
		id=i;
		if(check(id.type,expr.type)==null)error("type error");
	}
	public Type check(Type p1,Type p2) {
		if(Type.numeric(p1)&&Type.numeric(p2))return p2;
		else if(p1==Type.Bool&&p2==Type.Bool)return p2;
		else return null;
	}
	public void gen(int b ,int a) {
		emit(id.toString()+" = "+expr.gen().toString());
	}
}
