package parser;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

import inter.*;
import lexer.*;
import symbols.*;

public class Parser {
	private Stack<String> sta;
	private Lexer lex;
	/**
	 * 向前看一个词法单元
	 */
	private Token look;
	Env top=null;
	int used=0;
	int num=0;
	private FileWriter fw;
	
	
	
	public void out(String s) throws IOException {
		fw.write(++num+"	"+sta.toString()+"	"+s+"\n");
	}
	public void pop(int a) {
		for(int i=0;i<a;i++) {
			sta.pop();
		}
	}
	public void push(String s) {
		sta.push(s);
	}
	
	public Parser(Lexer l,FileWriter fw) throws IOException{
		lex=l;
		sta=new Stack<String>();
		this.fw=fw;
		move();
	}
	public Parser(Lexer l) throws IOException{
		lex=l;
		move();
	}
	/**
	 * 读入一个新的词法单元,并存入look
	 * @throws IOException
	 */
	void move() throws IOException{
if(look!=null)out("移入");
if(look!=null)sta.push(look.toString());
		look=lex.scan();
	}
	/**
	 * 匹配下一个词法单元，匹配成功则读入新的词法单元
	 * @param t
	 * @throws IOException
	 */
	void match(int t) throws IOException{

		if(look.tag==t) {
			move();
		}
		else error("syntax error");
	}
	void error(String s) {
		throw new Error("near line"+lex.line+": "+s) ;
	}
	public void program() throws IOException{
		Stmt s=block();
out("program->block");
pop(1);
push("program");
out("接受");
		int begin=s.newlabel();
		int after=s.newlabel();
		s.emitlabel(begin);
		s.gen(begin, after);
		s.emitlabel(after);
	}
	Stmt block() throws IOException{
		match('{');
out("移入");
sta.push("decls");
out("decls->空");
		Env savedEnv=top;
		top=new Env(top);
		decls();
out("stmts>空");
push("stmts");
		Stmt s=stmts();
		match('}');
out("block->{ decls stmts }");
pop(4);
push("block");
		top=savedEnv;
		return s;
	}
	void decls() throws IOException{
		while(look.tag==Tag.BASIC){
			Type p=type();
			Token tok=look;
			match(Tag.ID);
			match(';');
out("decl->type id ;");
pop(3);
push("decl");
out("decls->decls decl ");
pop(2);
push("decls");
			Id id=new Id((Word)tok, p, used);
			top.put(tok, id);
			used=used+p.width;
		}
	}
	Type type() throws IOException{
		Type p=(Type)look;
		match(Tag.BASIC);
out("type->basic");
pop(1);
push("type");
		if(look.tag!='[')return p;
		else return dims(p);
	}
	Type dims(Type p) throws IOException{
		match('[');
		Token tok=look;
		match(Tag.NUM);
		match(']');
out("type->type [ num ]");
pop(4);
push("type");
		if(look.tag=='[')
			p=dims(p);
		return new Array(((Num)tok).value,p);
	}
	Stmt stmts() throws IOException{
		if(look.tag=='}') {

			return Stmt.Null;
		}
		else return new Seq(stmt(),stmts());
	}
	Stmt stmt() throws IOException{
		Expr x;
		Stmt s,s1,s2;
		Stmt savedStmt;
		switch(look.tag) {
		case ';':
			move();
out("stmt->;");
pop(1);
push("stmt");
out("stmts->stmts stmt");
pop(1);
			return Stmt.Null;
		case Tag.IF:
			match(Tag.IF);
			match('(');
			x=bool();
			match(')');
			s1=stmt();
			if(look.tag!=Tag.ELSE) {
out("stmt->if ( bool ) stmt");
pop(5);
push("stmt");
out("stmts->stmts stmt");
pop(1);
				return new If(x,s1);
			}
			match(Tag.ELSE);
			s2=stmt();
out("stmt->if ( bool ) stmt else stmt");
pop(7);
push("stmt");
out("stmts->stmts stmt");
pop(1);
			return new Else(x,s1,s2);
		case Tag.WHILE:
			While whilenode =new While();
			savedStmt=Stmt.Enclosing;
			Stmt.Enclosing=whilenode;
			match(Tag.WHILE);
			match('(');
			x=bool();match(')');
			s1=stmt();
			whilenode.init(x, s1);
			Stmt.Enclosing=savedStmt;
out("stmt->while ( bool ) stmt");
pop(5);
push("stmt");
out("stmts->stmts stmt");
pop(1);
			return whilenode;
		case Tag.DO:
			Do donode=new Do();
			savedStmt =Stmt.Enclosing;Stmt.Enclosing=donode;
			match(Tag.DO);
			s1=stmt();
			match(Tag.WHILE);match('(');x=bool();match(')');match(';');
			donode.init(s1, x);
			Stmt.Enclosing=savedStmt;
out("stmt->do stmt while ( bool ) ;");
pop(7);
push("stmt");
out("stmts->stmts stmt");
pop(1);
			return donode;
		case Tag.BREAK:
			match(Tag.BREAK);match(';');
out("stmt->break ;");
pop(2);
push("stmt");
out("stmts->stmts stmt");
pop(1);
			return new Break();
		case '{':
			Stmt temp1=block();
out("stmt->block");
pop(1);
push("stmt");
			return temp1;
		default :
			Stmt temp2=assign();
out("stmt->loc = bool");
pop(3);
push("stmt");
			return temp2;
		}		
	}
	Stmt assign() throws IOException {
		Stmt stmt;Token t=look;
		match(Tag.ID);

		Id id=top.get(t);
		if(id==null)error(t.toString()+" undeclared");
		if(look.tag=='=') {
out("loc->id");
pop(1);
push("loc");
			move();
			stmt=new Set(id,bool());
		}
		else {
			Access x=offset(id);
out("loc->loc [ bool ]");
pop(4);
push("loc");
			match('=');
			stmt=new SetElem(x,bool());
		}
		match(';');

		return stmt;
	}
	Expr bool() throws IOException {
		Expr x=join();
out("bool->join");
pop(1);
push("bool");
		while(look.tag==Tag.OR) {
			Token tok=look;
			move();
			x=new Or(tok,x,join());
out("bool->bool || join");
pop(3);
push("bool");
		}

		return x;
	}
	Expr join() throws IOException {
		Expr x=equality();
out("join->equality");
pop(1);
push("join");
		while(look.tag==Tag.AND) {
			Token tok=look;
			move();
			x=new And(tok,x,equality());
out("join->join && equality");
pop(3);
push("join");
		}

		return x;
	}
	Expr equality() throws IOException {
		Expr x=rel();
out("equality->rel");
pop(1);
push("equality");
		while(look.tag==Tag.EQ||look.tag==Tag.NE) {
boolean is=look.tag==Tag.EQ;
			Token tok=look;
			move();
			x=new Rel(tok,x,rel());
if(is) {
	out("equality->equality == rel");
}
else {
	out("equality->equality != rel");
}
pop(3);
push("equality");
		}

		return x;
	}
	Expr rel() throws IOException {
		Expr x=expr();
		switch(look.tag) {
		case '<':{
			Token tok=look;
			move();
			Rel temp1=new Rel(tok,x,expr());
out("rel->expr < expr");
pop(3);
push("rel");
			
			return temp1;
		}
		case Tag.LE: {
			Token tok=look;
			move();
			Rel temp2=new Rel(tok,x,expr());
out("rel->expr <= expr");
pop(3);
push("rel");
			
			return temp2;
		}
		case Tag.GE: {
			Token tok=look;
			move();
			Rel temp3=new Rel(tok,x,expr());
out("rel->expr >= expr");
pop(3);
push("rel");
			
			return temp3;
		}
		case '>':{
			Token tok=look;
			move();
			Rel temp4=new Rel(tok,x,expr());
out("rel->expr > expr");
pop(3);
push("rel");
			
			return temp4;
		}
		default :
out("rel->expr");
pop(1);
push("rel");
			return x;
		}
	}
	Expr expr() throws IOException {
		Expr x=term();
out("expr->term");
pop(1);
push("expr");
		while(look.tag=='+'||look.tag=='-') {
boolean is=look.tag=='+';
			Token tok=look;
			move();
			x=new Arith(tok,x,term());
if(is) {
	out("expr->expr + term");
}
else {
	out("expr->expr - term");
}
pop(3);
push("expr");
		}
		return x;
	}
	Expr term() throws IOException {
		Expr x=unary();
out("term->unary");
pop(1);
push("term");
		while(look.tag=='*'||look.tag=='/') {
boolean is=look.tag=='*';
			Token tok=look;
			move();
			x=new Arith(tok,x,unary());
if(is) {
	out("term->term * unary");
}
else {
	out("term->term / unary");
}
pop(3);
push("term");
		}
		return x;
	}
	Expr unary() throws IOException {
		if(look.tag=='-') {
			move();
			Unary temp1=new Unary(Word.minus,unary());
out("unary-> - unary");
pop(2);
push("unary");
			return temp1;
		}
		else if(look.tag=='!') {
			Token tok=look;
			move();
			Not temp2=new Not(tok,unary());
out("unary-> ! unary");
pop(2);
push("unary");
			return temp2;
		}
		else return factor();
	}
	Expr factor() throws IOException {
		Expr x=null;
		switch(look.tag) {
			case '(':
				move();x=bool();match(')');
out("factor->( bool )");
pop(3);
push("factor");
				return x;
			case Tag.NUM:
				x=new Constant(look,Type.Int);move();
out("factor->num");
pop(1);
push("factor");
				return x;
			case Tag.REAL:
				x=new Constant(look,Type.Float);move();
out("factor->real");
pop(1);
push("factor");
				return x;
			case Tag.TRUE:
				x=Constant.True;move();
out("factor->true");
pop(1);
push("factor");
				return x;
			case Tag.FALSE:
				x=Constant.False;move();
out("factor->false");
pop(1);
push("factor");
				return x;
			default:
				error("symtax error");
				return x;
			case Tag.ID:
				String s=look.toString();
				Id id=top.get(look);
				if(id==null)error(look.toString()+" undeclared");
				move();
out("loc->id");
pop(1);
push("loc");
				if(look.tag!='[') {
out("factor->loc");
pop(1);
push("factor");
					return id;
				}
				else {
					Access temp=offset(id);
out("loc->loc [ bool ]");
pop(4);
push("loc");
out("factor->loc");
pop(1);
push("factor"); 
					return temp;
				}
		}
	}
	Access offset(Id a) throws IOException {
		Expr i;
		Expr w;
		Expr t1,t2;
		Expr loc;
		Type type=a.type;
		match('[');
		i=bool();
		match(']');
		type=((Array)type).of;
		w=new Constant(type.width);
		t1=new Arith(new Token('*'),i,w);
		loc=t1;
		while(look.tag=='[') {
			match('[');
			i=bool();match(']');
			type=((Array)type).of;
			w=new Constant(type.width);
			t1=new Arith(new Token('*'),i,w);
			t2=new Arith(new Token('+'),loc,t1);
			loc=t2;
		}
		return new Access(a,loc,type);
	}
}
