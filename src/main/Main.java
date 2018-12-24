package main;
import java.io.*;
import lexer.*;
import parser.Parser;


public class Main {

	public static void main(String[] args) throws IOException {
		File out=new File("result.txt");
		FileWriter fw=new FileWriter(out);
		File in=new File("test.cpp");
		FileReader fr=new FileReader(in);
		fw.write("±àºÅ	·ûºÅÕ»		¶¯×÷\n");
		Lexer lex=new Lexer(fr);
		Parser parse=new Parser(lex,fw);
		parse.program();
		//System.out.println(lex.scan().toString());
		fw.flush();
		fw.close();
		
		
	}

}
